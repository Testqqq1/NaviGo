package com.example.navigo.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.navigo.data.Place
import com.example.navigo.data.PlaceCategory
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun favoritesTab_displaysFavoriteItems() {
        val testFavorites = listOf(
            Place(
                id = "t1",
                name = "Test Favorite Landmark",
                address = "123 Bangkok",
                latitude = 13.75,
                longitude = 100.5,
                category = PlaceCategory.LANDMARK,
                isFavorite = true
            )
        )

        composeTestRule.setContent {
            FavoritesTabContent(
                favorites = testFavorites,
                onSelectPlace = {},
                onToggleFavorite = {}
            )
        }

        composeTestRule.onNodeWithText("Test Favorite Landmark").assertExists()
        composeTestRule.onNodeWithText("สถานที่โปรดของคุณ").assertExists()
    }
}
