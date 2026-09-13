package ir.amir.applimiter.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import ir.amir.applimiter.usage.UsageTracker

/** برنامه‌ی جلو را لحظه‌ای تشخیص می‌دهد و در صورت پایان زمان، کاربر را بیرون می‌اندازد. */
class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile private var instance: AppLockAccessibilityService? = null

        fun goHome(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, AppLockAccessibilityService::class.java)
                .flattenToString()
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }
    }

    private val tracker by lazy { UsageTracker(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        MonitorService.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        MonitorService.lastForegroundPackage = pkg
        try {
            Enforcer.evaluate(this, pkg, tracker)
        } catch (t: Throwable) {
            // نادیده بگیر تا سرویس نخوابد
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
