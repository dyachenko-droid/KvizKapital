package ru.kvizcap.quiz.app.core

import ru.kvizcap.quiz.app.R

data class TileArt(val res: Int, val aspect: Float, val anchor: Float)

object ArtMeta {
    val tiles: Map<String, TileArt> = mapOf(
        "b_bakery_1" to TileArt(R.drawable.b_bakery_1, 0.8000f, 0.6458f),
        "b_bakery_2" to TileArt(R.drawable.b_bakery_2, 0.7619f, 0.6531f),
        "b_bakery_3" to TileArt(R.drawable.b_bakery_3, 1.0952f, 0.7565f),
        "b_bakery_4" to TileArt(R.drawable.b_bakery_4, 0.9381f, 0.6675f),
        "b_bakery_5" to TileArt(R.drawable.b_bakery_5, 1.1905f, 0.7640f),
        "b_bank_1" to TileArt(R.drawable.b_bank_1, 0.7452f, 0.5751f),
        "b_bank_2" to TileArt(R.drawable.b_bank_2, 0.6452f, 0.5756f),
        "b_bank_3" to TileArt(R.drawable.b_bank_3, 0.8119f, 0.7038f),
        "b_bank_4" to TileArt(R.drawable.b_bank_4, 0.8762f, 0.6630f),
        "b_bank_5" to TileArt(R.drawable.b_bank_5, 0.8857f, 0.7312f),
        "b_books_1" to TileArt(R.drawable.b_books_1, 0.8643f, 0.6309f),
        "b_books_2" to TileArt(R.drawable.b_books_2, 0.7786f, 0.6514f),
        "b_books_3" to TileArt(R.drawable.b_books_3, 0.9810f, 0.7160f),
        "b_books_4" to TileArt(R.drawable.b_books_4, 0.9881f, 0.6554f),
        "b_books_5" to TileArt(R.drawable.b_books_5, 1.0667f, 0.7277f),
        "b_coffee_1" to TileArt(R.drawable.b_coffee_1, 0.7524f, 0.6234f),
        "b_coffee_2" to TileArt(R.drawable.b_coffee_2, 0.8524f, 0.6453f),
        "b_coffee_3" to TileArt(R.drawable.b_coffee_3, 0.9119f, 0.6997f),
        "b_coffee_4" to TileArt(R.drawable.b_coffee_4, 0.9190f, 0.6813f),
        "b_coffee_5" to TileArt(R.drawable.b_coffee_5, 0.9119f, 0.6997f),
        "b_garage_1" to TileArt(R.drawable.b_garage_1, 0.7357f, 0.6117f),
        "b_garage_2" to TileArt(R.drawable.b_garage_2, 0.7310f, 0.6026f),
        "b_garage_3" to TileArt(R.drawable.b_garage_3, 0.7714f, 0.6667f),
        "b_garage_4" to TileArt(R.drawable.b_garage_4, 0.7881f, 0.6254f),
        "b_garage_5" to TileArt(R.drawable.b_garage_5, 0.8238f, 0.6590f),
        "b_hotel_1" to TileArt(R.drawable.b_hotel_1, 0.6714f, 0.5993f),
        "b_hotel_2" to TileArt(R.drawable.b_hotel_2, 0.8190f, 0.6977f),
        "b_hotel_3" to TileArt(R.drawable.b_hotel_3, 0.9881f, 0.7976f),
        "b_hotel_4" to TileArt(R.drawable.b_hotel_4, 0.9071f, 0.6535f),
        "b_hotel_5" to TileArt(R.drawable.b_hotel_5, 0.9167f, 0.6208f),
        "b_office_1" to TileArt(R.drawable.b_office_1, 0.5524f, 0.5560f),
        "b_office_2" to TileArt(R.drawable.b_office_2, 0.6810f, 0.5490f),
        "b_office_3" to TileArt(R.drawable.b_office_3, 0.8119f, 0.6657f),
        "b_office_4" to TileArt(R.drawable.b_office_4, 1.0214f, 0.7552f),
        "b_office_5" to TileArt(R.drawable.b_office_5, 1.2690f, 0.8124f),
        "b_tech_1" to TileArt(R.drawable.b_tech_1, 0.6905f, 0.5897f),
        "b_tech_2" to TileArt(R.drawable.b_tech_2, 0.6429f, 0.4296f),
        "b_tech_3" to TileArt(R.drawable.b_tech_3, 0.6643f, 0.5269f),
        "b_tech_4" to TileArt(R.drawable.b_tech_4, 0.7905f, 0.5753f),
        "b_tech_5" to TileArt(R.drawable.b_tech_5, 0.8214f, 0.6870f),
        "d_fountain" to TileArt(R.drawable.d_fountain, 0.8524f, 0.4274f),
        "d_lamps" to TileArt(R.drawable.d_lamps, 0.9881f, 0.6289f),
        "d_park" to TileArt(R.drawable.d_park, 0.8571f, 0.5194f),
        "d_plaza" to TileArt(R.drawable.d_plaza, 0.8762f, 0.5870f),
        "d_plot_empty" to TileArt(R.drawable.d_plot_empty, 0.7333f, 0.4383f),
        "d_plot_locked" to TileArt(R.drawable.d_plot_locked, 0.7333f, 0.4675f),
        "d_tree" to TileArt(R.drawable.d_tree, 1.1905f, 0.6840f)
    )

    fun tile(name: String): TileArt = tiles.getValue(name)
}
