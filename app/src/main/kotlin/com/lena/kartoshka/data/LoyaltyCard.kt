package com.lena.kartoshka.data

import androidx.compose.ui.graphics.Color

data class LoyaltyCard(
    val id: String,
    val storeName: String,
    val color: Color
)

val sampleLoyaltyCards: List<LoyaltyCard> = listOf(
    LoyaltyCard("lc1", "Coop", Color(0xFFE53935)),
    LoyaltyCard("lc2", "Migros", Color(0xFFE65100)),
    LoyaltyCard("lc3", "IKEA", Color(0xFF1565C0)),
)
