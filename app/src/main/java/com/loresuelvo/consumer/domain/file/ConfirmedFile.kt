package com.loresuelvo.consumer.domain.file

data class ConfirmedFile(
    val id: String,
    val mimeType: String,
    val originalName: String,
    val codec: String,
    val durationSeconds: Int,
)