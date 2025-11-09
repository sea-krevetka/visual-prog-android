package com.example.calc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupNavigationButtons()
    }

    private fun setupNavigationButtons() {
        // Only wire buttons for activities that exist in the project.
        findViewById<Button>(R.id.btnGoToCalculator).setOnClickListener {
            val intent = Intent(this, CalculatorActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnGoToPlayer).setOnClickListener {
            val intent = Intent(this, MediaPlayerActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnGoToLocation).setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            startActivity(intent)
        }
    }
}