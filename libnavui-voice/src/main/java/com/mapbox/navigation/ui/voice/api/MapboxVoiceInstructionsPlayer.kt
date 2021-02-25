package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.media.AudioManager
import androidx.annotation.UiThread
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Hybrid implementation of [MapboxVoiceInstructionsPlayer] combining [VoiceInstructionsTextPlayer] and
 * [VoiceInstructionsFilePlayer] speech players.
 * @property context Context
 * @property accessToken String
 * @property language [Locale] language (ISO 639)
 * @property options [VoiceInstructionsPlayerOptions] (optional)
 */
@UiThread
class MapboxVoiceInstructionsPlayer @JvmOverloads constructor(
    private val context: Context,
    private val accessToken: String,
    private val language: String,
    private val options: VoiceInstructionsPlayerOptions = VoiceInstructionsPlayerOptions.Builder()
        .build()
) {

    private val playCallbackQueue: Queue<PlayCallback> = ConcurrentLinkedQueue()
    private val filePlayer: VoiceInstructionsFilePlayer =
        VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
            context,
            accessToken,
            language
        )
    private val textPlayer: VoiceInstructionsTextPlayer =
        VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(context, language)
    private var localCallback: VoiceInstructionsPlayerCallback =
        object : VoiceInstructionsPlayerCallback {
            override fun onDone(announcement: SpeechAnnouncement) {
                audioFocusDelegate.abandonFocus()
                val currentPlayCallback = playCallbackQueue.poll()
                val currentAnnouncement = currentPlayCallback.announcement
                val currentClientCallback = currentPlayCallback.callback
                currentClientCallback.onDone(currentAnnouncement)
                play()
            }
        }
    private val audioFocusDelegate: AudioFocusDelegate =
        AudioFocusDelegateProvider.retrieveAudioFocusDelegate(
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
            options
        )

    /**
     * Given [SpeechAnnouncement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     * @param callback
     */
    fun play(
        announcement: SpeechAnnouncement,
        callback: VoiceInstructionsPlayerCallback
    ) {
        playCallbackQueue.add(PlayCallback(announcement, callback))
        if (playCallbackQueue.size == 1) {
            play()
        }
    }

    /**
     * The method will set the volume to the specified level from [SpeechVolume].
     * Volume is specified as a float ranging from 0 to 1
     * where 0 is silence, and 1 is the maximum volume (the default behavior).
     * @param state volume level.
     */
    fun volume(state: SpeechVolume) {
        if (state.level < MIN_VOLUME_LEVEL || state.level > MAX_VOLUME_LEVEL) {
            throw IllegalArgumentException(
                "Volume level needs to be a float ranging from 0 to 1."
            )
        }
        filePlayer.volume(state)
        textPlayer.volume(state)
    }

    /**
     * Clears any announcements queued.
     */
    fun clear() {
        clean()
        filePlayer.clear()
        textPlayer.clear()
    }

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    fun shutdown() {
        clean()
        filePlayer.shutdown()
        textPlayer.shutdown()
    }

    private fun play() {
        if (playCallbackQueue.isNotEmpty()) {
            audioFocusDelegate.requestFocus()
            val currentPlayCallback = playCallbackQueue.peek()
            val currentPlay = currentPlayCallback.announcement
            currentPlay.file?.let {
                filePlayer.play(currentPlay, localCallback)
            } ?: textPlayer.play(currentPlay, localCallback)
        }
    }

    private fun clean() {
        playCallbackQueue.clear()
    }

    private companion object {
        private const val MAX_VOLUME_LEVEL = 1.0f
        private const val MIN_VOLUME_LEVEL = 0.0f
    }
}