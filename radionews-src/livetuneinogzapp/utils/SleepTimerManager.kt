package com.globalradio.livetuneinogzapp.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData
import com.globalradio.livetuneinogzapp.ads.AdManager
import com.globalradio.livetuneinogzapp.service.RadioPlayerService
import java.lang.ref.WeakReference

/**
 * Uyku zamanlayıcısı.
 * Süre bitince (isteğe bağlı fade) radyo durur veya kısılır.
 * Ekran kilitliyken de sayaç devam eder.
 */
object SleepTimerManager {

    val remainingMs = MutableLiveData(0L)
    val isActive = MutableLiveData(false)
    val totalMs = MutableLiveData(0L)

    private var timer: CountDownTimer? = null
    private var appContext: Context? = null
    private var currentActivityRef: WeakReference<Activity>? = null
    private var fadeStarted = false

    fun trackActivity(activity: Activity, resumed: Boolean) {
        currentActivityRef = if (resumed) WeakReference(activity) else null
    }

    private fun foregroundActivity(): Activity? =
        currentActivityRef?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }

    fun start(context: Context, durationMs: Long, total: Long = durationMs) {
        appContext = context.applicationContext
        cancel(notify = false)
        sendAction(RadioPlayerService.ACTION_RESTORE_VOLUME)
        fadeStarted = false
        val dur = durationMs.coerceAtLeast(1_000L)
        isActive.postValue(true)
        remainingMs.postValue(dur)
        totalMs.postValue(total.coerceAtLeast(dur))
        AppSettings(context).lastTimerMinutes = ((dur + 30_000L) / 60_000L).toInt().coerceIn(1, 180)

        timer = object : CountDownTimer(dur, 1_000L) {
            override fun onTick(ms: Long) {
                remainingMs.postValue(ms)
                maybeStartFade(ms)
                if (ms < 21_000L || (ms / 1000L) % 5L == 0L) {
                    sendAction(RadioPlayerService.ACTION_REFRESH_NOTIF)
                }
            }

            override fun onFinish() {
                remainingMs.postValue(0L)
                isActive.postValue(false)
                totalMs.postValue(0L)
                timer = null
                finishPlayback()
            }
        }.start()
    }

    fun extend(extraMs: Long) {
        val ctx = appContext ?: return
        val rem = remainingMs.value ?: 0L
        if (rem <= 0L) return
        val tot = (totalMs.value ?: rem) + extraMs
        start(ctx, rem + extraMs, tot)
    }

    fun shorten(deltaMs: Long) {
        val ctx = appContext ?: return
        val rem = remainingMs.value ?: 0L
        if (rem <= 0L) return
        val next = rem - deltaMs
        if (next <= 2_000L) {
            cancel()
            finishPlayback()
            return
        }
        val tot = (totalMs.value ?: rem).coerceAtLeast(next)
        start(ctx, next, tot)
    }

    private fun maybeStartFade(ms: Long) {
        val ctx = appContext ?: return
        if (!AppSettings(ctx).timerFadeOut || fadeStarted) return
        if (ms in 1L..20_000L) {
            fadeStarted = true
            sendAction(RadioPlayerService.ACTION_FADE_VOLUME)
        }
    }

    private fun finishPlayback() {
        val ctx = appContext ?: return
        val mute = AppSettings(ctx).timerMuteInsteadOfStop
        val activity = foregroundActivity()
        val afterAd = {
            sendAction(
                if (mute) RadioPlayerService.ACTION_TIMER_MUTE
                else RadioPlayerService.ACTION_TIMER_STOP
            )
        }
        if (activity != null && !mute) {
            AdManager.showRewardedThenRun(activity) { afterAd() }
        } else {
            afterAd()
        }
    }

    private fun sendAction(action: String) {
        val ctx = appContext ?: return
        try {
            ctx.startService(Intent(ctx, RadioPlayerService::class.java).setAction(action))
        } catch (_: Exception) {
        }
    }

    fun cancel(notify: Boolean = true) {
        timer?.cancel()
        timer = null
        fadeStarted = false
        if (notify) {
            isActive.postValue(false)
            remainingMs.postValue(0L)
            totalMs.postValue(0L)
            sendAction(RadioPlayerService.ACTION_RESTORE_VOLUME)
        }
    }

    fun progress(): Float {
        val tot = totalMs.value ?: 0L
        val rem = remainingMs.value ?: 0L
        if (tot <= 0L) return 0f
        return (rem.toFloat() / tot.toFloat()).coerceIn(0f, 1f)
    }

    fun formatRemaining(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val h = totalSec / 3600
        val min = (totalSec % 3600) / 60
        val sec = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, min, sec)
        else "%d:%02d".format(min, sec)
    }
}
