package com.lena.kartoshka.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.abs

@Entity(tableName = "loyalty_cards")
data class LoyaltyCard(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "barcode_value") val barcodeValue: String,
    @ColumnInfo(name = "barcode_format") val barcodeFormat: Int,
    val color: Long
)

private val cardColorPalette = listOf(
    0xFFE53935L, 0xFFE65100L, 0xFF1565C0L, 0xFF2E7D32L,
    0xFF6A1B9AL, 0xFF00838FL, 0xFF558B2FL, 0xFFC62828L,
    0xFF283593L, 0xFF00695CL
)

fun generateLoyaltyCardColor(name: String): Long =
    cardColorPalette[abs(name.hashCode()) % cardColorPalette.size]
