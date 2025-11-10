package com.example.calc.controller

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.example.calc.data.model.MusicFile
import com.example.calc.utils.TimeUtils
import java.util.concurrent.TimeUnit

class MediaPlayerController(
    private val context: android.content.Context,
    private val listener: PlayerListener
) {
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var currentFile: MusicFile? = null

    interface PlayerListener {
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onProgressUpdate(currentPosition: Int, duration: Int)
        fun onPlaybackComplete()
        fun onError(message: String)
    }

    init {
        setupMediaPlayer()
    }

    private fun setupMediaPlayer() {
        mediaPlayer.setOnCompletionListener {
            isPlaying = false
            listener.onPlaybackComplete()
        }
    }

    fun playMusicFile(musicFile: MusicFile) {
        currentFile = musicFile
        stop()
        
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(musicFile.path)
            mediaPlayer.prepare()
            listener.onProgressUpdate(0, mediaPlayer.duration)
            play()
        } catch (e: Exception) {
            listener.onError("Error playing file")
            e.printStackTrace()
        }
    }

    fun play() {
        if (currentFile == null) {
            listener.onError("Please select a music file first")
            return
        }

        if (!isPlaying) {
            mediaPlayer.start()
            isPlaying = true
            listener.onPlaybackStateChanged(true)
            startProgressUpdates()
        }
    }

    fun pause() {
        if (isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
            listener.onPlaybackStateChanged(false)
        }
    }

    fun stop() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.reset()
        }
        isPlaying = false
        listener.onPlaybackStateChanged(false)
        handler.removeCallbacks(updateProgress)
    }

    fun seekTo(progress: Int) {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.seekTo(progress)
        }
    }

    fun setVolume(volume: Float) {
        mediaPlayer.setVolume(volume, volume)
    }

    fun formatTime(milliseconds: Int): String {
        return TimeUtils.formatTime(milliseconds)
    }

    private fun startProgressUpdates() {
        handler.post(updateProgress)
    }

    private val updateProgress = object : Runnable {
        override fun run() {
            if (mediaPlayer.isPlaying) {
                val currentPosition = mediaPlayer.currentPosition
                val duration = mediaPlayer.duration
                listener.onProgressUpdate(currentPosition, duration)
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun release() {
        handler.removeCallbacks(updateProgress)
        mediaPlayer.release()
    }
}