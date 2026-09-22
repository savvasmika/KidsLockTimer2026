package com.example.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Provides brute-force protection and rate-limiting for temporary unlock codes & PIN entries.
 * After [maxAttempts] failed tries, triggers a cooldown period during which entry is blocked.
 */
class BruteForceProtector(
    private val maxAttempts: Int = 5,
    private val lockoutDurationSeconds: Int = 30
) {
    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _isLockedOut = MutableStateFlow(false)
    val isLockedOut: StateFlow<Boolean> = _isLockedOut.asStateFlow()

    private val _remainingLockoutSeconds = MutableStateFlow(0)
    val remainingLockoutSeconds: StateFlow<Int> = _remainingLockoutSeconds.asStateFlow()

    private var countdownJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Checks if user is allowed to attempt code/PIN entry.
     */
    fun canAttempt(): Boolean = !_isLockedOut.value

    /**
     * Records a failed attempt. If threshold exceeded, initiates lockout cooldown.
     */
    fun recordFailedAttempt() {
        val nextAttempts = _failedAttempts.value + 1
        _failedAttempts.value = nextAttempts

        if (nextAttempts >= maxAttempts) {
            triggerLockout()
        }
    }

    /**
     * Records a successful attempt, resetting failure counter.
     */
    fun recordSuccess() {
        _failedAttempts.value = 0
        _isLockedOut.value = false
        _remainingLockoutSeconds.value = 0
        countdownJob?.cancel()
    }

    private fun triggerLockout() {
        _isLockedOut.value = true
        _remainingLockoutSeconds.value = lockoutDurationSeconds

        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (sec in lockoutDurationSeconds downTo 1) {
                _remainingLockoutSeconds.value = sec
                delay(1000L)
            }
            _remainingLockoutSeconds.value = 0
            _isLockedOut.value = false
            _failedAttempts.value = 0
        }
    }

    fun reset() {
        countdownJob?.cancel()
        _failedAttempts.value = 0
        _isLockedOut.value = false
        _remainingLockoutSeconds.value = 0
    }
}
