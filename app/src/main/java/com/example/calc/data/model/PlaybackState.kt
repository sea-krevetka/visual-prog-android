package com.example.calc.data.model

data class PlaybackState(
    val isPlaying: Boolean,
    val currentPosition: Int,
    val duration: Int,
    val currentFile: MusicFile?
)