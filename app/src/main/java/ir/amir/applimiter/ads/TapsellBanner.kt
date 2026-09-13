package ir.amir.applimiter.ads

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import ir.tapsell.mediation.Tapsell
import ir.tapsell.mediation.ad.AdStateListener
import ir.tapsell.mediation.ad.request.BannerSize
import ir.tapsell.mediation.ad.request.RequestResultListener
import ir.tapsell.mediation.ad.views.banner.BannerContainer

/** بنر تپسل به شکل یک کامپوزبل؛ اگر زون ست نشده باشد چیزی رندر نمی‌کند. */
@Composable
fun TapsellBanner(modifier: Modifier = Modifier) {
    if (!AdsManager.bannerEnabled) return
    val activity = LocalContext.current as? Activity ?: return

    val container = remember { BannerContainer(activity) }
    var adId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            Tapsell.requestBannerAd(
                AdsConfig.BANNER_ZONE,
                BannerSize.BANNER_320_50,
                object : RequestResultListener {
                    override fun onSuccess(id: String) {
                        adId = id
                        runCatching {
                            Tapsell.showBannerAd(
                                id,
                                container,
                                activity,
                                object : AdStateListener.Banner {
                                    override fun onAdImpression() = Unit
                                    override fun onAdClicked() = Unit
                                    override fun onAdFailed(message: String) = Unit
                                }
                            )
                        }
                    }

                    override fun onFailure(message: String) = Unit
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            adId?.let { runCatching { Tapsell.destroyBannerAd(it) } }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { container }
    )
}
