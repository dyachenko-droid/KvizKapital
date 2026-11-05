package ru.kvizcap.quiz.app.startup

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Same install-data endpoint as Plunko; never log the URL containing devkey/device_id. */
class GcdAttributionApi(
    private val packageName: String,
    private val devKey: String,
    private val deviceId: () -> String,
    private val log: (String) -> Unit = {},
) : InstallDataSource {
    private val executor = Executors.newFixedThreadPool(2) { work ->
        Thread(work, "appsflyer-gcd").apply { isDaemon = true }
    }

    override suspend fun fetch(): AttributionData? {
        val id = runCatching(deviceId).getOrDefault("")
        if (id.isBlank()) {
            log("AppsFlyer GCD: UID ещё недоступен, запрос пропущен")
            return null
        }
        return suspendCancellableCoroutine { continuation ->
            val connection = AtomicReference<HttpURLConnection?>()
            val work = executor.submit {
                try {
                    val url = URI("https://gcdsdk.appsflyer.com/install_data/v4.0/$packageName?device_id=${encode(id)}&devkey=${encode(devKey)}").toURL()
                    val request = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8_000
                        readTimeout = 8_000
                        instanceFollowRedirects = false
                        setRequestProperty("Accept", "application/json")
                    }
                    connection.set(request)
                    if (!continuation.isActive) return@submit
                    log("AppsFlyer GCD: GET install_data; package=$packageName")
                    val code = request.responseCode
                    val status = if (code in 200..299) {
                        request.inputStream.bufferedReader(Charsets.UTF_8).use { parseGcdData(it.readText()) }
                    } else null
                    log("AppsFlyer GCD: HTTP=$code; распознанный af_status=$status; ожидание активно=${continuation.isActive}")
                    if (continuation.isActive) continuation.resume(status)
                } catch (error: Exception) {
                    log("AppsFlyer GCD: ошибка ${error.javaClass.simpleName}; ожидание активно=${continuation.isActive}")
                    if (continuation.isActive) continuation.resume(null)
                } finally {
                    connection.get()?.disconnect()
                }
            }
            continuation.invokeOnCancellation {
                log("AppsFlyer GCD: запрос отменён по завершению ожидания")
                // Disconnect off the main thread: Android's HTTP implementation can block here.
                work.cancel(true)
                executor.execute { connection.get()?.disconnect() }
            }
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

internal fun parseGcdData(body: String): AttributionData? = try {
    val data = Json.parseToJsonElement(body).jsonObject
    val parameters = data.mapNotNull { (key, value) ->
        (value as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() }?.let { key to it }
    }.toMap()
    val status = InstallAttribution.fromStatus(parameters["af_status"])
    status?.let { AttributionData(it, parameters["campaign"], parameters) }
} catch (_: Exception) { null }

internal fun parseGcdStatus(body: String): InstallAttribution? = parseGcdData(body)?.status
