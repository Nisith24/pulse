package com.pulse.core.domain.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspends the coroutine until the [Call] is completed and returns the [Response].
 */
suspend fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }
        })

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ex: Throwable) {
                // Ignore cancellation exceptions
            }
        }
    }
}

/**
 * Executes the [Request] with a non-blocking retry mechanism.
 * Retries up to 2 times (3 attempts total) on [IOException] or specific HTTP error codes (502, 503, 504).
 */
suspend fun OkHttpClient.executeWithRetry(request: Request): Response {
    var lastException: IOException? = null
    for (attempt in 0..2) {
        try {
            val response = newCall(request).await()
            // Retry on server errors (502, 503, 504) — Google Drive occasionally returns these
            if (response.isSuccessful || response.code !in listOf(502, 503, 504)) {
                return response
            }
            response.close()
        } catch (e: IOException) {
            lastException = e
        }

        if (attempt < 2) {
            // Non-blocking delay for exponential-like backoff (1000ms, 2000ms)
            delay(1000L * (attempt + 1))
        }
    }
    throw lastException ?: IOException("Retry exhausted for ${request.url}")
}
