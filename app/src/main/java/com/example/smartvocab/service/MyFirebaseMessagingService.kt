package com.example.smartvocab.service

import android.provider.Settings
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "FCM token mới nhận được: $token")
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCMService", "Nhận được tin nhắn push từ FCM: ${message.notification?.body}")
        
        message.notification?.let {
            showPushNotification(it.title ?: "SmartVocab Notification", it.body ?: "")
        }
    }

    private fun sendTokenToServer(token: String) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        
        val context = applicationContext
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        val docId = "${userId}_$deviceId"
        
        val deviceData = hashMapOf(
            "id" to docId,
            "userId" to userId,
            "deviceId" to deviceId,
            "token" to token,
            "updatedAt" to Timestamp.now()
        )
        
        FirebaseFirestore.getInstance()
            .collection("user_devices")
            .document(docId)
            .set(deviceData)
            .addOnSuccessListener {
                Log.d("FCMService", "Đã lưu token FCM thành công lên Firestore: $docId")
            }
            .addOnFailureListener { e ->
                Log.e("FCMService", "Lỗi lưu token FCM lên Firestore", e)
            }
    }

    private fun showPushNotification(title: String, body: String) {
        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "push_notifications"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Push Notifications",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Kênh nhận thông báo push từ hệ thống SmartVocab"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = android.content.Intent(this, com.example.smartvocab.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
