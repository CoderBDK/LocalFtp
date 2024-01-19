package com.coderbdk.localftp.ui

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.coderbdk.localftp.R
import com.coderbdk.localftp.di.ftp.LocalFtpServer


/**
 * Created by MD. ABDULLAH on Mon, Jan 15, 2024.
 */
class LocalFtpService : Service() {

    private val CHANNEL_ID = "ftp_local_server_notification_running"
    private val CHANNEL_NAME = "ftp_local_server_"
    private val CHANNEL_DESCRIPTION = "Local Ftp Server is running"
    private lateinit var localFtpServer: LocalFtpServer
    private val prefs by lazy {
        getSharedPreferences("local_ftp_server", MODE_PRIVATE)
    }
    override fun onCreate() {
        super.onCreate()
        val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        localFtpServer = setupServer(prefs.getInt("port", 8088), wm.connectionInfo.ipAddress)
        notifyNotification("Local Ftp Server", "Running...", true)
       // Toast.makeText(applicationContext, "${wm.connectionInfo.ipAddress}", Toast.LENGTH_SHORT).show()
    }

    private fun createNotification(title: String, description: String, ongoing: Boolean): Notification {
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_local_activity_24)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    applicationContext.resources,
                    R.mipmap.ic_launcher
                )
            )
            .setContentTitle(title)
            .setContentText(description)
            .setOngoing(ongoing)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
    }

    private fun notifyNotification(title: String, notificationDescription: String, ongoing: Boolean) {
        val notification = createNotification(title, notificationDescription, ongoing)
        val nManager = NotificationManagerCompat.from(applicationContext)
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            nManager.createNotificationChannel(channel)
            nManager.notify(1011, notification)
        } else {
            nManager.notify(1011, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {

        private var _localFtpServer: LocalFtpServer? = null

        private fun setupServer(port: Int, ip: Int): LocalFtpServer {
            if (_localFtpServer == null) {
                _localFtpServer = LocalFtpServer(
                    port,
                    ip
                )
                _localFtpServer!!.start()
                return _localFtpServer!!
            }
            return _localFtpServer!!
        }
        fun getServer(): LocalFtpServer? {
            return _localFtpServer
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notifyNotification("Local Ftp Server", "Stopped!", false)
        localFtpServer.stop()
        _localFtpServer = null
    }
}