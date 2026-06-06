package com.example.smartvocab.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.smartvocab.MainActivity
import com.example.smartvocab.data.model.LearningSettings
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Extension helper to await Task results in Coroutines
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("Task failed"))
        }
    }
}

class DailyReminderWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.success()
        
        val db = FirebaseFirestore.getInstance()
        val query = db.collection("user_settings")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            
        val settings = if (!query.isEmpty) {
            query.documents.first().toObject(LearningSettings::class.java)
        } else {
            LearningSettings(userId = userId)
        }
        
        if (settings != null && settings.dailyReminderEnabled) {
            sendNotification(
                context,
                "Đã đến giờ học rồi! 📚",
                "Hãy dành 5 phút mở SmartVocab để tiếp tục mở rộng vốn từ của bạn hôm nay nhé."
            )
        }
        
        // Reschedule daily reminder for tomorrow at target time
        ReminderManager.scheduleDailyReminder(context, settings?.reminderTime ?: "20:00", settings?.dailyReminderEnabled ?: true)
        
        return Result.success()
    }
}

class DueReviewReminderWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.success()
        
        val db = FirebaseFirestore.getInstance()
        val query = db.collection("user_settings")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            
        val settings = if (!query.isEmpty) {
            query.documents.first().toObject(LearningSettings::class.java)
        } else {
            LearningSettings(userId = userId)
        }
        
        if (settings == null || !settings.dueReviewReminderEnabled) {
            return Result.success()
        }
        
        val now = Timestamp.now()
        val progressQuery = db.collection("learning_progress")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "REVIEW")
            .get()
            .await()
            
        val dueCount = progressQuery.documents.count { doc ->
            val nextReview = doc.getTimestamp("nextReviewDate")
            nextReview != null && nextReview.seconds <= now.seconds
        }
        
        if (dueCount > 0) {
            sendNotification(
                context,
                "Có từ vựng cần ôn tập! ⏱️",
                "Bạn có $dueCount từ vựng đã đến hạn ôn tập. Hãy ôn ngay để không bị quên nhé."
            )
        }
        
        return Result.success()
    }
}

private fun sendNotification(context: Context, title: String, message: String) {
    val channelId = "smartvocab_reminders"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "SmartVocab Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Kênh thông báo nhắc nhở học tập của SmartVocab"
        }
        notificationManager.createNotificationChannel(channel)
    }
    
    val intent = Intent(context, MainActivity::class.java).apply {
        action = "OPEN_REVIEW"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        
    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
}

object ReminderManager {
    private const val DAILY_WORK_NAME = "daily_reminder_work"
    private const val DUE_WORK_NAME = "due_review_reminder_work"
    
    fun scheduleDailyReminder(context: Context, timeStr: String, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(DAILY_WORK_NAME)
        
        if (!enabled) return
        
        val delay = calculateDelay(timeStr)
        val workRequest = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(DAILY_WORK_NAME)
            .build()
            
        workManager.enqueueUniqueWork(
            DAILY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    fun scheduleDueReviewReminder(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(DUE_WORK_NAME)
        
        if (!enabled) return
        
        val workRequest = PeriodicWorkRequestBuilder<DueReviewReminderWorker>(2, TimeUnit.HOURS)
            .addTag(DUE_WORK_NAME)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            DUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    private fun calculateDelay(timeStr: String): Long {
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DATE, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
