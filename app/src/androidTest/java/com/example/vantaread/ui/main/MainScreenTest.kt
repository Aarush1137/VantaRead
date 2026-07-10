package com.example.vantaread.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.vantaread.data.model.Novel
import com.example.vantaread.ui.discover.SearchResultItem
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI smoke tests for current Compose surfaces. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent {
      SearchResultItem(
        novel = Novel(
          url = "https://www.royalroad.com/fiction/21220/mother-of-learning",
          title = "Mother of Learning",
          coverUrl = ""
        ),
        onClick = {}
      )
    }
  }

  @Test
  fun firstItem_exists() {
    composeTestRule.onNodeWithText("Mother of Learning").assertExists()
  }
}
