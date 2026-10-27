package com.qrpro.scanner.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Feedback {

    fun scanSuccess(context: Context, vibrate: Boolean, beep: Boolean) {
        if (vibrate) {
            runCatching {
                vibrator(context).vibrate(
                    VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }
        if (beep) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 140)
                Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 400)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrator(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
}
