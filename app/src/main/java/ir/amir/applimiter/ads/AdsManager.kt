package ir.amir.applimiter.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import ir.tapsell.mediation.Tapsell
import ir.tapsell.mediation.ad.AdStateListener
import ir.tapsell.mediation.ad.request.RequestResultListener
import ir.tapsell.mediation.ad.show.AdShowCompletionState

/**
 * لایه‌ی واسط با تپسل مدیشن.
 *
 * SDK تپسل خودش با ContentProvider هنگام بالا آمدن برنامه initialize می‌شود، پس اینجا فقط
 * تبلیغ را از قبل لود می‌کنیم و در زمان مناسب نشان می‌دهیم. اگر کلیدها ست نشده باشند،
 * همه‌ی متدها بی‌صدا کاری نمی‌کنند و برنامه بدون تبلیغ کار می‌کند.
 */
object AdsManager {

    private const val TAG = "AdsManager"

    @Volatile private var interstitialAdId: String? = null
    @Volatile private var requestInFlight = false
    @Volatile private var lastShownAt = 0L
    @Volatile private var showing = false
    @Volatile private var listenerRegistered = false

    private val interstitialEnabled: Boolean
        get() = AdsConfig.isConfigured(AdsConfig.INTERSTITIAL_ZONE)

    val bannerEnabled: Boolean
        get() = AdsConfig.isConfigured(AdsConfig.BANNER_ZONE)

    /** در ورود به برنامه صدا زده می‌شود: رضایت کاربر را می‌فرستد و تبلیغ بعدی را از قبل می‌گیرد. */
    fun warmUp(activity: Activity) {
        applyStoredConsent(activity)

        if (!listenerRegistered) {
            listenerRegistered = true
            runCatching {
                Tapsell.setInitializationListener {
                    preloadInterstitial()
                }
            }
        }
        preloadInterstitial()
    }

    fun applyStoredConsent(activity: Activity) {
        if (!ConsentStore.hasBeenAsked(activity)) return
        setUserConsent(activity, ConsentStore.isGranted(activity))
    }

    /** رضایت GDPR کاربر را به SDK می‌دهد. Activity لازم است (ادموب و Wortise بدون آن ارور می‌دهند). */
    fun setUserConsent(activity: Activity, granted: Boolean) {
        runCatching { Tapsell.setUserConsent(activity, granted) }
            .onFailure { Log.w(TAG, "consent failed: ${it.message}") }
    }

    /** تبلیغ آنی را از قبل می‌گیرد تا لحظه‌ی نمایش، تأخیری حس نشود. */
    fun preloadInterstitial() {
        if (!interstitialEnabled) return
        if (interstitialAdId != null || requestInFlight) return

        requestInFlight = true
        runCatching {
            Tapsell.requestInterstitialAd(
                AdsConfig.INTERSTITIAL_ZONE,
                object : RequestResultListener {
                    override fun onSuccess(adId: String) {
                        interstitialAdId = adId
                        requestInFlight = false
                    }

                    override fun onFailure(message: String) {
                        requestInFlight = false
                        Log.d(TAG, "interstitial request failed: $message")
                    }
                }
            )
        }.onFailure {
            requestInFlight = false
            Log.w(TAG, "interstitial request error: ${it.message}")
        }
    }

    /**
     * تبلیغ آنی را در زمان ورود کاربر نشان می‌دهد.
     * اگر تبلیغی آماده نباشد یا فاصله‌ی زمانی کم باشد، هیچ اتفاقی نمی‌افتد و فقط تبلیغ بعدی لود می‌شود.
     */
    fun showInterstitialOnEntry(activity: Activity) {
        if (!interstitialEnabled || showing) return

        val now = System.currentTimeMillis()
        if (now - lastShownAt < AdsConfig.INTERSTITIAL_MIN_INTERVAL_MS) return

        val adId = interstitialAdId
        if (adId == null) {
            preloadInterstitial()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return

        interstitialAdId = null
        showing = true
        lastShownAt = now

        runCatching {
            Tapsell.showInterstitialAd(
                adId,
                activity,
                object : AdStateListener.Interstitial {
                    override fun onAdImpression() = Unit

                    override fun onAdClicked() = Unit

                    override fun onAdClosed(completionState: AdShowCompletionState) {
                        showing = false
                        preloadInterstitial()
                    }

                    override fun onAdFailed(message: String) {
                        showing = false
                        lastShownAt = 0L
                        Log.d(TAG, "interstitial show failed: $message")
                        preloadInterstitial()
                    }
                }
            )
        }.onFailure {
            showing = false
            lastShownAt = 0L
            Log.w(TAG, "interstitial show error: ${it.message}")
        }
    }
}
