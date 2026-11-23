package com.blockphonespam.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberMatcherTest {

    @Test
    fun defaultPrefixesContainAllRequestedCodes() {
        assertEquals(
            listOf("025", "022", "023", "026", "028", "020"),
            PhoneNumberMatcher.DEFAULT_PREFIXES
        )
    }

    @Test
    fun normalizeVietnameseFormats() {
        assertEquals("02812345678", PhoneNumberMatcher.normalizePhoneNumber("+84 28 1234 5678"))
        assertEquals("0259876543", PhoneNumberMatcher.normalizePhoneNumber("+84259876543"))
        assertEquals("0223456789", PhoneNumberMatcher.normalizePhoneNumber("0084223456789"))
        assertEquals("02311223344", PhoneNumberMatcher.normalizePhoneNumber("842311223344"))
        assertEquals("0261234567", PhoneNumberMatcher.normalizePhoneNumber("(026) 123-4567"))
        assertEquals("0203344556", PhoneNumberMatcher.normalizePhoneNumber("0203344556"))
        // Định dạng SIP/IMS từ mạng VoLTE / SkyFi
        assertEquals("032123123123", PhoneNumberMatcher.normalizePhoneNumber("sip:+8432123123123@ims.skyfi.vn"))
        assertEquals("032123123123", PhoneNumberMatcher.normalizePhoneNumber("tel:+8432123123123"))
    }

    @Test
    fun normalizePrefixHandlesVariousUserInputs() {
        // Người dùng nhập thiếu '0' ở đầu
        assertEquals("024", PhoneNumberMatcher.normalizePrefix("24"))
        assertEquals("028", PhoneNumberMatcher.normalizePrefix("28"))
        assertEquals("032", PhoneNumberMatcher.normalizePrefix("32"))
        assertEquals("090", PhoneNumberMatcher.normalizePrefix("90"))

        // Người dùng nhập chuẩn dạng '0'
        assertEquals("024", PhoneNumberMatcher.normalizePrefix("024"))
        assertEquals("032", PhoneNumberMatcher.normalizePrefix("032"))

        // Người dùng nhập dạng quốc tế (+84, 84, 0084)
        assertEquals("024", PhoneNumberMatcher.normalizePrefix("+8424"))
        assertEquals("024", PhoneNumberMatcher.normalizePrefix("+84 24"))
        assertEquals("024", PhoneNumberMatcher.normalizePrefix("8424"))
        assertEquals("024", PhoneNumberMatcher.normalizePrefix("008424"))
        assertEquals("032", PhoneNumberMatcher.normalizePrefix("+8432"))

        // Đầu số dịch vụ đặc biệt (1800, 1900...)
        assertEquals("1900", PhoneNumberMatcher.normalizePrefix("1900"))
        assertEquals("1800", PhoneNumberMatcher.normalizePrefix("1800"))
    }

    @Test
    fun matchesEnabledDefaultPrefixes() {
        val enabled = PhoneNumberMatcher.DEFAULT_PREFIXES
        assertEquals("025", PhoneNumberMatcher.findMatchingPrefix("0251234567", enabled))
        assertEquals("022", PhoneNumberMatcher.findMatchingPrefix("+84229998888", enabled))
        assertEquals("023", PhoneNumberMatcher.findMatchingPrefix("84236123456", enabled))
        assertEquals("026", PhoneNumberMatcher.findMatchingPrefix("02633889900", enabled))
        assertEquals("028", PhoneNumberMatcher.findMatchingPrefix("+84 28 7777 8888", enabled))
        assertEquals("020", PhoneNumberMatcher.findMatchingPrefix("02043881122", enabled))
    }

    @Test
    fun matchesNewlyAddedPrefixesRegardlessOfInputFormat() {
        val customPrefixes = listOf("032", "24", "1900")

        // Cuộc gọi đến 032... từ SIM thường hoặc qua mạng VoLTE/SkyFi
        assertEquals("032", PhoneNumberMatcher.findMatchingPrefix("032123123123", customPrefixes))
        assertEquals("032", PhoneNumberMatcher.findMatchingPrefix("+8432123123123", customPrefixes))
        assertEquals("032", PhoneNumberMatcher.findMatchingPrefix("8432123123123", customPrefixes))
        assertEquals("032", PhoneNumberMatcher.findMatchingPrefix("sip:+8432123123123@ims.mnc002.mcc452.3gppnetwork.org", customPrefixes))

        // Đầu số 24 (tự hiểu 024)
        assertEquals("024", PhoneNumberMatcher.findMatchingPrefix("02473001234", customPrefixes))

        // Tổng đài 1900
        assertEquals("1900", PhoneNumberMatcher.findMatchingPrefix("19001560", customPrefixes))
    }

    @Test
    fun doesNotMatchNonSpamOrDisabledPrefixes() {
        val enabled = listOf("025", "028", "032")
        assertNull(PhoneNumberMatcher.findMatchingPrefix("0901234567", enabled))
        assertNull(PhoneNumberMatcher.findMatchingPrefix("0221234567", enabled))
        assertNull(PhoneNumberMatcher.findMatchingPrefix("0331234567", enabled))
        assertNull(PhoneNumberMatcher.findMatchingPrefix("", enabled))
        assertNull(PhoneNumberMatcher.findMatchingPrefix(null, enabled))
    }

    @Test
    fun prefersLongestMatchingPrefix() {
        val enabled = listOf("028", "02873")
        assertEquals("02873", PhoneNumberMatcher.findMatchingPrefix("02873001234", enabled))
        assertEquals("028", PhoneNumberMatcher.findMatchingPrefix("02838112233", enabled))
    }
}
