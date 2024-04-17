package com.jvziyaoyao.raw.camera.page.wheel

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity

class VibratorHelper(
    context: Context,
) {

    private var mVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(ComponentActivity.VIBRATOR_SERVICE) as Vibrator
    }

    fun playWheelVibrate() {
        val pattern = longArrayOf(10)
        if (mVibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mVibrator.vibrate(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    } else {
                        VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                )
            } else {
                // TODO 测试低版本系统
                mVibrator.vibrate(pattern, 0)
            }
        }
    }

    fun playRingtoneVibrate() {
        // 等待1200，振动800
        val pattern = longArrayOf(1200, 800)
        if (mVibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                mVibrator.vibrate(pattern, -1)
            }
        }
    }

    fun playWxRingtoneVibrate() {
        val pattern = longArrayOf(200, 1000, 1800, 190, 10, 190, 10, 190, 510)
        if (mVibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                mVibrator.vibrate(pattern, -1)
            }
        }
    }

    fun stopVibrate() {
        mVibrator.cancel()
    }


}