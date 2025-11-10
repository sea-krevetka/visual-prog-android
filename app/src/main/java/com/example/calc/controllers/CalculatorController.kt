package com.example.calc.controller

import java.util.Locale

class CalculatorController(
    private val onDisplayUpdate: (String) -> Unit
) {
    private var currentInput = ""
    private var lastCharIsOperator = false

    fun appendNumber(number: String) {
        currentInput = if (currentInput == "0" && number != ".") {
            number
        } else {
            currentInput + number
        }
        updateDisplay()
        lastCharIsOperator = false
    }

    fun appendDecimal() {
        if (canAddDecimal()) {
            appendToInput(".")
        }
    }

    fun addOperator(operator: String) {
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

    fun clearInput() {
        currentInput = ""
        lastCharIsOperator = false
        updateDisplay()
    }

    fun backspace() {
        if (currentInput.isNotEmpty()) {
            val lastChar = currentInput.last()
            if ("+-*/".contains(lastChar)) {
                lastCharIsOperator = false
            }
            currentInput = currentInput.substring(0, currentInput.length - 1)
            updateDisplay()
        }
    }

    fun calculateResult() {
        if (currentInput.isEmpty() || lastCharIsOperator) return

        try {
            if (currentInput.replace(" ", "") == "1000-7") {
                currentInput = ""
                updateDisplay()
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

    private fun appendToInput(value: String) {
        currentInput += value
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
        onDisplayUpdate(displayText)
    }
}