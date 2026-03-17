package com.passbolt.mobile.android.common.extension

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StringExtTest {
    @Test
    fun `toSingleLine replaces newlines with spaces`() {
        assertThat("line1\nline2\nline3".toSingleLine()).isEqualTo("line1 line2 line3")
    }

    @Test
    fun `toSingleLine replaces carriage returns with spaces`() {
        assertThat("line1\r\nline2\r\nline3".toSingleLine()).isEqualTo("line1 line2 line3")
    }

    @Test
    fun `toSingleLine collapses multiple newlines into single space`() {
        assertThat("line1\n\n\nline2".toSingleLine()).isEqualTo("line1 line2")
    }

    @Test
    fun `toSingleLine trims leading and trailing whitespace`() {
        assertThat("\nline1\n".toSingleLine()).isEqualTo("line1")
    }

    @Test
    fun `toSingleLine returns same string when no newlines`() {
        assertThat("no newlines here".toSingleLine()).isEqualTo("no newlines here")
    }

    @Test
    fun `toSingleLine handles empty string`() {
        assertThat("".toSingleLine()).isEqualTo("")
    }
}
