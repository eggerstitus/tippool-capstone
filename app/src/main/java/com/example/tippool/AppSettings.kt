package com.tippool

import android.content.Context
import android.content.SharedPreferences

data class AppSettings(
    val closingReminderEnabled: Boolean = true,
    val closingReminderDelayMinutes: Int = 15,
    val closingReminderItems: List<String> = listOf(
        "Check blast chiller",
        "Make sure things are charging",
    ),
    val morningReminderEnabled: Boolean = true,
    val morningReminderDelayMinutes: Int = 0,
    val morningReminderItems: List<String> = listOf(
        "Check catering orders",
    ),
    val saturdayExtraItems: List<String> = listOf(
        "Check Sunday catering",
        "Check Monday catering",
    ),
    val locationEmail: String? = null,          // null = not yet chosen
    val useCustomEmail: Boolean = false,
    val customEmail: String = "",
)

private const val PREFS_NAME     = "tippool_settings"
private const val SEP            = "|||"

private const val KEY_CLOSING_ENABLED = "closing_enabled"
private const val KEY_CLOSING_DELAY   = "closing_delay_min"
private const val KEY_CLOSING_ITEMS   = "closing_items"
private const val KEY_MORNING_ENABLED = "morning_enabled"
private const val KEY_MORNING_DELAY   = "morning_delay_min"
private const val KEY_MORNING_ITEMS   = "morning_items"
private const val KEY_SATURDAY_ITEMS  = "saturday_items"

private const val KEY_LOCATION_EMAIL  = "location_email"
private const val KEY_USE_CUSTOM_EMAIL = "use_custom_email"
private const val KEY_CUSTOM_EMAIL    = "custom_email"

private fun prefs(context: Context): SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

fun AppSettings.resolvedCateringEmail(): String = when {
    useCustomEmail && customEmail.isNotBlank() -> customEmail
    else -> locationEmail ?: CATERING_REPORT_EMAIL
}
fun save(context: Context, settings: AppSettings) {
    prefs(context).edit()
        .putBoolean(KEY_CLOSING_ENABLED, settings.closingReminderEnabled)
        .putInt(KEY_CLOSING_DELAY,       settings.closingReminderDelayMinutes)
        .putString(KEY_CLOSING_ITEMS,    settings.closingReminderItems.joinToString(SEP))
        .putBoolean(KEY_MORNING_ENABLED, settings.morningReminderEnabled)
        .putInt(KEY_MORNING_DELAY,       settings.morningReminderDelayMinutes)
        .putString(KEY_MORNING_ITEMS,    settings.morningReminderItems.joinToString(SEP))
        .putString(KEY_SATURDAY_ITEMS,   settings.saturdayExtraItems.joinToString(SEP))
        .putString(KEY_LOCATION_EMAIL,   settings.locationEmail)      // ← new
        .putBoolean(KEY_USE_CUSTOM_EMAIL, settings.useCustomEmail)    // ← new
        .putString(KEY_CUSTOM_EMAIL,     settings.customEmail)        // ← new
        .apply()
}

fun loadSettings(context: Context): AppSettings {
    return try {
        val p = prefs(context)
        val defaults = AppSettings()

        fun strList(key: String, default: List<String>): List<String> {
            val raw = p.getString(key, null) ?: return default
            return if (raw.isBlank()) emptyList() else raw.split(SEP)
        }

        AppSettings(
            closingReminderEnabled      = p.getBoolean(KEY_CLOSING_ENABLED, defaults.closingReminderEnabled),
            closingReminderDelayMinutes = p.getInt(KEY_CLOSING_DELAY,       defaults.closingReminderDelayMinutes),
            closingReminderItems        = strList(KEY_CLOSING_ITEMS,        defaults.closingReminderItems),
            morningReminderEnabled      = p.getBoolean(KEY_MORNING_ENABLED, defaults.morningReminderEnabled),
            morningReminderDelayMinutes = p.getInt(KEY_MORNING_DELAY,       defaults.morningReminderDelayMinutes),
            morningReminderItems        = strList(KEY_MORNING_ITEMS,        defaults.morningReminderItems),
            saturdayExtraItems          = strList(KEY_SATURDAY_ITEMS,       defaults.saturdayExtraItems),
            locationEmail               = p.getString(KEY_LOCATION_EMAIL, null),         // ← new
            useCustomEmail              = p.getBoolean(KEY_USE_CUSTOM_EMAIL, false),     // ← new
            customEmail                 = p.getString(KEY_CUSTOM_EMAIL, "") ?: "",       // ← new
        )
    } catch (e: Exception) {
        AppSettings() // fall back to defaults if anything goes wrong
    }
}
