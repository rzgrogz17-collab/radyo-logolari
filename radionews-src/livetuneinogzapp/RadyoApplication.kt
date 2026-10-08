package com.globalradio.livetuneinogzapp

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.globalradio.livetuneinogzapp.network.RetrofitClient
import com.globalradio.livetuneinogzapp.utils.SleepTimerManager
import com.globalradio.livetuneinogzapp.utils.AppSettings
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class RadyoApplication : Application() {

    override fun onCreate() {
        AppSettings.applyNightMode(this)
        super.onCreate()
        RetrofitClient.init(context = this)
        startOneSignal()

        // Uyku zamanlayıcısı süresi bitince ödüllü reklam gösterebilmek için
        // o anda ön planda olan Activity'yi takip eder.
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                SleepTimerManager.trackActivity(activity, resumed = true)
            }

            override fun onActivityPaused(activity: Activity) {
                SleepTimerManager.trackActivity(activity, resumed = false)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun startOneSignal() {
        val appId = getString(R.string.onesignal_app_id).trim()
        if (appId.isEmpty() || appId == "YOUR_ONESIGNAL_APP_ID") return
        OneSignal.Debug.logLevel = LogLevel.WARN
        OneSignal.initWithContext(this, appId)
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(true)
        }
    }
}