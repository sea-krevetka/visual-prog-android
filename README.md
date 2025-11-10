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




### Воспроизведение музыки

#### Инициализация медиаплеера:

```kotlin
private fun setupMediaPlayer() {
    mediaPlayer = MediaPlayer()
    mediaPlayer.setOnCompletionListener {
        isPlaying = false
        btnPlay.text = "Play"
        seekBar.progress = 0
        updateTimeDisplay(0, mediaPlayer.duration)
    }
}
```

#### Алгоритм воспроизведения:

1. **Выбор файла**: пользователь выбирает файл из списка
2. **Подготовка плеера**: сброс предыдущего состояния и загрузка нового файла
3. **Старт воспроизведения**: запуск медиаплеера и обновление интерфейса

```kotlin
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
    } catch (e: Exception) {
        Toast.makeText(this, "Error playing file", Toast.LENGTH_SHORT).show()
    }
}
```

### Управление воспроизведением

#### Кнопки управления:

- **Play**: начинает воспроизведение выбранного файла
- **Pause**: приостанавливает воспроизведение
- **Stop**: полностью останавливает и сбрасывает плеер

```kotlin
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
```

### Прогресс воспроизведения

#### SeekBar обновление:

```kotlin
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
```

#### Форматирование времени:

```kotlin
private fun formatTime(milliseconds: Int): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) - 
                 TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, seconds)
}
```

### Управление громкостью

#### Реализация контроля громкости:

```kotlin
private fun setupVolumeControl() {
    volumeSeekBar.progress = 50
    volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val volume = progress / 100.0f
            mediaPlayer.setVolume(volume, volume)
        }
    })
}
```

## Поиск и загрузка музыкальных файлов

### Алгоритм поиска файлов:

```kotlin
private fun findMusicFiles(directory: File, musicFiles: MutableList<File>) {
    try {
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles()
            files?.forEach { file ->
                if (file.isDirectory) {
                    findMusicFiles(file, musicFiles) // Рекурсивный обход
                } else {
                    if (isMusicFile(file)) {
                        musicFiles.add(file)
                    }
                }
            }
        }
    } catch (e: SecurityException) {
        Toast.makeText(this, "Cannot access directory", Toast.LENGTH_SHORT).show()
    }
}
```

### Поддерживаемые форматы:

```kotlin
private fun isMusicFile(file: File): Boolean {
    val supportedExtensions = arrayOf(".mp3", ".wav", ".ogg", ".m4a", ".flac")
    val fileName = file.name.lowercase()
    return supportedExtensions.any { fileName.endsWith(it) }
}
```

## Управление разрешениями

### Запрос разрешений:

```kotlin
private fun checkPermissions() {
    val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
        != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE)
    } else {
        loadMusicFiles()
    }
}
```






#### Инициализация и проверка разрешений

```kotlin
private fun checkLocationPermissions() {
    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
        != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_CODE)
    } else {
        startLocationUpdates()
    }
}
```

#### Запрос обновлений местоположения

```kotlin
private fun startLocationUpdates() {
    if (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    // GPS провайдер (высокая точность)
    locationManager.requestLocationUpdates(
        LocationManager.GPS_PROVIDER,
        1000L,
        1f,
        this
    )

    // Network провайдер (низкая точность, но работает везде)
    locationManager.requestLocationUpdates(
        LocationManager.NETWORK_PROVIDER,
        1000L,
        1f,
        this
    )

    getLastKnownLocation()
    tvStatus.text = "Location updates started"
}
```

#### Обработка обновлений местоположения

```kotlin
override fun onLocationChanged(location: Location) {
    updateLocationInfo(location)
    saveLocationToFile(location)
}

private fun updateLocationInfo(location: Location) {
    tvLatitude.text = "Latitude: ${location.latitude}"
    tvLongitude.text = "Longitude: ${location.longitude}"
    tvAltitude.text = "Altitude: ${location.altitude}m"
    
    val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    tvTime.text = "Time: ${timeFormat.format(Date(location.time))}"
    
    tvStatus.text = "Location updated: ${timeFormat.format(Date())}"
}
```

### Сохранение данных о местоположении

#### Формат данных:

```kotlin
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val time: Long,
    val provider: String
)
```

#### Сохранение в JSON файл:

```kotlin
private fun saveLocationToFile(location: Location) {
    val locationData = LocationData(
        latitude = location.latitude,
        longitude = location.longitude,
        altitude = location.altitude,
        time = location.time,
        provider = location.provider ?: "unknown"
    )

    val jsonString = gson.toJson(locationData)
    
    try {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), locationFile)
        FileOutputStream(file, true).use { fos ->
            fos.write("$jsonString\n".toByteArray())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```

#### Сохранение последнего известного местоположения:

```kotlin
private fun getLastKnownLocation() {
    if (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

    lastKnownLocation?.let {
        updateLocationInfo(it)
    } ?: run {
        tvStatus.text = "No last known location"
    }
}
```

### Безопасность и разрешения:

#### Необходимые разрешения в AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```


### Управление жизненным циклом:

```kotlin
override fun onPause() {
    super.onPause()
    locationManager.removeUpdates(this) // Экономия батареи
}

override fun onResume() {
    super.onResume()
    if (permissionsGranted) {
        startLocationUpdates()
    }
}
```
