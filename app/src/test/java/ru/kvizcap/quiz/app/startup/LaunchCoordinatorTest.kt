package ru.kvizcap.quiz.app.startup

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LaunchCoordinatorTest {
    private val remote = "https://example.com/privacy?campaign=test"

    private class Preferences(
        override var isFirstLaunch: Boolean = true,
        override var privacyUrl: String = DEFAULT_PRIVACY_URL,
        override var hasRemotePrivacyUrl: Boolean = false,
    ) : LaunchPreferences {
        override fun savePrivacyUrl(url: String) { privacyUrl = url; hasRemotePrivacyUrl = false }
        override fun completeFirstLaunch(url: String, fromFirebase: Boolean) {
            privacyUrl = url
            isFirstLaunch = false
            hasRemotePrivacyUrl = fromFirebase
        }
    }

    @Test fun organicInstallReadsOrgAndPersistsRemoteLink() = runTest {
        val prefs = Preferences()
        var field = ""
        val coordinator = LaunchCoordinator(prefs, { InstallAttribution.ORGANIC }, {
            field = it
            remote
        })
        assertEquals(LaunchRoute.Web(remote, false), coordinator.resolve())
        assertEquals("org", field)
        assertEquals(remote, prefs.privacyUrl)
        assertFalse(prefs.isFirstLaunch)
    }

    @Test fun nonOrganicSdkCampaignDecoratesTheLinkBeforeSavingAndNeverAgain() = runTest {
        val prefs = Preferences()
        val source = AppsFlyerAttribution(AttributionResolver({ null }))
        source.onConversionDataFail("temporary error")
        source.onConversionDataSuccess(mutableMapOf("af_status" to "unknown"))
        source.onConversionDataSuccess(mutableMapOf("af_status" to "Non-organic", "campaign" to "one_two", "ad_id" to "123", "af_ad" to "Creative"))
        source.onConversionDataSuccess(mutableMapOf("af_status" to "Organic", "campaign" to "wrong_campaign"))
        val coordinator = LaunchCoordinator(prefs, source, { "$remote&ad={{ad.id}}&name={{ad.name}}" }, nonOrganicLink = {
            nonOrganicLink(it, "af-id", source.campaign, source.parameters)
        })
        val expected = "$remote&ad=123&name=Creative&app_id=af-id&sub1=one&sub2=two"
        assertEquals(LaunchRoute.Web(expected, false), coordinator.resolve())
        assertEquals(expected, prefs.privacyUrl)
        assertTrue(prefs.hasRemotePrivacyUrl)
        assertFalse(prefs.isFirstLaunch)
        val cached = LaunchCoordinator(prefs, { error("Must use cache") }, { error("Must use cache") },
            nonOrganicLink = { error("Must not decorate a second time") })
        assertEquals(LaunchRoute.Web(expected, false), cached.resolve())
    }

    @Test fun organicLinksAndDefaultPrivacyAreNeverDecorated() = runTest {
        val organic = LaunchCoordinator(Preferences(), { InstallAttribution.ORGANIC }, { "12ук" },
            nonOrganicLink = { error("Only in_org links are decorated") })
        assertEquals(LaunchRoute.Web("12ук", false), organic.resolve())
        val fallback = LaunchCoordinator(Preferences(), { InstallAttribution.NON_ORGANIC }, { null },
            nonOrganicLink = { error("Default privacy must not be decorated") })
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, true), fallback.resolve())
    }

    @Test fun nonOrganicInstallReadsInOrg() = runTest {
        var field = ""
        val coordinator = LaunchCoordinator(Preferences(), { InstallAttribution.NON_ORGANIC }, {
            field = it
            remote
        })
        assertEquals(LaunchRoute.Web(remote, false), coordinator.resolve())
        assertEquals("in_org", field)
    }

    @Test fun savedRemoteLinkOpensWithoutAnyNetworkCalls() = runTest {
        val coordinator = LaunchCoordinator(Preferences(false, remote, true),
            { error("Attribution must not be requested again") },
            { error("Firebase must not be requested again") })
        assertEquals(LaunchRoute.Web(remote, false), coordinator.resolve())
    }

    @Test fun missingOrEmptyFirebaseValueRequiresAcceptance() = runTest {
        for (value in listOf(null, "")) {
            val prefs = Preferences()
            val coordinator = LaunchCoordinator(prefs, { InstallAttribution.ORGANIC }, { value })
            assertEquals("value=$value", LaunchRoute.Web(DEFAULT_PRIVACY_URL, true), coordinator.resolve())
            assertTrue(prefs.isFirstLaunch)
            assertEquals(DEFAULT_PRIVACY_URL, prefs.privacyUrl)
        }
    }

    @Test fun databasePermissionOrConnectionFailureUsesDefault() = runTest {
        val coordinator = LaunchCoordinator(Preferences(), { InstallAttribution.ORGANIC }, {
            throw IllegalStateException("Permission denied")
        })
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, true), coordinator.resolve())
    }

    @Test fun unknownAttributionDoesNotGuessOrganic() = runTest {
        val coordinator = LaunchCoordinator(Preferences(), { null }, {
            error("Unknown status must not query org or in_org")
        })
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, true), coordinator.resolve())
    }

    @Test fun attributionFailureUsesDefault() = runTest {
        val coordinator = LaunchCoordinator(Preferences(), { throw IllegalStateException("SDK unavailable") }, {
            error("Firebase must not run")
        })
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, true), coordinator.resolve())
    }

    @Test fun attributionTimeoutReadsOrgAndIgnoresLateNonOrganicCallback() = runTest {
        val remote = "12ук"
        val result = CompletableDeferred<InstallAttribution?>()
        val prefs = Preferences()
        val fields = mutableListOf<String>()
        val coordinator = LaunchCoordinator(prefs, { result.await() }, { fields += it; remote }, attributionTimeoutMillis = 100)
        assertEquals(LaunchRoute.Web(remote, false), coordinator.resolve())
        result.complete(InstallAttribution.NON_ORGANIC)
        runCurrent()
        assertEquals(listOf("org"), fields)
        assertEquals(remote, prefs.privacyUrl)
        assertFalse(prefs.isFirstLaunch)
        assertTrue(prefs.hasRemotePrivacyUrl)
    }

    @Test fun lateNonOrganicCallbackDuringFirebaseFetchDoesNotSwitchField() = runTest {
        val attribution = CompletableDeferred<InstallAttribution?>()
        val database = CompletableDeferred<String?>()
        val fields = mutableListOf<String>()
        val coordinator = LaunchCoordinator(Preferences(), { attribution.await() }, {
            fields += it
            attribution.complete(InstallAttribution.NON_ORGANIC)
            database.await()
        }, attributionTimeoutMillis = 100)
        val decision = CompletableDeferred<LaunchRoute>()
        launch { decision.complete(coordinator.resolve()) }
        testScheduler.advanceTimeBy(101)
        runCurrent()
        assertEquals(listOf("org"), fields)
        assertFalse(decision.isCompleted)
        database.complete(remote)
        assertEquals(LaunchRoute.Web(remote, false), decision.await())
        assertEquals(listOf("org"), fields)
    }

    @Test fun attributionTimeoutThenEmptyOrgFallsBackToDefault() = runTest {
        val prefs = Preferences()
        val fields = mutableListOf<String>()
        val coordinator = LaunchCoordinator(prefs, { awaitCancellation() }, {
            fields += it
            ""
        }, attributionTimeoutMillis = 100)
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, true), coordinator.resolve())
        assertEquals(listOf("org"), fields)
        assertTrue(prefs.isFirstLaunch)
    }

    @Test fun defaultTimeoutsAllow15SecondsForAppsFlyerThen8ForFirebase() = runTest {
        val fields = mutableListOf<String>()
        val coordinator = LaunchCoordinator(Preferences(), { awaitCancellation() }, {
            fields += it
            awaitCancellation()
        })
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, true), coordinator.resolve())
        assertEquals(listOf("org"), fields)
        assertEquals(23_000L, testScheduler.currentTime)
    }

    @Test fun databaseTimeoutUsesDefaultAndIgnoresLateValue() = runTest {
        val result = CompletableDeferred<String?>()
        val prefs = Preferences()
        val coordinator = LaunchCoordinator(prefs, { InstallAttribution.NON_ORGANIC }, { result.await() }, databaseTimeoutMillis = 100)
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, true), coordinator.resolve())
        result.complete(remote)
        runCurrent()
        assertEquals(DEFAULT_PRIVACY_URL, prefs.privacyUrl)
        assertTrue(prefs.isFirstLaunch)
    }

    @Test fun acceptingDefaultPolicyEntersNativeAppOnSubsequentLaunches() = runTest {
        val prefs = Preferences()
        val coordinator = LaunchCoordinator(prefs, { null }, { null })
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, true), coordinator.resolve())
        coordinator.acceptPrivacy()
        assertFalse(prefs.isFirstLaunch)
        assertEquals(LaunchRoute.Native, coordinator.resolve())
    }

    @Test fun defaultPolicyRemainsUntilExplicitAcceptance() = runTest {
        val prefs = Preferences()
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, true), LaunchCoordinator(prefs, { null }, { null }).resolve())
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, true), LaunchCoordinator(prefs, { null }, { null }).resolve())
        assertTrue(prefs.isFirstLaunch)
    }

    @Test fun firebaseReturningDefaultUrlStillCountsAsRemote() = runTest {
        val prefs = Preferences()
        val coordinator = LaunchCoordinator(prefs, { InstallAttribution.ORGANIC }, { DEFAULT_PRIVACY_URL })
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, false), coordinator.resolve())
        assertFalse(prefs.isFirstLaunch)
        assertTrue(prefs.hasRemotePrivacyUrl)
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, false), coordinator.resolve())
    }

    @Test fun cancellationDoesNotPersistFallback() = runTest {
        val prefs = Preferences(privacyUrl = remote)
        val coordinator = LaunchCoordinator(prefs, { awaitCancellation() }, { null })
        val job = launch { coordinator.resolve() }
        runCurrent()
        job.cancel()
        job.join()
        assertEquals(remote, prefs.privacyUrl)
        assertTrue(prefs.isFirstLaunch)
    }

    @Test fun emptyCompletedPreferenceFallsBackToNative() = runTest {
        val coordinator = LaunchCoordinator(Preferences(false, "", true),
            { error("Must not request attribution") }, { error("Must not request Firebase") })
        assertEquals(LaunchRoute.Native, coordinator.resolve())
    }

    @Test fun mapsOnlyKnownAppsFlyerStatuses() {
        assertEquals(InstallAttribution.ORGANIC, InstallAttribution.fromStatus("Organic"))
        assertEquals(InstallAttribution.NON_ORGANIC, InstallAttribution.fromStatus("Non-organic"))
        assertNull(InstallAttribution.fromStatus(null))
        assertNull(InstallAttribution.fromStatus("unknown"))
    }

    @Test fun diagnosticsIdentifyEmptyFirebaseStringAndSelectedField() = runTest {
        val logs = mutableListOf<String>()
        val coordinator = LaunchCoordinator(Preferences(), { InstallAttribution.NON_ORGANIC }, { "" }, log = logs::add)
        assertEquals(LaunchRoute.Web(DEFAULT_PRIVACY_URL, true), coordinator.resolve())
        assertTrue(logs.any { it.contains("/in_org") })
        assertTrue(logs.any { it.contains("непустая строка получена=false") })
        assertTrue(logs.any { it.contains("ЗАПАСНАЯ PRIVACY: Firebase: получена пустая строка") })
    }

    @Test fun nonOrganicBareGoogleDomainOpensFirebaseUrlWithoutAcceptanceAndPersists() = runTest {
        val prefs = Preferences()
        val logs = mutableListOf<String>()
        val fields = mutableListOf<String>()
        val coordinator = LaunchCoordinator(prefs, { InstallAttribution.NON_ORGANIC }, {
            fields += it
            "google.com"
        }, log = logs::add)
        assertEquals(LaunchRoute.Web("google.com", false), coordinator.resolve())
        assertEquals(listOf("in_org"), fields)
        assertEquals("google.com", prefs.privacyUrl)
        assertFalse(prefs.isFirstLaunch)
        assertTrue(prefs.hasRemotePrivacyUrl)
        assertTrue(logs.any { it.contains("строка принята без изменений: google.com") })
        assertFalse(logs.any { it.startsWith("ЗАПАСНАЯ PRIVACY:") })
        assertEquals(LaunchRoute.Web("google.com", false),
            LaunchCoordinator(prefs, { error("Must use cache") }, { error("Must use cache") }).resolve())
    }

    @Test fun organicBareDomainUsesOrg() = runTest {
        val fields = mutableListOf<String>()
        val coordinator = LaunchCoordinator(Preferences(), { InstallAttribution.ORGANIC }, {
            fields += it
            "ya.ru"
        })
        assertEquals(LaunchRoute.Web("ya.ru", false), coordinator.resolve())
        assertEquals(listOf("org"), fields)
    }

    @Test fun cachedBareRemoteDomainIsPassedUnchangedWithoutNetwork() = runTest {
        val coordinator = LaunchCoordinator(Preferences(false, "google.com", true),
            { error("Must use cache") }, { error("Must use cache") })
        assertEquals(LaunchRoute.Web("google.com", false), coordinator.resolve())
    }

    @Test fun acceptedUrlsAreReturnedExactlyAsReceived() {
        for (input in listOf(
            "google.com", "example.com:8443/path?x=1#section", "//example.com/path",
            "http://example.com/path", "HTTPS://EXAMPLE.com/Path",
            "example.com/path?next=https://other.example/page",
            "https://example.com/a%2Fb?x=a+b&x=a%20b&next=https%3A%2F%2Fother.example%2F#Fragment",
        )) assertEquals(input, input, receivedLink(input))
    }

    @Test fun arbitraryNonemptyStringsAreAcceptedAsFirebaseLinks() = runTest {
        for (input in listOf("12ук", " ", "not a URL", "https://", "/relative/path", "app://anything", " https://example.com ")) {
            val prefs = Preferences()
            val coordinator = LaunchCoordinator(prefs, { InstallAttribution.NON_ORGANIC }, { input })
            assertEquals(input, LaunchRoute.Web(input, false), coordinator.resolve())
            assertEquals(input, prefs.privacyUrl)
            assertFalse(prefs.isFirstLaunch)
            assertTrue(prefs.hasRemotePrivacyUrl)
        }
    }

    @Test fun unicodeText12ukIsSavedAndReopenedWithoutNetworkOrAcceptance() = runTest {
        val prefs = Preferences()
        val fields = mutableListOf<String>()
        val coordinator = LaunchCoordinator(prefs, { InstallAttribution.NON_ORGANIC }, {
            fields += it
            "12ук"
        })
        assertEquals(LaunchRoute.Web("12ук", false), coordinator.resolve())
        assertEquals(listOf("in_org"), fields)
        assertEquals("12ук", prefs.privacyUrl)
        assertFalse(prefs.isFirstLaunch)
        assertTrue(prefs.hasRemotePrivacyUrl)
        val repeated = LaunchCoordinator(prefs,
            { error("Must reopen cached text") }, { error("Must reopen cached text") })
        assertEquals(LaunchRoute.Web("12ук", false), repeated.resolve())
    }

    @Test fun diagnosticsDistinguishTimeoutFromCompletedUnknownStatus() = runTest {
        val timeoutLogs = mutableListOf<String>()
        LaunchCoordinator(Preferences(), { awaitCancellation() }, { null }, attributionTimeoutMillis = 10, log = timeoutLogs::add).resolve()
        assertTrue(timeoutLogs.any { it.contains("AppsFlyer: ТАЙМАУТ 10мс") })
        assertTrue(timeoutLogs.any { it.contains("считаем установку ORGANIC") })
        assertTrue(timeoutLogs.any { it.contains("Firebase: выбран корневой путь /org") })
        val nullLogs = mutableListOf<String>()
        LaunchCoordinator(Preferences(), { null }, { null }, log = nullLogs::add).resolve()
        assertFalse(nullLogs.any { it.contains("ТАЙМАУТ") })
        assertTrue(nullLogs.any { it.contains("результат для выбора маршрута=null") })
    }

    @Test fun diagnosticsContainDatabaseErrorAndFallbackDecision() = runTest {
        val logs = mutableListOf<String>()
        LaunchCoordinator(Preferences(), { InstallAttribution.NON_ORGANIC }, { error("Permission denied") }, log = logs::add).resolve()
        assertTrue(logs.any { it.contains("Firebase /in_org: ОШИБКА IllegalStateException: Permission denied") })
        assertTrue(logs.any { it.startsWith("ЗАПАСНАЯ PRIVACY:") })
    }

    @Test fun diagnosticsExplainWhenPreferencesSkipFirebase() = runTest {
        val logs = mutableListOf<String>()
        LaunchCoordinator(Preferences(false, remote, true), { error("No attribution") }, { error("No Firebase") }, log = logs::add).resolve()
        assertTrue(logs.any { it.contains("first_launch=false, remote_privacy=true") })
        assertTrue(logs.any { it.contains("AppsFlyer/Firebase не запрашиваем") })
    }
}
