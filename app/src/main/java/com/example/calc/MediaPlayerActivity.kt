package com.example.calc

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.concurrent.TimeUnit
import java.io.FileOutputStream
import java.io.InputStream

class MediaPlayerActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var volumeSeekBar: SeekBar

    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var currentFile: File? = null

    companion object {
        private const val REQUEST_PERMISSION_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)

        initializeViews()
        setupMediaPlayer()
        checkPermissions()
        setupVolumeControl()
    }

    private fun initializeViews() {
        mediaPlayer = MediaPlayer()
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

    private fun setupMediaPlayer() {
        btnPlay.setOnClickListener {
            playMusic()
        }

        btnPause.setOnClickListener {
            pauseMusic()
        }

        btnStop.setOnClickListener {
            stopMusic()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer.isPlaying) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mediaPlayer.setOnCompletionListener {
            isPlaying = false
            btnPlay.text = "Play"
            seekBar.progress = 0
            updateTimeDisplay(0, mediaPlayer.duration)
        }
    }

    private fun setupVolumeControl() {
        volumeSeekBar.progress = 50
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val volume = progress / 100.0f
                mediaPlayer.setVolume(volume, volume)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun checkPermissions() {
        // On Android 13+ use READ_MEDIA_AUDIO for audio files; older versions use READ_EXTERNAL_STORAGE
        val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val permissions = arrayOf(readPermission)

        if (ContextCompat.checkSelfPermission(this, readPermission) != PackageManager.PERMISSION_GRANTED) {
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
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusicFiles()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMusicFiles() {
        val musicFiles = mutableListOf<File>()
        val storageDir = Environment.getExternalStorageDirectory()

        findMusicFiles(storageDir, musicFiles)

        // If no files found on external storage, try bundled assets or raw resources
        if (musicFiles.isEmpty()) {
            val bundled = loadBundledMusic()
            if (bundled.isEmpty()) {
                Toast.makeText(this, "No music files found", Toast.LENGTH_SHORT).show()
            } else {
                musicFiles.addAll(bundled)
            }
        }

        val adapter = MusicAdapter(musicFiles) { file ->
            playSelectedMusic(file)
        }
        recyclerView.adapter = adapter
    }

    private fun loadBundledMusic(): List<File> {
        val result = mutableListOf<File>()

        // Try assets/music first
        try {
            val assetList = assets.list("music")
            if (assetList != null && assetList.isNotEmpty()) {
                val outDir = File(cacheDir, "bundled_music")
                if (!outDir.exists()) outDir.mkdirs()
                assetList.forEach { name ->
                    try {
                        val input: InputStream = assets.open("music/$name")
                        val outFile = File(outDir, name)
                        copyStreamToFile(input, outFile)
                        result.add(outFile)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (result.isNotEmpty()) return result
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: load from res/raw (use reflection to enumerate R.raw fields)
        try {
            val rawClass = R.raw::class.java
            val fields = rawClass.fields
            if (fields.isNotEmpty()) {
                val outDir = File(cacheDir, "bundled_music")
                if (!outDir.exists()) outDir.mkdirs()
                for (field in fields) {
                    try {
                        val resId = field.getInt(null)
                        val name = field.name
                        val input = resources.openRawResource(resId)
                        val outFile = File(outDir, "$name.mp3")
                        copyStreamToFile(input, outFile)
                        result.add(outFile)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    private fun copyStreamToFile(input: InputStream, outFile: File) {
        input.use { inputStream ->
            FileOutputStream(outFile).use { output ->
                inputStream.copyTo(output)
            }
        }
    }

    private fun findMusicFiles(directory: File, musicFiles: MutableList<File>) {
        try {
            if (directory.exists() && directory.isDirectory) {
                val files = directory.listFiles()
                files?.forEach { file ->
                    if (file.isDirectory) {
                        findMusicFiles(file, musicFiles)
                    } else {
                        if (isMusicFile(file)) {
                            musicFiles.add(file)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Cannot access directory: ${directory.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isMusicFile(file: File): Boolean {
        val supportedExtensions = arrayOf(".mp3", ".wav", ".ogg", ".m4a", ".flac")
        val fileName = file.name.lowercase()
        return supportedExtensions.any { fileName.endsWith(it) }
    }

    private fun playSelectedMusic(file: File) {
        currentFile = file
        stopMusic()
        
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.prepare()
            
            seekBar.max = mediaPlayer.duration
            updateTimeDisplay(0, mediaPlayer.duration)
            
            playMusic()
            
            Toast.makeText(this, "Playing: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing file", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun playMusic() {
        if (currentFile == null) {
            Toast.makeText(this, "Please select a music file first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isPlaying) {
            mediaPlayer.start()
            isPlaying = true
            btnPlay.text = "Playing"
            startSeekBarUpdate()
        }
    }

    private fun pauseMusic() {
        if (isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
            btnPlay.text = "Play"
        }
    }

    private fun stopMusic() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.reset()
        }
        isPlaying = false
        btnPlay.text = "Play"
        seekBar.progress = 0
        updateTimeDisplay(0, if (mediaPlayer.duration > 0) mediaPlayer.duration else 0)
        handler.removeCallbacks(updateSeekBar)
    }

    private fun startSeekBarUpdate() {
        handler.postDelayed(updateSeekBar, 1000)
    }

    private val updateSeekBar = object : Runnable {
        override fun run() {
            if (mediaPlayer.isPlaying) {
                val currentPosition = mediaPlayer.currentPosition
                seekBar.progress = currentPosition
                updateTimeDisplay(currentPosition, mediaPlayer.duration)
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun updateTimeDisplay(current: Int, total: Int) {
        tvCurrentTime.text = formatTime(current)
        tvTotalTime.text = formatTime(total)
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) - 
                     TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            pauseMusic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBar)
        mediaPlayer.release()
    }
}