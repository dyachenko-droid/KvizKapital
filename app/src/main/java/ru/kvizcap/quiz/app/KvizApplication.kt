package ru.kvizcap.quiz.app

import android.app.Application
import com.appsflyer.AppsFlyerLib
import com.appsflyer.share.attribution.AppsFlyerRequestListener
import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.kvizcap.quiz.app.startup.AppsFlyerAttribution
import ru.kvizcap.quiz.app.startup.AppsFlyerIdentity
import ru.kvizcap.quiz.app.startup.AppsFlyerSessions
import ru.kvizcap.quiz.app.startup.AttributionResolver
import ru.kvizcap.quiz.app.startup.FirebasePrivacyLinks
import ru.kvizcap.quiz.app.startup.GcdAttributionApi
import ru.kvizcap.quiz.app.startup.LaunchCoordinator
import ru.kvizcap.quiz.app.startup.StartupPreferences
import ru.kvizcap.quiz.app.startup.nonOrganicLink
import ru.kvizcap.quiz.app.startup.startupNetwork

private const val APPSFLYER_DEV_KEY = "CMXkxjgQ3hhzsY4ELzWhcU"

class KvizApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val startupPreferences by lazy { StartupPreferences(this) }
    private val identity by lazy {
        AppsFlyerIdentity(
            idProvider = { AppsFlyerLib.getInstance().getAppsFlyerUID(this).orEmpty() },
            login = { OneSignal.login(it) },
        )
    }
    private val attribution by lazy {
        val installData = GcdAttributionApi(packageName, APPSFLYER_DEV_KEY, {
            AppsFlyerLib.getInstance().getAppsFlyerUID(this).orEmpty()
        })
        AppsFlyerAttribution(AttributionResolver(installData), onDataReceived = {
            applicationScope.launch { identity.sync() }
        })
    }
    val launchCoordinator by lazy {
        LaunchCoordinator(startupPreferences, attribution, FirebasePrivacyLinks(), nonOrganicLink = { raw ->
            nonOrganicLink(raw, identity.sync(), attribution.campaign, attribution.parameters)
        })
    }

    override fun onCreate() {
        super.onCreate()
        deleteSharedPreferences("startup_diagnostics")
        OneSignal.initWithContext(this, "4796ac94-a85d-47a1-a961-9c7970a3dc92")
        try {
            AppsFlyerLib.getInstance().apply {
                init(APPSFLYER_DEV_KEY, attribution, this@KvizApplication)
                identity.sync()
                registerConversionListener(attribution)
                val sessions = AppsFlyerSessions(
                    isSdkReady = { isSessionReady() },
                    startSession = {
                        identity.sync()
                        start(object : AppsFlyerRequestListener {
                            override fun onSuccess() { applicationScope.launch { identity.sync() } }
                            override fun onError(code: Int, error: String) = Unit
                        })
                    },
                )
                registerSessionReadyListener {
                    applicationScope.launch { sessions.onSdkReady() }
                }
                applicationScope.launch {
                    try { startupNetwork(this@KvizApplication).collect(sessions::onNetworkChanged) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { /* Routing still has its own attribution deadline. */ }
                }
            }
        } catch (_: Exception) {
            attribution.unavailable()
        }
    }
}
