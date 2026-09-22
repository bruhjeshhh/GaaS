package com.brajesh.gaas.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class ParsedItem(
    val name: String,
    val quantity: String, // kept as free text ("2 medium", "~1 tbsp") since input quantities are rarely exact
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

@Serializable
data class ParsedMeal(
    val items: List<ParsedItem>,
    val totalCalories: Double,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val assumptionsNote: String // e.g. "assumed medium roti (~40g), 1 tsp ghee, home-style paneer curry"
)

sealed class GeminiResult {
    data class Success(val meal: ParsedMeal) : GeminiResult()
    data class ApiError(val message: String) : GeminiResult()
    data class ParseError(val message: String) : GeminiResult()
    data class NetworkError(val cause: Throwable) : GeminiResult()
}

/**
 * Talks directly to the Gemini API using the user's own key — no backend in between.
 * The prompt is the whole trick here: it's written for someone describing a meal the
 * way they'd tell a friend about it, not someone entering precise gram weights.
 *
 * Parsing is deliberately hand-rolled rather than `decodeFromString<ParsedMeal>`:
 * a strict reflective decode throws on any single deviation from the schema, and models
 * slip all the time (markdown fences, trailing prose, numbers where strings belong,
 * `null` fields, truncated output). We instead walk the JSON with type coercion so a
 * loose response still becomes a usable estimate instead of an error wall.
 */
class GeminiClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun estimateMacros(mealDescription: String): GeminiResult {
        val prompt = buildPrompt(mealDescription)

        val requestJson = buildString {
            append("""{"contents":[{"parts":[{"text":""")
            append(json.encodeToString(prompt))
            append("""}]}],"generationConfig":{"response_mime_type":"application/json","temperature":0.2,"maxOutputTokens":2048}}""")
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return GeminiResult.ApiError("Gemini API error (${response.code}): ${bodyStr.take(300)}")
                }
                val text = extractText(bodyStr)
                if (text == null) {
                    return GeminiResult.ParseError(parseFailureReason(bodyStr))
                }
                val meal = parseMeal(text)
                if (meal == null) {
                    val seen = text.realJsonSnippet()
                    return GeminiResult.ParseError(
                        if (seen.isEmpty()) "Gemini returned an empty estimate. Try again."
                        else "Couldn't read the estimate Gemini returned (saw: $seen). Try rephrasing."
                    )
                }
                GeminiResult.Success(meal)
            }
        } catch (e: IOException) {
            GeminiResult.NetworkError(e)
        } catch (e: Exception) {
            GeminiResult.ParseError(e.message ?: "Unknown parse failure")
        }
    }

    /**
     * Gemini's response wraps the generated text inside candidates[0].content.parts[0].text.
     * We pull just that string back out before decoding it as our ParsedMeal shape.
     */
    private fun extractText(rawBody: String): String? {
        val envelope = json.parseToJsonElement(rawBody)
        return runCatching {
            envelope.jsonObject["candidates"]
                ?.jsonArray?.get(0)
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray?.get(0)
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.content
        }.getOrNull()
    }

    /** Turns a parse-time failure into a human-readable reason by inspecting the API envelope. */
    private fun parseFailureReason(rawBody: String): String {
        val root = runCatching { json.parseToJsonElement(rawBody) }.getOrNull() as? JsonObject
        val finishReason = root?.get("candidates")
            ?.jsonArrayOrNull()?.firstOrNull()
            ?.jsonObjectOrNull()?.get("finishReason")
            ?.stringValue()
        val blockReason = root?.get("promptFeedback")
            ?.jsonObjectOrNull()?.get("blockReason")
            ?.stringValue()
        return when {
            blockReason != null -> "Gemini blocked the request ($blockReason). Try rephrasing."
            finishReason == "MAX_TOKENS" -> "Gemini's estimate was cut off mid-answer. Try a shorter description."
            finishReason == "SAFETY" -> "Gemini flagged the request as unsafe. Try rephrasing."
            rawBody.isBlank() -> "Gemini returned an empty response. Check your network and try again."
            else -> "Gemini returned no usable content."
        }
    }

    /**
     * Digs the JSON object out of whatever text the model produced — it may be wrapped in
     * markdown fences or have a sentence trailing it. First `{` to last `}` handles all of it.
     */
    private fun parseMeal(text: String): ParsedMeal? {
        val root = extractJsonObject(text) as? JsonObject ?: return null

        val items = root["items"].jsonArrayOrNull().orEmpty().mapNotNull { element ->
            val o = element.jsonObjectOrNull() ?: return@mapNotNull null
            ParsedItem(
                name = o["name"].stringValue(),
                quantity = o["quantity"].stringValue(),
                calories = o["calories"].doubleValue(),
                proteinG = o["proteinG"].doubleValue(),
                carbsG = o["carbsG"].doubleValue(),
                fatG = o["fatG"].doubleValue()
            )
        }

        // Trust the model's totals when present; if it omitted/zeroed them, backfill from the items.
        fun total(field: String, sum: Double): Double {
            val declared = root[field].doubleValue()
            return if (declared > 0) declared else sum
        }

        val sumCalories = items.sumOf { it.calories }
        val sumProteinG = items.sumOf { it.proteinG }
        val sumCarbsG = items.sumOf { it.carbsG }
        val sumFatG = items.sumOf { it.fatG }

        return ParsedMeal(
            items = items,
            totalCalories = total("totalCalories", sumCalories),
            totalProteinG = total("totalProteinG", sumProteinG),
            totalCarbsG = total("totalCarbsG", sumCarbsG),
            totalFatG = total("totalFatG", sumFatG),
            assumptionsNote = root["assumptionsNote"].stringValue()
        )
    }

    /** The first `{` to the last `}` in the text, parsed — or null if there is no JSON in it. */
    private fun extractJsonObject(text: String): JsonElement? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { json.parseToJsonElement(text.substring(start, end + 1)) }.getOrNull()
    }

    private fun String.realJsonSnippet(): String {
        val start = indexOf('{')
        val end = lastIndexOf('}')
        val sample = if (start >= 0 && end > start) substring(start, end + 1) else trim()
        return sample.replace('\n', ' ').take(120)
    }

    // ---- coercion helpers: never throw, degrade to sensible defaults ----

    private fun JsonElement?.stringValue(fallback: String = ""): String =
        if (this is JsonPrimitive && this != JsonNull) content else fallback

    private fun JsonElement?.doubleValue(fallback: Double = 0.0): Double =
        if (this is JsonPrimitive && this != JsonNull) (content.toDoubleOrNull() ?: fallback) else fallback

    private fun JsonElement?.jsonObjectOrNull(): JsonObject? = this as? JsonObject

    private fun JsonElement?.jsonArrayOrNull(): JsonArray? = this as? JsonArray

    private fun buildPrompt(mealDescription: String): String = """
        You are a nutrition estimation assistant inside a food-logging app. The user
        describes meals casually and verbosely, the way they'd tell a friend what they
        ate — not with exact grams or brand names. Examples of real input you'll see:

        - "3 rotis with a little ghee, homemade paneer and stuff like that"
        - "had a bowl of curd rice, some pickle, skipped breakfast"
        - "chicken sandwich from the cafe, medium fries, diet coke"

        Your job: read the description, infer reasonable standard portion sizes for any
        vague quantities ("a little", "some", "a bowl"), and estimate macros for the
        whole meal. Use typical home-cooking or restaurant-standard assumptions where
        the user didn't specify (e.g. "roti" = ~40g whole wheat flatbread, "a little
        ghee" = ~1 tsp, "paneer" = ~50-80g if not quantified as a main component).
        Prefer Indian home-cooking conventions when dishes are Indian, since that's the
        most common cuisine this app sees. When genuinely ambiguous, pick the most
        common/likely reading rather than asking for clarification — this is a single-shot
        estimate, not a conversation.

        Respond with ONLY valid JSON in this exact shape. No markdown fences, no
        commentary before or after, no nulls. Always emit the totals even
        if you have to sum the items yourself. Numbers must be plain JSON numbers
        (never quoted strings), and "quantity" must be a plain string:

        {
          "items": [
            {"name": string, "quantity": string, "calories": number, "proteinG": number, "carbsG": number, "fatG": number}
          ],
          "totalCalories": number,
          "totalProteinG": number,
          "totalCarbsG": number,
          "totalFatG": number,
          "assumptionsNote": string
        }

        "assumptionsNote" should briefly state the portion assumptions you made in
        plain language (one sentence), so the user can see what you guessed and correct
        it next time if it's off. Numbers should be totals for the actual quantity
        described (e.g. all 3 rotis combined, not per-roti) unless "quantity" per item
        makes that ambiguous, in which case per-item values should already reflect that
        item's full described quantity.

        Meal description to estimate:
        "$mealDescription"
    """.trimIndent()
}