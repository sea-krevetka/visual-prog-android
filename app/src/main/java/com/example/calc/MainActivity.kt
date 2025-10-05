package com.example.calc

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var inputField: EditText
    private lateinit var kenImageView: ImageView
    private var currentInput = ""
    private var lastCharIsOperator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupNumberButtons()
        setupOperatorButtons()
        setupFunctionButtons()
        setupKenImage()
    }

    private fun initializeViews() {
        inputField = findViewById(R.id.inputField)
        kenImageView = findViewById(R.id.kenImageView)
    }

    private fun setupKenImage() {
        findViewById<View>(R.id.overlay).visibility = View.GONE
        findViewById<View>(R.id.imageContainer).visibility = View.GONE
        val overlay = findViewById<View>(R.id.overlay)
        val imageContainer = findViewById<View>(R.id.imageContainer)

        overlay.setOnClickListener {
            hideKenImage()
        }
        imageContainer.setOnClickListener {
            hideKenImage()
        }
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
                appendToInput((it as Button).text.toString())
            }
        }

        findViewById<Button>(R.id.btnDecimal).setOnClickListener {
            if (canAddDecimal()) {
                appendToInput(".")
            }
        }
    }

    private fun setupOperatorButtons() {
        findViewById<Button>(R.id.btnAdd).setOnClickListener { addOperator("+") }
        findViewById<Button>(R.id.btnSubtract).setOnClickListener { addOperator("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { addOperator("×") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { addOperator("÷") }
    }

    private fun setupFunctionButtons() {
        findViewById<Button>(R.id.btnClear).setOnClickListener { clearInput() }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { backspace() }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { calculateResult() }
    }

    private fun appendToInput(value: String) {
        currentInput = if (currentInput == "0" && value != ".") {
            value
        } else {
            currentInput + value
        }
        updateDisplay()
        lastCharIsOperator = false
    }

    private fun addOperator(operator: String) {
        if (currentInput.isEmpty()) {
            if (operator == "-") {
                currentInput = "-"
            }
        } else if (!lastCharIsOperator) {
            val calcOperator = when (operator) {
                "×" -> "*"
                "÷" -> "/"
                else -> operator
            }
            currentInput += calcOperator
            lastCharIsOperator = true
        }
        updateDisplay()
    }

    private fun canAddDecimal(): Boolean {
        if (currentInput.isEmpty()) return true

        val parts = currentInput.split("+", "-", "*", "/")
        if (parts.isNotEmpty()) {
            val lastNumber = parts.last()
            return !lastNumber.contains(".")
        }
        return true
    }

    private fun clearInput() {
        currentInput = ""
        lastCharIsOperator = false
        updateDisplay()
    }

    private fun backspace() {
        if (currentInput.isNotEmpty()) {
            val lastChar = currentInput.last()
            if ("+-*/".contains(lastChar)) {
                lastCharIsOperator = false
            }
            currentInput = currentInput.substring(0, currentInput.length - 1)
            updateDisplay()
        }
    }

    private fun calculateResult() {
        if (currentInput.isEmpty() || lastCharIsOperator) return

        try {
            if (currentInput.replace(" ", "") == "1000-7") {
                showKenImage()
                return
            }

            val result = evaluateExpression(currentInput)
            currentInput = formatResult(result)
            lastCharIsOperator = false
            updateDisplay()
        } catch (e: Exception) {
            currentInput = "Error"
            updateDisplay()
            currentInput = ""
        }
    }

    private fun showKenImage() {
        findViewById<View>(R.id.overlay).visibility = View.VISIBLE
        findViewById<View>(R.id.imageContainer).visibility = View.VISIBLE
        // Очищаем поле ввода
        currentInput = ""
        updateDisplay()
    }

    private fun evaluateExpression(expression: String): Double {
        var currentNumber = ""
        var currentOperator = '+'
        var result = 0.0
        var tempResult = 0.0

        for (i in expression.indices) {
            val ch = expression[i]

            if (ch.isDigit() || ch == '.') {
                currentNumber += ch
            }

            if ((!ch.isDigit() && ch != '.') || i == expression.length - 1) {
                if (currentNumber.isNotEmpty()) {
                    val number = currentNumber.toDouble()

                    when (currentOperator) {
                        '+' -> result += tempResult.also { tempResult = number }
                        '-' -> result += tempResult.also { tempResult = -number }
                        '*' -> tempResult *= number
                        '/' -> {
                            if (number == 0.0) throw ArithmeticException("Division by zero")
                            tempResult /= number
                        }
                        else -> tempResult = number
                    }

                    currentNumber = ""
                }

                if (ch in "+-*/") {
                    currentOperator = ch
                }
            }
        }

        result += tempResult
        return result
    }

    private fun formatResult(result: Double): String {
        return if (result == result.toLong().toDouble()) {
            String.format(Locale.getDefault(), "%d", result.toLong())
        } else {
            String.format(Locale.getDefault(), "%.2f", result)
        }
    }

    private fun updateDisplay() {
        val displayText = if (currentInput.isEmpty()) {
            "0"
        } else {
            currentInput.replace("*", "×").replace("/", "÷")
        }
        inputField.setText(displayText)
    }
}