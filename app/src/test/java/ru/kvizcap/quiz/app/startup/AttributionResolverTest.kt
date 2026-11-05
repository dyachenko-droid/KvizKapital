package ru.kvizcap.quiz.app.startup

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AttributionResolverTest {
    @Test fun nonOrganicSdkResultResolvesImmediatelyWithoutHttp() = runTest {
        val resolver = AttributionResolver({ error("Must not query GCD") })
        resolver.onSdkResult(InstallAttribution.NON_ORGANIC)
        assertEquals(InstallAttribution.NON_ORGANIC, resolver.awaitAttribution())
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test fun organicSdkResultIsRefinedAfterFiveSeconds() = runTest {
        var requests = 0
        val resolver = AttributionResolver({ requests++; AttributionData(InstallAttribution.NON_ORGANIC, "gcd_campaign") })
        resolver.onSdkResult(InstallAttribution.ORGANIC, "preliminary_organic_campaign")
        assertEquals(InstallAttribution.NON_ORGANIC, resolver.awaitAttribution())
        assertEquals("gcd_campaign", resolver.campaign)
        assertEquals(5_000L, testScheduler.currentTime)
        assertEquals(1, requests)
    }

    @Test fun failedOrganicRefreshUsesSdkOrganicResult() = runTest {
        val resolver = AttributionResolver({ null })
        resolver.onSdkResult(InstallAttribution.ORGANIC)
        assertEquals(InstallAttribution.ORGANIC, resolver.awaitAttribution())
        assertEquals(5_000L, testScheduler.currentTime)
    }

    @Test fun unknownResponseDoesNotPreventLaterKnownCallback() = runTest {
        val resolver = AttributionResolver({ error("Too early for GCD") })
        val result = async { resolver.awaitAttribution() }
        runCurrent()
        resolver.onSdkResult(null)
        testScheduler.advanceTimeBy(2_000)
        assertFalse(result.isCompleted)
        resolver.onSdkResult(InstallAttribution.NON_ORGANIC)
        assertEquals(InstallAttribution.NON_ORGANIC, result.await())
    }

    @Test fun nonOrganicCallbackDuringGraceCancelsOrganicBranch() = runTest {
        val resolver = AttributionResolver({ error("Cancelled grace must not query GCD") })
        resolver.onSdkResult(InstallAttribution.ORGANIC)
        val result = async { resolver.awaitAttribution() }
        testScheduler.advanceTimeBy(2_000)
        resolver.onSdkResult(InstallAttribution.NON_ORGANIC)
        assertEquals(InstallAttribution.NON_ORGANIC, result.await())
        assertEquals(2_000L, testScheduler.currentTime)
    }

    @Test fun nonOrganicCallbackCancelsPendingHttpRequest() = runTest {
        var cancelled = false
        val resolver = AttributionResolver({
            try { awaitCancellation() } finally { cancelled = true }
        })
        resolver.onSdkResult(InstallAttribution.ORGANIC)
        val result = async { resolver.awaitAttribution() }
        testScheduler.advanceTimeBy(5_001)
        resolver.onSdkResult(InstallAttribution.NON_ORGANIC)
        assertEquals(InstallAttribution.NON_ORGANIC, result.await())
        assertTrue(cancelled)
    }

    @Test fun missingSdkResultCanBeRecoveredByGcd() = runTest {
        val resolver = AttributionResolver({ AttributionData(InstallAttribution.NON_ORGANIC) })
        assertEquals(InstallAttribution.NON_ORGANIC, resolver.awaitAttribution())
        assertEquals(10_000L, testScheduler.currentTime)
    }

    @Test fun missingDataRetriesWithin15SecondsThenReadsOrg() = runTest {
        var requests = 0
        val resolver = AttributionResolver({ requests++; null })
        val fields = mutableListOf<String>()
        val coordinator = LaunchCoordinator(Preferences(), resolver, { fields += it; REMOTE })
        assertEquals(LaunchRoute.Web(REMOTE, false), coordinator.resolve())
        assertEquals(listOf("org"), fields)
        assertEquals(2, requests) // 10s and 13s, third attempt cannot outlive the 15s deadline.
        assertEquals(15_000L, testScheduler.currentTime)
    }

    @Test fun stalledOrganicHttpRequestCannotExtendLaunchDeadline() = runTest {
        var cancelled = false
        val resolver = AttributionResolver({
            try { awaitCancellation() } finally { cancelled = true }
        })
        resolver.onSdkResult(InstallAttribution.ORGANIC)
        val fields = mutableListOf<String>()
        val coordinator = LaunchCoordinator(Preferences(), resolver, { fields += it; REMOTE })
        assertEquals(LaunchRoute.Web(REMOTE, false), coordinator.resolve())
        assertEquals(listOf("org"), fields)
        assertEquals(15_000L, testScheduler.currentTime)
        assertTrue(cancelled)
    }

    @Test fun gcdParserAcceptsOnlyKnownStatusAndHandlesBadResponses() {
        assertEquals(InstallAttribution.NON_ORGANIC, parseGcdStatus("""{"af_status":"Non-organic"}"""))
        assertEquals(InstallAttribution.ORGANIC, parseGcdStatus("""{"af_status":"ORGANIC"}"""))
        for (body in listOf("{}", "[]", "invalid", """{"af_status":null}""", """{"af_status":"unknown"}""")) {
            assertNull(body, parseGcdStatus(body))
        }
    }

    @Test fun gcdParserRetainsCampaignWithTheSelectedAttribution() {
        assertEquals(AttributionData(InstallAttribution.NON_ORGANIC, "one_two", mapOf("af_status" to "Non-organic", "campaign" to "one_two")),
            parseGcdData("""{"af_status":"Non-organic","campaign":"one_two"}"""))
    }

    @Test fun gcdParserKeepsScalarFieldsAndIgnoresStructuredAndNullValues() {
        val result = parseGcdData("""{"af_status":"Non-organic","campaign":{"invalid":true},"ad_id":123,"adset_id":"456","is_first_launch":true,"empty":"","missing":null,"structured":[],"af_ad":"Creative"}""")!!
        assertNull(result.campaign)
        assertEquals(mapOf("af_status" to "Non-organic", "ad_id" to "123", "adset_id" to "456", "is_first_launch" to "true", "af_ad" to "Creative"), result.parameters)
    }

    @Test fun sdkMetadataSurvivesAnIncompleteCallbackAndDoesNotMixOrganicValues() = runTest {
        val resolver = AttributionResolver({ error("No GCD needed") })
        val sdk = AppsFlyerAttribution(resolver)
        val payload = mutableMapOf<String, Any>("af_status" to "Non-organic", "campaign" to "one_two", "ad_id" to 123, "adset" to "Group")
        sdk.onConversionDataSuccess(payload)
        payload["ad_id"] = "mutated"
        sdk.onConversionDataSuccess(mutableMapOf("af_status" to "Non-organic", "af_ad" to "Creative"))
        sdk.onConversionDataSuccess(mutableMapOf("af_status" to "Organic", "ad_id" to "wrong"))
        assertEquals(InstallAttribution.NON_ORGANIC, sdk.awaitAttribution())
        assertEquals("123", sdk.parameters["ad_id"])
        assertEquals("Creative", sdk.parameters["af_ad"])
        assertEquals("Group", sdk.parameters["adset"])
        assertEquals("one_two", sdk.campaign)
        sdk.onConversionDataSuccess(mutableMapOf("af_status" to "Non-organic", "ad_id" to "late"))
        assertEquals("123", sdk.parameters["ad_id"]) // Selected launch snapshot is stable.
    }

    @Test fun gcdMetadataReplacesOrganicSdkMetadataWhenRefinementFindsPaidInstall() = runTest {
        val resolver = AttributionResolver({ AttributionData(InstallAttribution.NON_ORGANIC, "gcd_campaign", mapOf("ad_id" to "paid-ad")) })
        resolver.onSdkResult(InstallAttribution.ORGANIC, parameters = mapOf("adset" to "organic-group"))
        assertEquals(InstallAttribution.NON_ORGANIC, resolver.awaitAttribution())
        assertEquals(mapOf("ad_id" to "paid-ad"), resolver.parameters)
    }

    private class Preferences : LaunchPreferences {
        override var isFirstLaunch = true
        override var privacyUrl = DEFAULT_PRIVACY_URL
        override var hasRemotePrivacyUrl = false
        override fun savePrivacyUrl(url: String) { privacyUrl = url }
        override fun completeFirstLaunch(url: String, fromFirebase: Boolean) {
            isFirstLaunch = false; privacyUrl = url; hasRemotePrivacyUrl = fromFirebase
        }
    }

    private companion object { const val REMOTE = "https://example.com/remote" }
}
