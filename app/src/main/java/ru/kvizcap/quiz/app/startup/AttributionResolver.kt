package ru.kvizcap.quiz.app.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AttributionData(
    val status: InstallAttribution,
    val campaign: String? = null,
    val parameters: Map<String, String> = emptyMap(),
)

/** SDK maps may contain nulls or structured values; only scalar values can fill URL macros. */
internal fun attributionParameters(data: Map<String, Any?>?): Map<String, String> = buildMap {
    data?.forEach { (key, value) ->
        if (value is String || value is Number || value is Boolean) {
            value.toString().takeIf { it.isNotEmpty() }?.let { put(key, it) }
        }
    }
}

fun interface InstallDataSource {
    suspend fun fetch(): AttributionData?
}

/** Plunko-style organic grace and GCD refresh, bounded by LaunchCoordinator's deadline. */
class AttributionResolver(
    private val installData: InstallDataSource,
    private val log: (String) -> Unit = {},
) : AttributionSource {
    private val sdkData = MutableStateFlow<AttributionData?>(null)
    @Volatile
    private var resolvedData: AttributionData? = null
    val campaign: String? get() = resolvedData?.campaign
    val parameters: Map<String, String> get() = resolvedData?.parameters.orEmpty()

    fun onSdkResult(status: InstallAttribution?, campaign: String? = null, parameters: Map<String, String> = emptyMap()) {
        // An error/unknown response must not erase a known attribution; paid wins a race.
        sdkData.update { previous ->
            when {
                previous?.status == InstallAttribution.NON_ORGANIC && status != InstallAttribution.NON_ORGANIC -> previous
                status != null -> {
                    val matching = previous?.takeIf { it.status == status }
                    AttributionData(status, campaign ?: matching?.campaign, matching?.parameters.orEmpty() + parameters)
                }
                else -> previous
            }
        }
    }

    override suspend fun awaitAttribution(): InstallAttribution = coroutineScope {
        val result = CompletableDeferred<InstallAttribution>()
        var inFlight: Deferred<AttributionData?>? = null

        suspend fun refresh(): AttributionData? {
            val request = inFlight?.takeIf { it.isActive } ?: async {
                try { installData.fetch() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    log("AppsFlyer GCD: ошибка ${error.javaClass.simpleName}; продолжаем ожидание")
                    null
                }
            }.also { inFlight = it }
            return request.await()
        }

        fun publish(refreshed: AttributionData?, sdkFallback: AttributionData? = null) {
            val sdk = sdkData.value
            val data = if (sdk?.status == InstallAttribution.NON_ORGANIC) {
                val matching = refreshed?.takeIf { it.status == sdk.status }
                sdk.copy(
                    campaign = sdk.campaign ?: matching?.campaign,
                    parameters = matching?.parameters.orEmpty() + sdk.parameters,
                )
            } else if (refreshed != null) {
                val matching = sdkFallback?.takeIf { it.status == refreshed.status }
                refreshed.copy(
                    campaign = refreshed.campaign ?: matching?.campaign,
                    parameters = matching?.parameters.orEmpty() + refreshed.parameters,
                )
            } else sdkFallback
            if (data != null && !result.isCompleted) {
                resolvedData = data
                result.complete(data.status)
                log("AppsFlyer: уточнение завершено, статус для Firebase=${data.status}")
            }
        }

        val observer = launch {
            sdkData.map { it?.status }.filterNotNull().distinctUntilChanged().collectLatest { status ->
                when (status) {
                    InstallAttribution.NON_ORGANIC -> publish(sdkData.value)
                    InstallAttribution.ORGANIC -> {
                        log("AppsFlyer: Organic из SDK; ждём 5000мс перед уточнением GCD")
                        delay(5_000)
                        publish(refresh(), sdkData.value)
                    }
                }
            }
        }
        val retries = launch {
            delay(10_000)
            repeat(3) { attempt ->
                if (result.isCompleted) return@launch
                log("AppsFlyer: уточнение отсутствующего результата GCD, попытка ${attempt + 1}/3")
                publish(refresh(), sdkData.value)
                delay(3_000)
            }
        }
        try {
            result.await()
        } finally {
            observer.cancel()
            retries.cancel()
            inFlight?.cancel()
            log("AppsFlyer: ожидание атрибуции завершено; дальнейшие callbacks только в журнале и памяти SDK")
        }
    }
}
