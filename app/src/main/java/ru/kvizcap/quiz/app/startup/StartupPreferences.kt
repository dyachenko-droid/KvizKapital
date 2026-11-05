package ru.kvizcap.quiz.app.startup

import android.content.Context

class StartupPreferences(context: Context) : LaunchPreferences {
    private val prefs = context.getSharedPreferences("startup", Context.MODE_PRIVATE)

    override val isFirstLaunch: Boolean get() = prefs.getBoolean("first_launch", true)
    override val privacyUrl: String get() = prefs.getString("privacy_url", DEFAULT_PRIVACY_URL) ?: DEFAULT_PRIVACY_URL

    override val hasRemotePrivacyUrl: Boolean get() = prefs.getBoolean("remote_privacy", false)

    override fun savePrivacyUrl(url: String) {
        prefs.edit().putString("privacy_url", url).putBoolean("remote_privacy", false).apply()
    }

    override fun completeFirstLaunch(url: String, fromFirebase: Boolean) {
        prefs.edit().putString("privacy_url", url).putBoolean("first_launch", false)
            .putBoolean("remote_privacy", fromFirebase).apply()
    }

    var notificationPermissionRequested: Boolean
        get() = prefs.getBoolean("notification_permission_requested", false)
        set(value) { prefs.edit().putBoolean("notification_permission_requested", value).apply() }
}
