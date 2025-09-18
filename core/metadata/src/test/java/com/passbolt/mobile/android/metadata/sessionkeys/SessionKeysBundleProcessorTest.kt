package com.passbolt.mobile.android.metadata.sessionkeys

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionKeysBundleProcessorTest {
    private val processor = SessionKeysBundleProcessor()

    @Test
    fun `marshalFetchedSessionKey returns string for plain string`() {
        val input = "key"
        val result = processor.marshalFetchedSessionKey(input)
        assertEquals("key", result)
    }

    @Test
    fun `marshalFetchedSessionKey returns string for prefixed string`() {
        val input = "9:key"
        val result = processor.marshalFetchedSessionKey(input)
        assertEquals("key", result)
    }

    @Test
    fun `marshalSessionKeyPrePush returns uppercase for plain string`() {
        val input = "key"
        val result = processor.marshalSessionKeyPrePush(input)
        assertEquals("9:KEY", result)
    }

    @Test
    fun `marshalSessionKeyPrePush returns uppercase for already prefixed string`() {
        val input = "9:key"
        val result = processor.marshalSessionKeyPrePush(input)
        assertEquals("9:KEY", result)
    }
}
