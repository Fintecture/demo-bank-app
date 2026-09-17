package com.fintecture.demobank

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Owns the "complete but do not return me" request.
 *
 * In a ViewModel rather than the activity so a rotation mid-request does not lose the result or
 * re-enable the buttons behind a request that is still running.
 */
class StrandViewModel : ViewModel() {

    sealed interface Result {
        /** The callback ran and redirected onwards, which is what a completed step 2 does. */
        data class CallbackDriven(val state: String) : Result

        /** Reached the server but it did not redirect, so the outcome was not applied. */
        data class NotApplied(val state: String, val httpStatus: Int) : Result

        data class Failed(val state: String) : Result
    }

    private val _result = MutableLiveData<Result?>()
    val result: LiveData<Result?> get() = _result

    private var inFlightFor: String? = null

    val isRunning: Boolean get() = inFlightFor != null

    fun strand(session: DemoBankSession, outcome: Outcome) {
        if (isRunning) return
        inFlightFor = session.state

        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) { driveCallback(session.callbackUrl(outcome)) }

            // Tagged with the state it belongs to, so a result from a previous handoff cannot be
            // shown against a session that arrived in the meantime.
            _result.value = when {
                status == null -> Result.Failed(session.state)
                status in 300..399 -> Result.CallbackDriven(session.state)
                else -> Result.NotApplied(session.state, status)
            }
            inFlightFor = null
        }
    }

    fun clear() {
        _result.value = null
    }

    /**
     * Drives step 2 without following the redirect: following it would land back in the merchant
     * flow, which is the opposite of the case being reproduced.
     *
     * Only the status is returned. A 2xx here is not success: it means a page came back rather than
     * a redirect, which is what an error or a re-rendered consent screen looks like.
     */
    private fun driveCallback(url: String): Int? = runCatching {
        (URL(url).openConnection() as HttpURLConnection).run {
            instanceFollowRedirects = false
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            try {
                responseCode
            } finally {
                disconnect()
            }
        }
    }.getOrNull()
}
