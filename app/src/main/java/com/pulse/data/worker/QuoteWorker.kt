package com.pulse.data.worker

import android.content.Context
import androidx.work.*
import com.pulse.core.domain.util.NotificationHelper
import java.util.concurrent.TimeUnit

class QuoteWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val quotes = listOf(
        // Motivational
        "The only way to do great work is to love what you do. — Steve Jobs",
        "Success is not final, failure is not fatal: it is the courage to continue that counts. — Winston Churchill",
        "It always seems impossible until it's done. — Nelson Mandela",
        "Don't count the days, make the days count. — Muhammad Ali",
        "Believe you can and you're halfway there. — Theodore Roosevelt",
        "Wait for nothing. Your life is happening right now.",
        "Your future is created by what you do today, not tomorrow.",
        "Discipline is choosing between what you want now and what you want most.",
        
        // Pushing / Productivity
        "Stop wishing, start working. Get back to Pulse!",
        "Consistency is key. Have you checked your lectures today?",
        "One step at a time, but keep moving. One lecture at a time.",
        "The distance between dreams and reality is called action. Take one now.",
        "Your goals don't care how you feel. Show up anyway.",
        "Focus on being productive instead of busy.",
        "Work hard in silence, let your success be your noise.",
        "If you want results, you have to do the work. No shortcuts.",
        "Just 10 minutes. Open a lecture and start. The rest will follow."
    )

    override suspend fun doWork(): Result {
        val randomQuote = quotes.random()
        NotificationHelper.showQuoteNotification(applicationContext, randomQuote)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "PeriodicQuoteWorker"

        fun enqueuePeriodicQuote(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Works offline
                .build()

            val request = PeriodicWorkRequestBuilder<QuoteWorker>(
                6, TimeUnit.HOURS, // Every 6 hours
                1, TimeUnit.HOURS     // Flexible interval: can run within last hour of window
            )
            .setConstraints(constraints)
            .addTag(WORK_NAME)
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing to avoid resetting the 6h timer
                request
            )
        }
    }
}
