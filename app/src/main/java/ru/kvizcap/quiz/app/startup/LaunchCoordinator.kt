package ru.kvizcap.quiz.app.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

const val DEFAULT_PRIVACY_URL = "https://dyachenko-droid.github.io/KvizKapital/privacy.html"

enum class InstallAttribution(val databaseField: String) {
    ORGANIC("org"), NON_ORGANIC("in_org");

    companion object {
        fun fromStatus(value: Any?): InstallAttribution? = when (value?.toString()?.trim()?.lowercase()) {
            "organic" -> ORGANIC
            "non-organic" -> NON_ORGANIC
            else -> null
        }
    }
}

sealed interface LaunchRoute {
    data object Loading : LaunchRoute
    data object Native : LaunchRoute
    data class Web(val url: String, val requiresAcceptance: Boolean) : LaunchRoute
}

interface LaunchPreferences {
    val isFirstLaunch: Boolean
    val privacyUrl: String
    val hasRemotePrivacyUrl: Boolean
    fun savePrivacyUrl(url: String)
    fun completeFirstLaunch(url: String, fromFirebase: Boolean)
}

fun interface AttributionSource {
    suspend fun awaitAttribution(): InstallAttribution?
}

fun interface PrivacyLinkSource {
    suspend fun fetch(field: String): String?
}

/** Late SDK/database callbacks cannot change a decision after the deadline. */
class LaunchCoordinator(
    private val preferences: LaunchPreferences,
    private val attribution: AttributionSource,
    private val links: PrivacyLinkSource,
    private val attributionTimeoutMillis: Long = 15_000,
    private val databaseTimeoutMillis: Long = 8_000,
    private val log: (String) -> Unit = {},
    private val nonOrganicLink: (String) -> String = { it },
) {
    suspend fun resolve(): LaunchRoute {
        log("Запуск: first_launch=${preferences.isFirstLaunch}, remote_privacy=${preferences.hasRemotePrivacyUrl}, сохранённый URL=${preferences.privacyUrl}")
        if (!preferences.isFirstLaunch) {
            val saved = receivedLink(preferences.privacyUrl)
            return if (saved != null && preferences.hasRemotePrivacyUrl) {
                log("Маршрут: WebView из сохранённых настроек; AppsFlyer/Firebase не запрашиваем. URL=$saved; Принять=false")
                LaunchRoute.Web(saved, requiresAcceptance = false)
            } else {
                log("Маршрут: нативное приложение; первый запуск уже завершён, сохранённой удалённой строки нет")
                LaunchRoute.Native
            }
        }
        val install = safelyWithin("AppsFlyer", attributionTimeoutMillis, onTimeout = {
            log("AppsFlyer: нет ответа за ${attributionTimeoutMillis}мс; считаем установку ORGANIC и читаем Firebase /org")
            InstallAttribution.ORGANIC
        }) { attribution.awaitAttribution() }
        log("AppsFlyer: результат для выбора маршрута=$install")
        var fallbackReason = "AppsFlyer не вернул известный статус до принятия решения (см. записи выше)"
        val remoteUrl = install?.let {
            log("Firebase: выбран корневой путь /${it.databaseField} по статусу $install")
            val raw = safelyWithin("Firebase /${it.databaseField}", databaseTimeoutMillis) { links.fetch(it.databaseField) }
            val valid = receivedLink(raw)
            fallbackReason = if (raw == null) "Firebase: нет строкового значения (поле отсутствует, другой тип, ошибка или таймаут)"
                else "Firebase: получена пустая строка"
            log("Firebase: строка=$raw; непустая строка получена=${valid != null}; формат адреса не проверяется")
            if (valid != null) log("Firebase: строка принята без изменений: $valid")
            valid
        }

        if (remoteUrl != null) {
            val finalUrl = if (install == InstallAttribution.NON_ORGANIC) nonOrganicLink(remoteUrl) else remoteUrl
            preferences.completeFirstLaunch(finalUrl, fromFirebase = true)
            log("Маршрут: WebView из Firebase; Принять=false; first_launch=false сохранён")
            return LaunchRoute.Web(finalUrl, requiresAcceptance = false)
        }
        preferences.savePrivacyUrl(DEFAULT_PRIVACY_URL)
        log("ЗАПАСНАЯ PRIVACY: $fallbackReason")
        log("Маршрут: WebView стандартной privacy; URL=$DEFAULT_PRIVACY_URL; Принять=true; first_launch остаётся true")
        return LaunchRoute.Web(DEFAULT_PRIVACY_URL, requiresAcceptance = true)
    }

    fun acceptPrivacy() {
        preferences.completeFirstLaunch(DEFAULT_PRIVACY_URL, fromFirebase = false)
        log("Нажата Принять: first_launch=false, remote_privacy=false; маршрут: нативное приложение")
    }

    private suspend fun <T> safelyWithin(
        stage: String,
        timeout: Long,
        onTimeout: () -> T? = { null },
        action: suspend () -> T,
    ): T? = try {
        log("$stage: ожидание, лимит ${timeout}мс")
        // Wrap nullable results so a completed null differs from a timeout.
        val outcome = withTimeoutOrNull(timeout) { Result.success(action()) }
        if (outcome == null) {
            log("$stage: ТАЙМАУТ ${timeout}мс; поздний ответ не изменит маршрут")
            onTimeout()
        } else {
            log("$stage: ожидание завершено")
            outcome.getOrThrow()
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        log("$stage: ОШИБКА ${error.javaClass.simpleName}: ${error.message}")
        null
    }
}

/** Any nonempty Firebase string is a remote link, verbatim, regardless of its format. */
fun receivedLink(value: String?): String? = value?.takeIf(String::isNotEmpty)
