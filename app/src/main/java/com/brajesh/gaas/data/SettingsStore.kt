package com.brajesh.gaas.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class MacroGoal(
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

/**
 * Holds the Gemini API key and the user's daily macro goal.
 * Backed by EncryptedSharedPreferences rather than plain SharedPreferences —
 * the key is still extractable on a rooted/decompiled device, but this at
 * least keeps it off disk in plaintext and out of a routine backup dump.
 * Falls back to plain SharedPreferences if the device Keystore misbehaves
 * (a known crash on some handsets) so first launch never dies.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "gaas_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        context.getSharedPreferences("gaas_settings", Context.MODE_PRIVATE)
    }

    var geminiApiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var goal: MacroGoal?
        get() {
            val cal = prefs.getFloat(KEY_GOAL_CAL, -1f)
            if (cal < 0) return null
            return MacroGoal(
                calories = cal.toDouble(),
                proteinG = prefs.getFloat(KEY_GOAL_PROTEIN, 0f).toDouble(),
                carbsG = prefs.getFloat(KEY_GOAL_CARBS, 0f).toDouble(),
                fatG = prefs.getFloat(KEY_GOAL_FAT, 0f).toDouble()
            )
        }
        set(value) {
            if (value == null) {
                prefs.edit().remove(KEY_GOAL_CAL).apply()
                return
            }
            prefs.edit()
                .putFloat(KEY_GOAL_CAL, value.calories.toFloat())
                .putFloat(KEY_GOAL_PROTEIN, value.proteinG.toFloat())
                .putFloat(KEY_GOAL_CARBS, value.carbsG.toFloat())
                .putFloat(KEY_GOAL_FAT, value.fatG.toFloat())
                .apply()
        }

    val isOnboarded: Boolean
        get() = geminiApiKey != null && goal != null

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_GOAL_CAL = "goal_cal"
        private const val KEY_GOAL_PROTEIN = "goal_protein"
        private const val KEY_GOAL_CARBS = "goal_carbs"
        private const val KEY_GOAL_FAT = "goal_fat"
    }
}
