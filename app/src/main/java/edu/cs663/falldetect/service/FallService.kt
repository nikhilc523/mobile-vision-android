package edu.cs663.falldetect.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import edu.cs663.falldetect.R
import edu.cs663.falldetect.ui.MainActivity
import edu.cs663.falldetect.util.Log

/**
 * Foreground service for fall detection monitoring.
 * Runs CameraX pipeline and maintains persistent notification.
 */
class FallService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "fall_detection_service"
        
        const val ACTION_START = "edu.cs663.falldetect.action.START"
        const val ACTION_STOP = "edu.cs663.falldetect.action.STOP"
        
        fun startService(context: Context) {
            val intent = Intent(context, FallService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, FallService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
    
    private var isRunning = false
    
    override fun onCreate() {
        super.onCreate()
        Log.i("FallService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isRunning) {
                    startForegroundService()
                }
            }
            ACTION_STOP -> {
                stopForegroundService()
            }
        }
        
        return START_STICKY
    }
    
    private fun startForegroundService() {
        Log.i("Starting fall detection service")
        isRunning = true
        
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // TODO: Initialize CameraX pipeline
        // TODO: Initialize LSTM interpreter
        // TODO: Initialize feature buffer
        // TODO: Start pose analysis
    }
    
    private fun stopForegroundService() {
        Log.i("Stopping fall detection service")
        isRunning = false
        
        // TODO: Release CameraX resources
        // TODO: Close LSTM interpreter
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i("FallService destroyed")
        isRunning = false
    }
}

