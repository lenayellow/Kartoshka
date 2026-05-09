package com.lena.kartoshka.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color

data class Ingredient(
    val name: String,
    val amount: String,
    val isProbablyOwned: Boolean = false
)

data class Recipe(
    val id: String,
    val title: String,
    val author: String,
    val tagline: String,
    val coverColor: Color,
    val likes: Int,
    val ingredients: List<Ingredient>,
    val isOwn: Boolean = false
)

val sampleRecipes = mutableStateListOf(
    Recipe(
        id = "r1",
        title = "Паста Карбонара",
        author = "Chef Mario",
        tagline = "Классика итальянской кухни",
        coverColor = Color(0xFFDDA68B),
        likes = 247,
        ingredients = listOf(
            Ingredient("Спагетти", "400 г"),
            Ingredient("Бекон", "200 г"),
            Ingredient("Яйца", "4 шт"),
            Ingredient("Пармезан", "100 г"),
            Ingredient("Чёрный перец", "по вкусу"),
            Ingredient("Чеснок", "2 зубчика"),
            Ingredient("Соль", "1 ч.л.", isProbablyOwned = true),
            Ingredient("Оливковое масло", "2 ст.л.", isProbablyOwned = true)
        )
    ),
    Recipe(
        id = "r2",
        title = "Греческий салат",
        author = "Mediterranean Kitchen",
        tagline = "Свежо и легко",
        coverColor = Color(0xFF4F8579),
        likes = 183,
        ingredients = listOf(
            Ingredient("Помидоры", "3 шт"),
            Ingredient("Огурцы", "2 шт"),
            Ingredient("Болгарский перец", "1 шт"),
            Ingredient("Сыр фета", "200 г"),
            Ingredient("Оливки", "100 г"),
            Ingredient("Красный лук", "1 шт"),
            Ingredient("Орегано", "1 ч.л."),
            Ingredient("Оливковое масло", "3 ст.л.", isProbablyOwned = true),
            Ingredient("Соль", "по вкусу", isProbablyOwned = true)
        )
    ),
    Recipe(
        id = "r3",
        title = "Тыквенный суп-пюре",
        author = "Autumn Flavors",
        tagline = "Согреет в любую погоду",
        coverColor = Color(0xFF7B6B8A),
        likes = 312,
        ingredients = listOf(
            Ingredient("Тыква", "1 кг"),
            Ingredient("Морковь", "2 шт"),
            Ingredient("Лук", "1 шт"),
            Ingredient("Чеснок", "3 зубчика"),
            Ingredient("Сливки", "200 мл"),
            Ingredient("Имбирь", "1 ч.л."),
            Ingredient("Растительное масло", "2 ст.л.", isProbablyOwned = true),
            Ingredient("Соль", "по вкусу", isProbablyOwned = true)
        )
    ),
    Recipe(
        id = "r4",
        title = "Шоколадный брауни",
        author = "Sweet Lab",
        tagline = "Хрустящий снаружи, нежный внутри",
        coverColor = Color(0xFF3D5A4E),
        likes = 429,
        ingredients = listOf(
            Ingredient("Тёмный шоколад", "200 г"),
            Ingredient("Сливочное масло", "150 г"),
            Ingredient("Яйца", "3 шт"),
            Ingredient("Сахар", "150 г"),
            Ingredient("Мука", "80 г"),
            Ingredient("Какао-порошок", "2 ст.л."),
            Ingredient("Грецкие орехи", "100 г"),
            Ingredient("Соль", "щепотка", isProbablyOwned = true)
        )
    )
)
