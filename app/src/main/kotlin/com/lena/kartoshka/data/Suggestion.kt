package com.lena.kartoshka.data

import androidx.compose.ui.graphics.Color

data class Suggestion(
    val id: String,
    val name: String,
    val color: Color
)

val sampleSuggestions: List<Suggestion> = listOf(
    Suggestion("s1", "Аптека", Color(0xFF4F8579)),
    Suggestion("s2", "Подарки на новый год", Color(0xFFDDA68B)),
    Suggestion("s3", "Продукты", Color(0xFF5B7178)),
    Suggestion("s4", "Работа", Color(0xFF6B6B8E)),
    Suggestion("s5", "Путешествие", Color(0xFF4A7A8A)),
    Suggestion("s6", "День рождения", Color(0xFF8E6B7A)),
)
