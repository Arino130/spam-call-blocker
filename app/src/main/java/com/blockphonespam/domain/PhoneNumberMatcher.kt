package com.blockphonespam.domain

object PhoneNumberMatcher {

    val DEFAULT_PREFIXES = listOf("025", "022", "023", "026", "028", "020")

    /**
     * Bóc tách số điện thoại từ bất kỳ định dạng nào (tel:..., sip:..., url-encoded...).
     * Loại bỏ các phần mở rộng của SIP như "@ims.skyfi.vn" hoặc các tham số URI.
     */
    fun extractPhoneNumber(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""
        var cleaned = rawNumber.trim()
        if (cleaned.startsWith("tel:", ignoreCase = true)) {
            cleaned = cleaned.substring(4)
        } else if (cleaned.startsWith("sip:", ignoreCase = true)) {
            cleaned = cleaned.substring(4)
        }
        val atIndex = cleaned.indexOf('@')
        if (atIndex != -1) {
            cleaned = cleaned.substring(0, atIndex)
        }
        val semiIndex = cleaned.indexOf(';')
        if (semiIndex != -1) {
            cleaned = cleaned.substring(0, semiIndex)
        }
        return cleaned
    }

    /**
     * Chuẩn hóa số điện thoại Việt Nam về dạng bắt đầu bằng '0' và chỉ gồm chữ số.
     * Ví dụ:
     * - "+84 28 1234 5678" -> "02812345678"
     * - "008425123456" -> "025123456"
     * - "84231234567" -> "0231234567"
     * - "sip:+8432123123123@ims" -> "032123123123"
     * - "(022) 345-6789" -> "0223456789"
     */
    fun normalizePhoneNumber(rawNumber: String?): String {
        val extracted = extractPhoneNumber(rawNumber)
        if (extracted.isBlank()) return ""
        val hasLeadingPlus = extracted.startsWith("+")
        val digitsOnly = extracted.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return ""

        return when {
            hasLeadingPlus && digitsOnly.startsWith("84") -> "0" + digitsOnly.removePrefix("84")
            digitsOnly.startsWith("0084") -> "0" + digitsOnly.removePrefix("0084")
            digitsOnly.startsWith("84") && digitsOnly.length >= 9 -> "0" + digitsOnly.removePrefix("84")
            else -> digitsOnly
        }
    }

    /**
     * Chuẩn hóa đầu số người dùng nhập:
     * - Chuyển các định dạng quốc tế (+84, 0084, 84...) về tiền tố '0' (VD: "+8424", "8424" -> "024").
     * - Nếu người dùng nhập thiếu '0' ở đầu (VD: "24", "28", "90"), tự động bù '0' -> "024", "028", "090".
     * - Giữ nguyên các đầu số dịch vụ đặc biệt bắt đầu bằng '1' (VD: "1900", "1800").
     */
    fun normalizePrefix(rawPrefix: String): String {
        val extracted = extractPhoneNumber(rawPrefix)
        if (extracted.isBlank()) return ""
        val hasLeadingPlus = extracted.startsWith("+")
        val digitsOnly = extracted.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return ""

        val withoutCountryCode = when {
            hasLeadingPlus && digitsOnly.startsWith("84") -> "0" + digitsOnly.removePrefix("84")
            digitsOnly.startsWith("0084") -> "0" + digitsOnly.removePrefix("0084")
            digitsOnly.startsWith("84") && digitsOnly.length >= 3 -> "0" + digitsOnly.removePrefix("84")
            else -> digitsOnly
        }

        return if (!withoutCountryCode.startsWith("0") && !withoutCountryCode.startsWith("1")) {
            "0$withoutCountryCode"
        } else {
            withoutCountryCode
        }
    }

    /**
     * Kiểm tra số điện thoại có khớp với bất kỳ đầu số đang bật nào hay không.
     * So sánh đa chiều (cả số bắt đầu bằng 0... lẫn đầu số tương ứng dạng 84... hoặc số thuần).
     * Trả về đầu số khớp (ưu tiên đầu số dài nhất nếu trùng tiền tố) hoặc null nếu không khớp.
     */
    fun findMatchingPrefix(rawNumber: String?, enabledPrefixes: Collection<String>): String? {
        val normalizedNumber = normalizePhoneNumber(rawNumber)
        val extracted = extractPhoneNumber(rawNumber)
        val rawDigitsOnly = extracted.filter { it.isDigit() }

        if (normalizedNumber.isEmpty() && rawDigitsOnly.isEmpty()) return null

        return enabledPrefixes
            .asSequence()
            .map { normalizePrefix(it) }
            .filter { it.isNotEmpty() }
            .sortedByDescending { it.length }
            .firstOrNull { prefix ->
                // Khớp số đã chuẩn hóa dạng 0...
                (normalizedNumber.isNotEmpty() && normalizedNumber.startsWith(prefix)) ||
                // Khớp số thuần từ mạng viễn thông
                (rawDigitsOnly.isNotEmpty() && rawDigitsOnly.startsWith(prefix)) ||
                // Khớp trường hợp nhà mạng gửi định dạng quốc tế 84... nhưng tiền tố lưu là 0...
                (prefix.startsWith("0") && rawDigitsOnly.startsWith("84" + prefix.removePrefix("0")))
            }
    }
}
