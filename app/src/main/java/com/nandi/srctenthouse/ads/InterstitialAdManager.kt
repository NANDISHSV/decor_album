package com.nandi.srctenthouse.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdManager {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun preload(activity: Activity) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            activity,
            AdIds.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    fun showIfAvailable(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            preload(activity) // wasn't ready this time — make sure it's loading for next time
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preload(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                preload(activity)
                onDismissed()
            }
        }
        ad.show(activity)
    }
}