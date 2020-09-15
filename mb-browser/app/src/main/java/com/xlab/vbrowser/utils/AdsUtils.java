package com.xlab.vbrowser.utils;

public class AdsUtils {
    /*public static void loadAds(final Context context, final AdView adView, final boolean animate) {
        if (context == null) {
            return;
        }

        //Initialize Admob
        MobileAds.initialize(context, context.getString(R.string.admob_app_id));
        AdRequest.Builder builder = new AdRequest.Builder();

        if (BuildConfig.DEBUG) {
            //builder.addTestDevice("BC40BF527166C8B3DDFA9A76D0784C32");
        }

        AdRequest adRequest = builder.build();
        adView.setAdListener(new AdListener(){
            @Override
            public void onAdLoaded() {
                if (animate) {
                    Animation animation = new TranslateAnimation(0, 0, -50, 0);
                    animation.setDuration(200);
                    animation.setFillAfter(true);
                    adView.startAnimation(animation);
                }

                adView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                adView.setVisibility(View.GONE);
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
            }
        });
        adView.loadAd(adRequest);
    }

    public static InterstitialAd loadInterstitalAds(final Context context, final IAdsUtils adsUtils) {
        if (context == null) {
            return null;
        }

        //Initialize Admob
        MobileAds.initialize(context, context.getString(R.string.admob_app_id));

        final InterstitialAd interstitialAd = new InterstitialAd(context);
        interstitialAd.setAdUnitId(context.getString(R.string.admon_browser_insterstital_ads_id));
        interstitialAd.loadAd(new AdRequest.Builder().build());

        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                adsUtils.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when the ad is displayed.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                adsUtils.onAdClosed();
            }
        });

        return interstitialAd;
    }*/
}
