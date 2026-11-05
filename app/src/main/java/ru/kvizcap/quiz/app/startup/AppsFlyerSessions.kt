package ru.kvizcap.quiz.app.startup

/** Called on the application main scope, once for every SDK session-ready signal. */
class AppsFlyerSessions(
    private val isSdkReady: () -> Boolean,
    private val startSession: () -> Unit,
    private val log: (String) -> Unit = {},
) {
    private var online = false
    private var pending = false

    fun onSdkReady() {
        pending = true
        log("AppsFlyer SessionReady: SDK готов к новой сессии; сеть=$online")
        startIfReady()
    }

    fun onNetworkChanged(available: Boolean) {
        online = available
        log("AppsFlyer: доступность проверенного интернет-соединения=$available")
        startIfReady()
    }

    private fun startIfReady() {
        if (!pending || !online || !isSdkReady()) return
        pending = false
        try {
            log("AppsFlyer: start() из готовности SDK и сети")
            startSession()
        } catch (error: Exception) {
            pending = true
            log("AppsFlyer start ОШИБКА: ${error.javaClass.simpleName}")
        }
    }
}
