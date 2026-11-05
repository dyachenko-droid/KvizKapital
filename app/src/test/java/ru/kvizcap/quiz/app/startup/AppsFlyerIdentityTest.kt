package ru.kvizcap.quiz.app.startup

import org.junit.Assert.assertEquals
import org.junit.Test

class AppsFlyerIdentityTest {
    @Test fun oneSignalLoginUsesTheSameIdReturnedForTheLink() {
        val logins = mutableListOf<String>()
        val identity = AppsFlyerIdentity({ "af-123" }, logins::add)
        assertEquals("af-123", identity.sync())
        assertEquals("af-123", identity.sync())
        assertEquals(listOf("af-123"), logins)
    }

    @Test fun missingIdIsRetriedWhenItBecomesAvailable() {
        var id = ""
        val logins = mutableListOf<String>()
        val identity = AppsFlyerIdentity({ id }, logins::add)
        assertEquals("", identity.sync())
        assertEquals(emptyList<String>(), logins)
        id = "ready-id"
        assertEquals("ready-id", identity.sync())
        assertEquals(listOf("ready-id"), logins)
    }

    @Test fun failedLoginDoesNotBlockTheLinkAndIsRetried() {
        var attempts = 0
        val identity = AppsFlyerIdentity({ "af-id" }, { attempts++; if (attempts == 1) error("not ready") })
        assertEquals("af-id", identity.sync())
        assertEquals("af-id", identity.sync())
        identity.sync()
        assertEquals(2, attempts)
    }
}
