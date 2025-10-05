## Как работает подсчет выражений

### Основной алгоритм вычислений

Калькулятор использует алгоритм для вычисления математических выражений, реализованный в методе `evaluateExpression()`. Вот как он работает:

#### Шаги алгоритма:

1. **Инициализация переменных**:
   - `currentNumber` - накопление текущего числа
   - `currentOperator` - текущий оператор (по умолчанию '+')
   - `result` - финальный результат
   - `tempResult` - временный результат для операций с приоритетом

2. **Пошаговый парсинг выражения**:
   - Проход по каждому символу выражения
   - Накопление цифр и точки в `currentNumber`

3. **Обработка операторов и чисел**:
   - При встрече оператора или в конце выражения:
     - Преобразование `currentNumber` в число
     - Применение текущего оператора к числу
     - Сброс `currentNumber`

4. **Логика операторов**:
   - `+` и `-`: добавляют `tempResult` к `result` и устанавливают новое значение
   - `*` и `/`: выполняют операции непосредственно с `tempResult`

### Обработка особых случаев:

1. **Деление на ноль**: генерирует исключение `ArithmeticException`
2. **Пустой ввод**: возвращает "0"
3. **Оператор в конце**: игнорируется при вычислении
4. **Несколько операторов подряд**: учитывается только последний


```
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
```


bonus: при вводе определенного выражения есть пасхалка..
