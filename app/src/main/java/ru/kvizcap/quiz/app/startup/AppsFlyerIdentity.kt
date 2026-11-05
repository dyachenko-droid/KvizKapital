package ru.kvizcap.quiz.app.startup

class AppsFlyerIdentity(
    private val idProvider: () -> String,
    private val login: (String) -> Unit,
) {
    private var linkedId: String? = null

    /** Returns the same UID used in links and retries a failed OneSignal login on the next signal. */
    fun sync(): String {
        val id = runCatching(idProvider).getOrDefault("")
        if (id.isNotBlank() && id != linkedId) {
            runCatching { login(id) }.onSuccess { linkedId = id }
        }
        return id
    }
}
