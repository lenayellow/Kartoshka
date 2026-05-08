package com.lena.kartoshka.data

data class Item(
    val id: String,
    val name: String,
    val tags: Set<ItemTag> = emptySet(),
    val note: String = ""
)

val sampleItemsByList: Map<String, List<Item>> = mapOf(
    "1" to listOf(
        Item("1_1", "Молоко")
    ),
    "2" to listOf(
        Item("2_1", "Хлеб"),
        Item("2_2", "Масло"),
        Item("2_3", "Яйца"),
        Item("2_4", "Сыр"),
        Item("2_5", "Йогурт"),
        Item("2_6", "Апельсины")
    ),
    "3" to listOf(
        Item("3_1", "Уголь"),
        Item("3_2", "Дрова"),
        Item("3_3", "Шашлык"),
        Item("3_4", "Овощи"),
        Item("3_5", "Кетчуп"),
        Item("3_6", "Пиво"),
        Item("3_7", "Вода")
    ),
    "4" to listOf(
        Item("4_1", "Кофе"),
        Item("4_2", "Чай"),
        Item("4_3", "Печенье"),
        Item("4_4", "Фрукты"),
        Item("4_5", "Салфетки"),
        Item("4_6", "Зубная паста"),
        Item("4_7", "Шампунь")
    ),
    "5" to listOf(
        Item("5_1", "Рамки"),
        Item("5_2", "Ящик"),
        Item("5_3", "Подушки"),
        Item("5_4", "Лампа"),
        Item("5_5", "Коврик"),
        Item("5_6", "Горшки"),
        Item("5_7", "Зеркало"),
        Item("5_8", "Вешалка"),
        Item("5_9", "Органайзер"),
        Item("5_10", "Бокалы"),
        Item("5_11", "Тарелки"),
        Item("5_12", "Полотенца")
    )
)
