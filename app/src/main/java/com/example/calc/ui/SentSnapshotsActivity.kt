package com.example.calc.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calc.R
import com.example.calc.data.repository.SendLogRepository
import com.example.calc.ui.adapters.SentSnapshotsAdapter

class SentSnapshotsActivity : AppCompatActivity() {
    private val TAG = "SentSnapshotsActivity"
    private lateinit var btnRefresh: Button
    private lateinit var btnClear: Button
    private lateinit var rvSent: RecyclerView
    private lateinit var adapter: SentSnapshotsAdapter
    private lateinit var repo: SendLogRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sent_snapshots)
        repo = SendLogRepository(this)
        btnRefresh = findViewById(R.id.btnRefreshSent)
        btnClear = findViewById(R.id.btnClearSent)
        rvSent = findViewById(R.id.rvSentSnapshots)
        adapter = SentSnapshotsAdapter()
        rvSent.layoutManager = LinearLayoutManager(this)
        rvSent.adapter = adapter

        btnRefresh.setOnClickListener { load() }
        btnClear.setOnClickListener {
            repo.clear()
            load()
        }
        load()
    }

    private fun load() {
        try {
            Log.d(TAG, "Loading sent/saved data")
            val list = repo.readAll()
            Log.d(TAG, "Loaded ${list.size} entries")
            adapter.setItems(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading data: ${e.message}", e)
        }
    }
}
