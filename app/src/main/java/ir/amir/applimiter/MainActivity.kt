package ir.amir.applimiter

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.applimiter.ads.AdsConsentDialog
import ir.amir.applimiter.ads.AdsConfig
import ir.amir.applimiter.ads.AdsManager
import ir.amir.applimiter.ads.ConsentStore
import ir.amir.applimiter.service.AppLockAccessibilityService
import ir.amir.applimiter.service.MonitorService
import ir.amir.applimiter.ui.AppLimiterTheme
import ir.amir.applimiter.ui.AppListScreen
import ir.amir.applimiter.usage.UsageTracker
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var refresh by mutableIntStateOf(0)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // آماده‌سازی تبلیغات: رضایت ذخیره‌شده را می‌فرستد و تبلیغ ورودی را از قبل لود می‌کند
        AdsManager.warmUp(this)

        setContent {
            AppLimiterTheme {
                MainScreen(refreshKey = refresh)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh++
        if (UsageTracker.hasUsageAccess(this)) {
            MonitorService.start(this)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(refreshKey: Int) {
    val context = LocalContext.current
    val activity = context as? Activity
    val hasUsage = remember(refreshKey) { UsageTracker.hasUsageAccess(context) }
    val hasAccessibility = remember(refreshKey) { AppLockAccessibilityService.isEnabled(context) }
    val hasOverlay = remember(refreshKey) { Settings.canDrawOverlays(context) }

    var needsConsent by remember { mutableStateOf(!ConsentStore.hasBeenAsked(context)) }

    // تبلیغ آنی بعد از ورود کاربر؛ با کمی تأخیر تا لیست اپ‌ها اول بیاید
    LaunchedEffect(needsConsent) {
        if (needsConsent || activity == null) return@LaunchedEffect
        delay(AdsConfig.INTERSTITIAL_DELAY_MS)
        AdsManager.showInterstitialOnEntry(activity)
    }

    if (needsConsent && activity != null) {
        AdsConsentDialog { granted ->
            ConsentStore.save(activity, granted)
            AdsManager.setUserConsent(activity, granted)
            AdsManager.preloadInterstitial()
            needsConsent = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("مدیر زمان برنامه‌ها") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasUsage || !hasAccessibility || !hasOverlay) {
                Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "برای فعال شدن محدودیت‌ها این دسترسی‌ها لازم است:",
                            fontWeight = FontWeight.Bold
                        )
                        if (!hasUsage) {
                            PermissionRow("دسترسی آمار استفاده") {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        }
                        if (!hasAccessibility) {
                            PermissionRow("سرویس دسترس‌پذیری (بستن فوری برنامه)") {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        }
                        if (!hasOverlay) {
                            PermissionRow("نمایش روی برنامه‌های دیگر") {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:" + context.packageName)
                                    )
                                )
                            }
                        }
                        Text(
                            "بعد از فعال‌سازی به برنامه برگرد؛ وضعیت خودکار به‌روز می‌شود.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            AppListScreen(refreshKey = refreshKey)
        }
    }
}

@Composable
private fun PermissionRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, modifier = Modifier.padding(end = 8.dp))
        OutlinedButton(onClick = onClick) { Text("فعال کن") }
    }
}
