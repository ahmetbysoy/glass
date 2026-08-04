package com.glasspro.tracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.IBinder

class AlertService : Service() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("glass_alert", "Likidasyon Uyarıları", NotificationManager.IMPORTANCE_HIGH)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }
    override fun onBind(intent: android.content.Intent?): IBinder? = null
    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = Notification.Builder(this, "glass_alert")
            .setContentTitle("GlassPro")
            .setContentText("Yeni likidasyon tespit edildi!")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
        startForeground(1, notification)
        return START_STICKY
    }
}
