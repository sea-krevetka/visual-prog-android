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

    findViewById<Button>(R.id.btnGoToTelephony).setOnClickListener {
        val intent = Intent(this, TelephonyActivity::class.java)
        startActivity(intent)
    }

    findViewById<Button>(R.id.btnGoToZMQ).setOnClickListener {
        val intent = Intent(this, ZMQActivity::class.java)
        startActivity(intent)
    }
}
    findViewById<Button>(R.id.btnGoToPlayer).setOnClickListener {
        val intent = Intent(this, MediaPlayerActivity::class.java)
        startActivity(intent)
    }

        findViewById<Button>(R.id.btnGoToOther).setOnClickListener {
            val intent = Intent(this, OtherActivity::class.java)
            startActivity(intent)
        }
    }
}