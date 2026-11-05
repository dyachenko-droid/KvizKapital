package ru.kvizcap.quiz.app.startup

import org.junit.Assert.assertEquals
import org.junit.Test

class AppsFlyerSessionsTest {
    @Test fun onlineNetworkAloneDoesNotStartBeforeSdkReady() {
        var starts = 0
        val sessions = AppsFlyerSessions({ true }, { starts++ })
        sessions.onNetworkChanged(true)
        assertEquals(0, starts)
        sessions.onSdkReady()
        sessions.onNetworkChanged(true)
        assertEquals(1, starts)
    }

    @Test fun sdkReadinessWaitsForNetwork() {
        var starts = 0
        val sessions = AppsFlyerSessions({ true }, { starts++ })
        sessions.onSdkReady()
        assertEquals(0, starts)
        sessions.onNetworkChanged(true)
        assertEquals(1, starts)
    }

    @Test fun subsequentForegroundReadinessStartsAnotherSession() {
        var starts = 0
        var ready = true
        val sessions = AppsFlyerSessions({ ready }, { starts++; ready = false })
        sessions.onNetworkChanged(true)
        sessions.onSdkReady()
        assertEquals(1, starts)
        ready = true
        sessions.onSdkReady()
        assertEquals(2, starts)
    }

    @Test fun staleReadySignalCannotStartSessionAfterBackgrounding() {
        var starts = 0
        var ready = true
        val sessions = AppsFlyerSessions({ ready }, { starts++ })
        sessions.onSdkReady()
        ready = false
        sessions.onNetworkChanged(true)
        assertEquals(0, starts)
        ready = true
        sessions.onSdkReady()
        assertEquals(1, starts)
    }
}
