package com.example.calc.data.model

import java.io.File

data class MusicFile(
    val file: File,
    val name: String,
    val path: String,
    val size: Long
) {
    constructor(file: File) : this(
        file = file,
        name = file.name,
        path = file.absolutePath,
        size = file.length()
    )
}