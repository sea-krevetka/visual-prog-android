package com.example.calc.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calc.R
import com.example.calc.controller.MediaPlayerController
import com.example.calc.data.model.MusicFile
import com.example.calc.data.repository.MusicRepository
import java.io.File

class MediaPlayerActivity : AppCompatActivity(), MediaPlayerController.PlayerListener {

    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var volumeSeekBar: SeekBar

    private lateinit var mediaPlayerController: MediaPlayerController
    private lateinit var musicRepository: MusicRepository

    companion object {
        private const val REQUEST_PERMISSION_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)

        initializeViews()
        setupControllers()
        checkPermissions()
    }

    private fun initializeViews() {
        seekBar = findViewById(R.id.seekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        btnPlay = findViewById(R.id.btnPlay)
        btnPause = findViewById(R.id.btnPause)
        btnStop = findViewById(R.id.btnStop)
        recyclerView = findViewById(R.id.recyclerView)
        volumeSeekBar = findViewById(R.id.volumeSeekBar)

        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupControllers() {
        musicRepository = MusicRepository(this)
        mediaPlayerController = MediaPlayerController(this, this)

        setupButtonListeners()
        setupVolumeControl()
    }

    private fun setupButtonListeners() {
        btnPlay.setOnClickListener { mediaPlayerController.play() }
        btnPause.setOnClickListener { mediaPlayerController.pause() }
        btnStop.setOnClickListener { mediaPlayerController.stop() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayerController.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupVolumeControl() {
        volumeSeekBar.progress = 50
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mediaPlayerController.setVolume(progress / 100.0f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE)
        } else {
            loadMusicFiles()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE && grantResults.isNotEmpty() && 
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMusicFiles()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMusicFiles() {
        val musicFiles = musicRepository.getMusicFiles()
        val adapter = MusicAdapter(musicFiles) { musicFile ->
            mediaPlayerController.playMusicFile(musicFile)
            Toast.makeText(this, "Playing: ${musicFile.name}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter
    }

    // MediaPlayerController.PlayerListener implementations
    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        btnPlay.text = if (isPlaying) "Playing" else "Play"
    }

    override fun onProgressUpdate(currentPosition: Int, duration: Int) {
        seekBar.max = duration
        seekBar.progress = currentPosition
        tvCurrentTime.text = mediaPlayerController.formatTime(currentPosition)
        tvTotalTime.text = mediaPlayerController.formatTime(duration)
    }

    override fun onPlaybackComplete() {
        seekBar.progress = 0
        btnPlay.text = "Play"
    }

    override fun onError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayerController.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayerController.release()
    }
}