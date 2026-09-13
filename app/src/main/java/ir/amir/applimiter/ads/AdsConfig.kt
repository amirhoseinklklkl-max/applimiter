package ir.amir.applimiter.ads

import ir.amir.applimiter.BuildConfig

/**
 * تنظیمات تبلیغات تپسل.
 * App ID از طریق manifest placeholder (TapsellMediationAppKey) به SDK داده می‌شود
 * و زون‌ها از gradle.properties خوانده می‌شوند تا داخل کد hardcode نشوند.
 */
object AdsConfig {

    /** زون تبلیغ آنی (Interstitial) که بعد از ورود کاربر به برنامه نمایش داده می‌شود. */
    val INTERSTITIAL_ZONE: String = BuildConfig.TAPSELL_ZONE_INTERSTITIAL.trim()

    /** زون بنر پایین لیست برنامه‌ها. خالی باشد، بنری نمایش داده نمی‌شود. */
    val BANNER_ZONE: String = BuildConfig.TAPSELL_ZONE_BANNER.trim()

    /** حداقل فاصله‌ی بین دو تبلیغ آنی؛ جلوی اذیت شدن کاربر را می‌گیرد. */
    const val INTERSTITIAL_MIN_INTERVAL_MS = 3 * 60_000L

    /** کمی تأخیر بعد از باز شدن برنامه تا لیست اپ‌ها اول لود شود و تبلیغ روی صفحه‌ی سفید نیفتد. */
    const val INTERSTITIAL_DELAY_MS = 900L

    private val placeholders = setOf(
        "",
        "YOUR_TAPSELL_APP_ID",
        "YOUR_INTERSTITIAL_ZONE_ID",
        "YOUR_BANNER_ZONE_ID"
    )

    fun isConfigured(zone: String): Boolean = zone !in placeholders
}
