package com.sahandsoft.matnkhan

import com.sahandsoft.matnkhan.ocr.TextPostprocessor
import org.junit.Assert.assertEquals
import org.junit.Test

class TextPostprocessorTest {

    @Test
    fun `empty and blank input returns empty string`() {
        assertEquals("", TextPostprocessor.clean(""))
        assertEquals("", TextPostprocessor.clean("   \n \n\t "))
    }

    @Test
    fun `persian and latin digits are preserved`() {
        val input = "قیمت ۱۲۳ تومان و کد 456"
        assertEquals(input, TextPostprocessor.clean(input))
    }

    @Test
    fun `arabic-indic digits are converted to persian digits`() {
        assertEquals("۲۰۲۴", TextPostprocessor.clean("٢٠٢٤"))
        assertEquals("۰۱۲۳۴۵۶۷۸۹", TextPostprocessor.clean("٠١٢٣٤٥٦٧٨٩"))
    }

    @Test
    fun `arabic yeh and kaf are normalized to persian letters`() {
        assertEquals("کتاب یار", TextPostprocessor.clean("كتاب يار"))
    }

    @Test
    fun `tatweel characters are removed`() {
        assertEquals("سلام", TextPostprocessor.clean("سلــام"))
    }

    @Test
    fun `repeated spaces are collapsed inside lines`() {
        assertEquals("سلام دنیا", TextPostprocessor.clean("سلام   \t دنیا"))
    }

    @Test
    fun `three or more newlines collapse to one blank line`() {
        assertEquals("پاراگراف اول\n\nپاراگراف دوم", TextPostprocessor.clean("پاراگراف اول\n\n\n\n\nپاراگراف دوم"))
    }

    @Test
    fun `crlf line endings are normalized`() {
        assertEquals("خط اول\nخط دوم", TextPostprocessor.clean("خط اول\r\nخط دوم"))
    }

    @Test
    fun `mixed persian english text keeps character order intact`() {
        val input = "این یک متن Persian Test 123 است"
        assertEquals(input, TextPostprocessor.clean(input))
    }

    @Test
    fun `lines are trimmed`() {
        assertEquals("خط اول\nخط دوم", TextPostprocessor.clean("  خط اول  \n   خط دوم   "))
    }
}
