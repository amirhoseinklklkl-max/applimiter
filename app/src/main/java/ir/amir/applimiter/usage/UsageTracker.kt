package ir.amir.applimiter.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import ir.amir.applimiter.data.LimitStore

/** میزان استفاده‌ی امروز هر برنامه را از روی رویدادهای سیستم حساب می‌کند (هر شبانه‌روز صفر می‌شود). */
class UsageTracker(private val context: Context) {

    private val usm by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    companion object {
        private const val EVENT_FOREGROUND = 1 // MOVE_TO_FOREGROUND / ACTIVITY_RESUMED
        private const val EVENT_BACKGROUND = 2 // MOVE_TO_BACKGROUND / ACTIVITY_PAUSED

        @Volatile private var cache: Map<String, Long> = emptyMap()
        @Volatile private var cacheAt = 0L
        private const val CACHE_MS = 1500L

        fun hasUsageAccess(context: Context): Boolean {
            val aom = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val op = "android:get_usage_stats"
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                aom.unsafeCheckOpNoThrow(op, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                aom.checkOpNoThrow(op, Process.myUid(), context.packageName)
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }

    fun todayUsageMillis(force: Boolean = false): Map<String, Long> {
        val now = System.currentTimeMillis()
        if (!force && now - cacheAt < CACHE_MS) return cache

        val begin = LimitStore.startOfToday()
        val result = HashMap<String, Long>()
        val openedAt = HashMap<String, Long>()

        try {
            val events = usm.queryEvents(begin, now)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                when (event.eventType) {
                    EVENT_FOREGROUND -> openedAt[pkg] = event.timeStamp
                    EVENT_BACKGROUND -> {
                        val start = openedAt.remove(pkg) ?: continue
                        result[pkg] = (result[pkg] ?: 0L) + (event.timeStamp - start)
                    }
                }
            }
            // جلساتی که همین حالا باز هستند
            for ((pkg, start) in openedAt) {
                result[pkg] = (result[pkg] ?: 0L) + (now - start)
            }
        } catch (t: Throwable) {
            return cache
        }

        cache = result
        cacheAt = now
        return result
    }

    fun todayUsageFor(pkg: String): Long = todayUsageMillis()[pkg] ?: 0L

    /** آخرین برنامه‌ای که به فورگراند آمده (برای وقتی سرویس دسترس‌پذیری خاموش است). */
    fun currentForegroundPackage(): String? {
        val now = System.currentTimeMillis()
        return try {
            val events = usm.queryEvents(now - 10_000, now)
            val event = UsageEvents.Event()
            var last: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == EVENT_FOREGROUND) last = event.packageName
            }
            last
        } catch (t: Throwable) {
            null
        }
    }
}

fun Long.asHumanDuration(): String {
    val totalMinutes = this / 60_000
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h > 0 && m > 0 -> "$h ساعت و $m دقیقه"
        h > 0 -> "$h ساعت"
        else -> "$m دقیقه"
    }
}

fun Int.minutesAsHuman(): String {
    val h = this / 60
    val m = this % 60
    return when {
        this == 0 -> "مسدود"
        h > 0 && m > 0 -> "$h ساعت و $m دقیقه"
        h > 0 -> "$h ساعت"
        else -> "$m دقیقه"
    }
}
