package ru.kvizcap.quiz.app.startup

import com.appsflyer.share.AppsFlyerConversionListener
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class AppsFlyerAttribution(
    private val resolver: AttributionResolver,
    private val log: (String) -> Unit = {},
    private val onDataReceived: () -> Unit = {},
) : AppsFlyerConversionListener, AttributionSource {
    val campaign: String? get() = resolver.campaign
    val parameters: Map<String, String> get() = resolver.parameters

    override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
        val status = InstallAttribution.fromStatus(data?.get("af_status"))
        log("AppsFlyer onConversionDataSuccess: af_status=${data?.get("af_status")}, is_first_launch=${data?.get("is_first_launch")}, media_source=${data?.get("media_source")}, campaign=${data?.get("campaign")}; распознан=$status")
        val parameters = attributionParameters(data)
        resolver.onSdkResult(status, parameters["campaign"], parameters)
        onDataReceived()
    }

    override fun onConversionDataFail(error: String?) { unavailable("onConversionDataFail: $error") }
    override suspend fun awaitAttribution(): InstallAttribution? = resolver.awaitAttribution()
    fun unavailable(reason: String = "SDK недоступен") {
        log("AppsFlyer: $reason; продолжаем ждать корректные данные до общего лимита")
        resolver.onSdkResult(null)
    }
}

class FirebasePrivacyLinks(private val log: (String) -> Unit = {}) : PrivacyLinkSource {
    override suspend fun fetch(field: String): String? = suspendCancellableCoroutine { continuation ->
        val reference = FirebaseDatabase.getInstance().reference.child(field)
        log("Firebase GET: $reference")
        continuation.invokeOnCancellation { log("Firebase /$field: ожидание отменено, поздний callback будет проигнорирован") }
        reference.get()
            .addOnSuccessListener { snapshot ->
                log("Firebase callback: exists=${snapshot.exists()}, тип=${snapshot.value?.javaClass?.simpleName}, значение=${snapshot.value}; ожидание активно=${continuation.isActive}")
                if (continuation.isActive) continuation.resume(snapshot.value as? String)
            }
            .addOnFailureListener { error ->
                log("Firebase callback ОШИБКА: ${error.javaClass.simpleName}: ${error.message}; ожидание активно=${continuation.isActive}")
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        // Cancellation invalidates this continuation; late responses are ignored.
    }
}
