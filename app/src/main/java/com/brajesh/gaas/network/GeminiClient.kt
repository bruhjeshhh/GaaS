package com.brajesh.gaas.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    data class ParseError(val rawResponse: String) : GeminiResult()
    data class NetworkError(val cause: Throwable) : GeminiResult()
}

/**
 * Talks directly to the Gemini API using the user's own key — no backend in between.
 * The prompt is the whole trick here: it's written for someone describing a meal the
 * way they'd tell a friend about it, not someone entering precise gram weights.
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
            append("""}]}],"generationConfig":{"response_mime_type":"application/json","temperature":0.2}}""")
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
                val text = extractText(bodyStr) ?: return GeminiResult.ParseError(bodyStr)
                val meal = json.decodeFromString<ParsedMeal>(text)
                GeminiResult.Success(meal)
            }
        } catch (e: IOException) {
            GeminiResult.NetworkError(e)
        } catch (e: Exception) {
            GeminiResult.ParseError(e.message ?: "Unknown parse failure")
        }
    }

    /**
     * Gemini's response wraps the actual JSON text inside candidates[0].content.parts[0].text.
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

        Respond with ONLY valid JSON matching exactly this shape, no markdown fences,
        no commentary outside the JSON:

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
