package com.example.security

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks message IDs and nonces with timestamp expiration to prevent replay attacks.
 * Rejects stale or duplicate messages.
 */
class NonceReplayManager(
    private val maxAgeMillis: Long = 300_000 // 5 minutes message window
) {
    companion object {
        private const val TAG = "KidLock_Replay"
    }

    // Map of nonce/messageId to expiration timestamp
    private val seenNonces = ConcurrentHashMap<String, Long>()

    /**
     * Validates whether a message is fresh (not stale) and not a replay.
     * @return true if valid, false if replayed or expired.
     */
    fun validateAndRecordNonce(nonce: String, timestamp: Long): Boolean {
        if (nonce.isBlank()) return false
        val now = System.currentTimeMillis()

        // 1. Check if message timestamp is outside valid window (older than maxAge or in distant future)
        if (Math.abs(now - timestamp) > maxAgeMillis) {
            Log.w(TAG, "Rejected message with timestamp outside validity window: diff=${now - timestamp}ms")
            return false
        }

        // 2. Clean up expired entries periodically
        cleanupExpired(now)

        // 3. Check if nonce has already been used
        if (seenNonces.containsKey(nonce)) {
            Log.w(TAG, "Replay detected! Nonce already consumed: $nonce")
            return false
        }

        // 4. Record the nonce with its expiration time
        seenNonces[nonce] = now + maxAgeMillis
        return true
    }

    fun getTrackedCount(): Int = seenNonces.size

    fun clearAll() {
        seenNonces.clear()
    }

    private fun cleanupExpired(now: Long) {
        val iterator = seenNonces.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value < now) {
                iterator.remove()
            }
        }
    }
}
