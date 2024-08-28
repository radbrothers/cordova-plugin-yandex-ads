package io.luzh.cordova.plugin

import android.net.Uri
import android.util.Log
// import packagenamehere.R
import com.yandex.mobile.ads.common.InitializationListener
import com.yandex.mobile.ads.common.MobileAds.initialize
import com.yandex.mobile.ads.common.MobileAds.setUserConsent
import io.luzh.cordova.plugin.helpers.BannerAdsHelper
import io.luzh.cordova.plugin.helpers.FeedAdsHelper
import io.luzh.cordova.plugin.helpers.InterstitialAdsHelper
import io.luzh.cordova.plugin.helpers.OpenAppAdsHelper
import io.luzh.cordova.plugin.helpers.RewardedAdsHelper
// import io.luzh.cordova.plugin.helpers.instream.InstreamAdsHelper
import io.luzh.cordova.plugin.utils.Constants
import io.luzh.cordova.plugin.utils.Constants.KEY_BANNER_AT_TOP
import io.luzh.cordova.plugin.utils.Constants.KEY_BANNER_SIZE
import io.luzh.cordova.plugin.utils.Constants.KEY_BLOCK_ID_BANNER
import io.luzh.cordova.plugin.utils.Constants.KEY_BLOCK_ID_FEED
import io.luzh.cordova.plugin.utils.Constants.KEY_BLOCK_ID_INSTREAM
import io.luzh.cordova.plugin.utils.Constants.KEY_BLOCK_ID_INTERSTITIAL
import io.luzh.cordova.plugin.utils.Constants.KEY_BLOCK_ID_OPEN_APP
import io.luzh.cordova.plugin.utils.Constants.KEY_BLOCK_ID_REWARDED
import io.luzh.cordova.plugin.utils.Constants.KEY_OPTIONS
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_HIDE_BANNER
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_HIDE_FEED_APP_ADS
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_HIDE_INSTREAM_APP_ADS
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_LOAD_BANNER
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_LOAD_FEED_APP_ADS
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_LOAD_INSTREAM_APP_ADS
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_LOAD_INTERSTITIAL
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_LOAD_OPEN_APP_ADS
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_RELOAD_BANNER
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_RUN
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_SET_USER_CONSENT
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_SHOW_BANNER
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_SHOW_FEED_APP_ADS
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_SHOW_INSTREAM_APP_ADS
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_SHOW_INTERSTITIAL
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_SHOW_OPEN_APP_ADS
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_SHOW_REWARDED_VIDEO
import io.luzh.cordova.plugin.utils.ConstantsActions.ACTION_lOAD_REWARDED_VIDEO
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.json.JSONArray
import org.json.JSONException

class YandexAdsPlugin : CordovaPlugin() {
    private var cordovaWebView: CordovaWebView? = null

    private var bannerAdsHelper: BannerAdsHelper? = null
    private var rewardedAdsHelper: RewardedAdsHelper? = null
    private var interstitialAdsHelper: InterstitialAdsHelper? = null
    private var openAppAdsHelper: OpenAppAdsHelper? = null
    private var instreamAdsHelper: InstreamAdsHelper? = null
    private var feedAdsHelper: FeedAdsHelper? = null

    @Throws(JSONException::class)
    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {
        return when (action) {
            // Global
            ACTION_RUN -> { initAction(args, callbackContext); true }
            ACTION_SET_USER_CONSENT -> { setUserConsentAction(args.getBoolean(0), callbackContext); true }

            // Reward video ads
            ACTION_lOAD_REWARDED_VIDEO -> { rewardedAdsHelper?.load(callbackContext); true }
            ACTION_SHOW_REWARDED_VIDEO -> { rewardedAdsHelper?.show(callbackContext); true }

            // Banner ads
            ACTION_LOAD_BANNER -> { bannerAdsHelper?.load(callbackContext); true }
            ACTION_SHOW_BANNER -> { bannerAdsHelper?.show(callbackContext); true }
            ACTION_HIDE_BANNER -> { bannerAdsHelper?.hide(callbackContext); true }
            ACTION_RELOAD_BANNER -> { bannerAdsHelper?.reload(callbackContext); true }

            // Interstitial ads
            ACTION_LOAD_INTERSTITIAL -> { interstitialAdsHelper?.load(callbackContext); true }
            ACTION_SHOW_INTERSTITIAL -> { interstitialAdsHelper?.show(callbackContext); true }

            // Open app ads
            ACTION_LOAD_OPEN_APP_ADS -> { openAppAdsHelper?.load(callbackContext); true }
            ACTION_SHOW_OPEN_APP_ADS -> { openAppAdsHelper?.show(callbackContext); true }

            // Instream ads
            // ACTION_LOAD_INSTREAM_APP_ADS -> { instreamAdsHelper?.load(callbackContext); true }
            // ACTION_SHOW_INSTREAM_APP_ADS -> { instreamAdsHelper?.show(callbackContext); true }
            // ACTION_HIDE_INSTREAM_APP_ADS -> { instreamAdsHelper?.hide(callbackContext); true }

            // Feed
            ACTION_LOAD_FEED_APP_ADS -> { feedAdsHelper?.load(callbackContext); true }
            ACTION_SHOW_FEED_APP_ADS -> { feedAdsHelper?.show(callbackContext); true }
            ACTION_HIDE_FEED_APP_ADS -> { feedAdsHelper?.hide(callbackContext); true }

            // Unknown
            else -> false
        }
    }

    override fun initialize(cordova: CordovaInterface?, webView: CordovaWebView?) {
        cordovaWebView = webView
    }


    /**
     * Intilization action Initializes Yandex Ads
     */
    private fun initAction(args: JSONArray, callbackContext: CallbackContext) {
        val webView = cordovaWebView ?: return
        val rewardedBlockId: String = args.getString(KEY_BLOCK_ID_REWARDED)
        val interstitialBlockId: String = args.getString(KEY_BLOCK_ID_INTERSTITIAL)
        val bannerBlockId: String = args.getString(KEY_BLOCK_ID_BANNER)
        val openAppBlockId: String = args.getString(KEY_BLOCK_ID_OPEN_APP)
        // val instreamBlockId: String = args.getString(KEY_BLOCK_ID_INSTREAM)
        val feedBlockId: String = args.getString(KEY_BLOCK_ID_FEED)
        val options = args.optJSONObject(KEY_OPTIONS)

        val bannerAtTop = options.optBoolean(KEY_BANNER_AT_TOP, false)
        val bannerSize = options.optJSONObject(KEY_BANNER_SIZE)

        // val intreamContentUrl = Uri.parse("android.resource://" + cordova.context.packageName + "/" + R.raw.jc).toString()

        bannerAdsHelper = BannerAdsHelper(this, webView, bannerBlockId, bannerAtTop, bannerSize)
        rewardedAdsHelper = RewardedAdsHelper(this, webView, rewardedBlockId)
        interstitialAdsHelper = InterstitialAdsHelper(this, webView, interstitialBlockId)
        openAppAdsHelper = OpenAppAdsHelper(this, webView, openAppBlockId)
        //instreamAdsHelper = InstreamAdsHelper(this, webView, instreamBlockId, intreamContentUrl)
        feedAdsHelper = FeedAdsHelper(this, webView, feedBlockId)


        initialize(
            cordova.activity,
            InitializationListener { callbackContext.success() })
    }


    private fun setUserConsentAction(value: Boolean, callbackContext: CallbackContext) {
        setUserConsent(value)
        Log.d(Constants.YANDEX_ADS_TAG, "setUserConsent: $value")
        callbackContext.success()
    }
}
