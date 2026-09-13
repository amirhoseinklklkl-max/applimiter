package ir.amir.applimiter.service

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
import androidx.core.content.ContextCompat
import ir.amir.applimiter.MainActivity
import ir.amir.applimiter.R
import ir.amir.applimiter.data.InstalledAppsRepository
import ir.amir.applimiter.usage.UsageTracker
import ir.amir.applimiter.usage.asHumanDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** سرویس همیشه‌روشن: هر ۲ ثانیه وضعیت برنامه‌ی جلو را بررسی می‌کند. */
class MonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "monitor_channel"
        private const val NOTIF_ID = 1001
        private const val TICK_MS = 2000L

        @Volatile var lastForegroundPackage: String? = null

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tracker by lazy { UsageTracker(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification("در حال مراقبت از زمان استفاده"))
        scope.launch {
            while (isActive) {
                try {
                    tick()
                } catch (t: Throwable) {
                    // ادامه بده
                }
                delay(TICK_MS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun tick() {
        if (!UsageTracker.hasUsageAccess(this)) return
        val pkg = tracker.currentForegroundPackage() ?: lastForegroundPackage ?: return
        Enforcer.evaluate(this, pkg, tracker)

        val remaining = Enforcer.remainingMillis(this, pkg, tracker)
        val text = if (remaining == null) {
            "در حال مراقبت از زمان استفاده"
        } else {
            val name = InstalledAppsRepository.labelOf(this, pkg)
            "$name: ${remaining.asHumanDuration()} باقی‌مانده"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "مراقبت از زمان",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
