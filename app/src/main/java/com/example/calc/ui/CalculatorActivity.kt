package com.example.calc.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.calc.R
import com.example.calc.controller.CalculatorController

class CalculatorActivity : AppCompatActivity() {

    private lateinit var inputField: EditText
    private lateinit var kenImageView: ImageView
    private lateinit var calculatorController: CalculatorController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        initializeViews()
        setupCalculatorController()
        setupNumberButtons()
        setupOperatorButtons()
        setupFunctionButtons()
        setupKenImage()
    }

    private fun initializeViews() {
        inputField = findViewById(R.id.inputField)
        kenImageView = findViewById(R.id.kenImageView)
    }

    private fun setupCalculatorController() {
        calculatorController = CalculatorController { displayText ->
            inputField.setText(displayText)
        }
    }

    private fun setupKenImage() {
        findViewById<View>(R.id.overlay).visibility = View.GONE
        findViewById<View>(R.id.imageContainer).visibility = View.GONE
        val overlay = findViewById<View>(R.id.overlay)
        val imageContainer = findViewById<View>(R.id.imageContainer)

        overlay.setOnClickListener { hideKenImage() }
        imageContainer.setOnClickListener { hideKenImage() }
    }

    private fun hideKenImage() {
        findViewById<View>(R.id.overlay).visibility = View.GONE
        findViewById<View>(R.id.imageContainer).visibility = View.GONE
    }

    private fun setupNumberButtons() {
        val numberButtonIds = arrayOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        numberButtonIds.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                calculatorController.appendNumber((it as Button).text.toString())
            }
        }

        findViewById<Button>(R.id.btnDecimal).setOnClickListener {
            calculatorController.appendDecimal()
        }
    }

    private fun setupOperatorButtons() {
        findViewById<Button>(R.id.btnAdd).setOnClickListener { 
            calculatorController.addOperator("+") 
        }
        findViewById<Button>(R.id.btnSubtract).setOnClickListener { 
            calculatorController.addOperator("-") 
        }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { 
            calculatorController.addOperator("×") 
        }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { 
            calculatorController.addOperator("÷") 
        }
    }

    private fun setupFunctionButtons() {
        findViewById<Button>(R.id.btnClear).setOnClickListener { 
            calculatorController.clearInput() 
        }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { 
            calculatorController.backspace() 
        }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { 
            calculatorController.calculateResult() 
        }
    }

    private fun showKenImage() {
        findViewById<View>(R.id.overlay).visibility = View.VISIBLE
        findViewById<View>(R.id.imageContainer).visibility = View.VISIBLE
    }
}