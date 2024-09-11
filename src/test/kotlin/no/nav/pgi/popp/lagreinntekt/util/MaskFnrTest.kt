package no.nav.pgi.popp.lagreinntekt.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MaskFnrTest {

    @Test
    fun `mask zero of ten digits`() {
        assertEquals("1234567890", "1234567890".maskFnr())
    }

    @Test
    fun `mask last five of eleven digits`() {
        assertEquals("123456*****", "12345678901".maskFnr())
    }

    @Test
    fun `mask last five of eleven digits if digits are in the middle of characters`() {
        assertEquals("a123456*****s", "a12345678901s".maskFnr())
    }

    @Test
    fun `should mask several subsequent fnrs`() {
        assertEquals("123456***** 234567*****", "12345678901 23456789012".maskFnr())
    }
}