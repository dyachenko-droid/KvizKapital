package ru.kvizcap.quiz.app.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import ru.kvizcap.quiz.app.R
import ru.kvizcap.quiz.app.mvi.Buzz
import ru.kvizcap.quiz.app.mvi.Sfx

class SoundBoard(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()

    private val ids = mapOf(
        Sfx.TAP to R.raw.sfx_tap,
        Sfx.CORRECT to R.raw.sfx_correct,
        Sfx.WRONG to R.raw.sfx_wrong,
        Sfx.COIN to R.raw.sfx_coin,
        Sfx.COLLECT to R.raw.sfx_collect,
        Sfx.UPGRADE to R.raw.sfx_upgrade,
        Sfx.RANK to R.raw.sfx_rank,
        Sfx.TICK to R.raw.sfx_tick,
        Sfx.FINISH to R.raw.sfx_finish,
    ).mapValues { (_, res) -> pool.load(context, res, 1) }

    private val volume = mapOf(Sfx.TAP to 0.35f, Sfx.TICK to 0.4f)

    fun play(sfx: Sfx) {
        val id = ids[sfx] ?: return
        val v = volume[sfx] ?: 0.8f
        pool.play(id, v, v, 1, 0, 1f)
    }

    fun release() = pool.release()
}

class Haptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun buzz(b: Buzz) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = if (Build.VERSION.SDK_INT >= 29) {
            when (b) {
                Buzz.LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                Buzz.CONFIRM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                Buzz.REJECT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                Buzz.HEAVY -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            }
        } else {
            when (b) {
                Buzz.LIGHT -> VibrationEffect.createOneShot(10, 60)
                Buzz.CONFIRM -> VibrationEffect.createOneShot(20, 140)
                Buzz.REJECT -> VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 30), intArrayOf(0, 180, 0, 180), -1)
                Buzz.HEAVY -> VibrationEffect.createOneShot(45, 255)
            }
        }
        v.vibrate(effect)
    }
}
