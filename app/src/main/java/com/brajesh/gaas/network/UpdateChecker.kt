package com.brajesh.gaas.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** What the last update check found. Drives both the Home banner and Settings. */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()

    /** Newest tagged release is the one already running. */
    data class UpToDate(val version: String) : UpdateState()

    /**
     * [dismissed] is per-version: the user can hide the Home banner, but a newer
     * release brings it back.
     */
    data class Available(
        val version: String,
        val url: String,
        val dismissed: Boolean = false
    ) : UpdateState()

    data class Failed(val reason: String) : UpdateState()
}

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tag: String? = null,
    @SerialName("html_url") val url: String? = null,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("prerelease") val prerelease: Boolean = false
)

/**
 * Checks GitHub for a newer tagged release so the app can offer the update
 * itself, rather than hoping everyone watches the repo.
 *
 * Deliberately queries `/releases` instead of `/releases/latest`: CI publishes a
 * rolling **debug-build** release on every push and it is not a prerelease, so
 * `/latest` would always resolve to that build and never to a real version. We
 * take the newest `v*` release instead.
 */
class UpdateChecker(private val currentVersion: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /** Blocking; callers are expected to be on an IO dispatcher, like [GeminiClient]. */
    suspend fun check(): UpdateState {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$OWNER/$REPO/releases?per_page=30")
            .header("Accept", "application/vnd.github+json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    // 403/429 here is almost always the unauthenticated 60/hour cap.
                    UpdateState.Failed(
                        if (response.code == 403 || response.code == 429) {
                            "GitHub's rate limit was hit. Try again later."
                        } else {
                            "GitHub returned an error (${response.code})."
                        }
                    )
                } else {
                    findNewer(body)
                }
            }
        } catch (e: IOException) {
            UpdateState.Failed("No connection — couldn't check for updates.")
        } catch (e: Exception) {
            UpdateState.Failed("Couldn't read GitHub's response (${e::class.simpleName ?: "?"}).")
        }
    }

    private fun findNewer(body: String): UpdateState {
        val releases = runCatching { json.decodeFromString<List<GitHubRelease>>(body) }.getOrNull()
            ?: return UpdateState.Failed("Couldn't read GitHub's response.")

        val newest = releases
            .filter { !it.draft && !it.prerelease }
            .mapNotNull { release -> release.tag?.takeIf { it.startsWith("v") }?.let { it to release.url.orEmpty() } }
            .firstOrNull()
            ?: return UpdateState.UpToDate(currentVersion)

        val (tag, url) = newest
        val version = tag.removePrefix("v")
        return if (url.isNotEmpty() && isNewerThanCurrent(version)) {
            UpdateState.Available(version, url)
        } else {
            UpdateState.UpToDate(currentVersion)
        }
    }

    /**
     * Component-wise numeric compare, so 1.10.0 beats 1.9.0 and "1.4" equals
     * "1.4.0" (no nagging). Anything unparseable counts as 0 rather than
     * throwing — a bad tag should not break the check.
     */
    private fun isNewerThanCurrent(candidate: String): Boolean {
        val parts = { v: String -> v.removePrefix("v").substringBefore('-').split('.') }
        val newer = parts(candidate).map { it.toIntOrNull() ?: 0 }
        val current = parts(currentVersion).map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(newer.size, current.size)) {
            val a = newer.getOrElse(i) { 0 }
            val b = current.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private companion object {
        const val OWNER = "bruhjeshhh"
        const val REPO = "GaaS"
    }
}
