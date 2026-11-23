package com.blockphonespam.data.repository

import com.blockphonespam.data.local.AppDatabase
import com.blockphonespam.data.local.BlockedCallEntity
import com.blockphonespam.data.local.PrefixRuleEntity
import com.blockphonespam.domain.PhoneNumberMatcher
import kotlinx.coroutines.flow.Flow

class SpamBlockRepository(private val database: AppDatabase) {

    private val prefixDao = database.prefixRuleDao()
    private val blockedCallDao = database.blockedCallDao()

    fun observePrefixes(): Flow<List<PrefixRuleEntity>> = prefixDao.observeAll()

    fun observeBlockedCalls(): Flow<List<BlockedCallEntity>> = blockedCallDao.observeAll()

    /**
     * Khởi tạo sẵn các đầu số mặc định (025, 022, 023, 026, 028, 020) nếu bảng đang rỗng.
     * Tự động chuẩn hóa các đầu số cũ trong database nếu chưa đúng định dạng.
     */
    suspend fun ensureDefaultPrefixesSeeded() {
        if (prefixDao.count() == 0) {
            val now = System.currentTimeMillis()
            val defaults = PhoneNumberMatcher.DEFAULT_PREFIXES.mapIndexed { index, prefix ->
                PrefixRuleEntity(
                    prefix = prefix,
                    isEnabled = true,
                    isDefault = true,
                    createdAt = now + index
                )
            }
            prefixDao.insertAll(defaults)
        } else {
            val existing = prefixDao.getAll()
            for (rule in existing) {
                val normalized = PhoneNumberMatcher.normalizePrefix(rule.prefix)
                if (normalized.isNotEmpty() && normalized != rule.prefix) {
                    prefixDao.deleteByPrefix(rule.prefix)
                    prefixDao.upsert(
                        rule.copy(
                            prefix = normalized,
                            isDefault = normalized in PhoneNumberMatcher.DEFAULT_PREFIXES
                        )
                    )
                }
            }
        }
    }

    suspend fun getEnabledPrefixes(): List<String> {
        ensureDefaultPrefixesSeeded()
        return prefixDao.getEnabledPrefixes()
    }

    suspend fun togglePrefix(prefix: String, enabled: Boolean) {
        prefixDao.setEnabled(prefix, enabled)
    }

    /**
     * Thêm đầu số mới vào bộ nhớ ứng dụng. Trả về true nếu thêm thành công, false nếu không hợp lệ.
     */
    suspend fun addPrefix(rawPrefix: String): Boolean {
        val normalized = PhoneNumberMatcher.normalizePrefix(rawPrefix)
        if (normalized.length < 2 || normalized.length > 10) return false
        prefixDao.upsert(
            PrefixRuleEntity(
                prefix = normalized,
                isEnabled = true,
                isDefault = normalized in PhoneNumberMatcher.DEFAULT_PREFIXES,
                createdAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun deletePrefix(prefix: String) {
        prefixDao.deleteByPrefix(prefix)
    }

    suspend fun recordBlockedCall(phoneNumber: String, matchedPrefix: String) {
        blockedCallDao.insert(
            BlockedCallEntity(
                phoneNumber = phoneNumber,
                matchedPrefix = matchedPrefix,
                blockedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteBlockedCall(id: Long) {
        blockedCallDao.deleteById(id)
    }

    suspend fun clearBlockedCalls() {
        blockedCallDao.clearAll()
    }
}
