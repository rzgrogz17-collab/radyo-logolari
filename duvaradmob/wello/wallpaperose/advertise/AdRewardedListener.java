package com.wello.wallpaperose.advertise;

/** Ödüllü AdMob reklamının sonucu. */
public interface AdRewardedListener {
    void onComplete();

    void onDismissed();

    void onError();
}
