package isim.ia2y.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.charset.StandardCharsets

class TextEncodingUtf8Test {
    @Test
    fun frenchAccents_roundTripUtf8_withoutMojibake() {
        val probe = "é à ç ù ê œ"
        val roundTrip = String(probe.toByteArray(StandardCharsets.UTF_8), StandardCharsets.UTF_8)

        assertEquals(probe, roundTrip)
        assertFalse(roundTrip.contains("Ã"))
        assertFalse(roundTrip.contains("Â"))
    }
}
