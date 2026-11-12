package com.suyziss.gallery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class GalleryLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun galleryLaunchesAndShowsItsTitle() {
        composeRule.onNodeWithText("Suyziss Gallery").assertIsDisplayed()
    }

    @Test
    fun permissionCallToActionIsAvailableBeforeAccessIsGranted() {
        composeRule.onNodeWithText("Разрешите доступ к медиатеке").assertExists()
    }
}
