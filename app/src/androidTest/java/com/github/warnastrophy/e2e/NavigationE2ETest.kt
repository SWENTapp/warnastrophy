package com.github.warnastrophy.e2e

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.WarnastrophyApp
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.ui.features.contact.AddContactTestTags
import com.github.warnastrophy.core.ui.features.contact.ContactListScreenTestTags
import com.github.warnastrophy.core.ui.features.contact.EditContactTestTags
import com.github.warnastrophy.core.ui.features.contact.UITest
import com.github.warnastrophy.core.ui.features.map.MapScreenTestTags
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import org.junit.Before
import org.junit.Test

class NavigationE2ETest : UITest() {

  @Before
  override fun setUp() {
    super.setUp()
    val context = composeTestRule.activity.applicationContext
    ContactRepositoryProvider.init(context)
    repository = ContactRepositoryProvider.repository
  }

  @Test
  fun testTagsAreCorrectlySet() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_DASHBOARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_MAP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).assertIsDisplayed()
  }

  @Test
  fun startsOnDashboard_bottomNavVisible() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Dashboard", ignoreCase = true)
  }

  @Test
  fun navigate_Dashboard_to_Map_and_back() {
    setContent()
    composeTestRule
        .onNode(
            hasClickAction().and(hasTestTag(NavigationTestTags.TAB_MAP)), useUnmergedTree = true)
        .assertExists()
        .assertIsEnabled()
        .performClick()

    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Map", ignoreCase = true)

    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_DASHBOARD).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Dashboard", ignoreCase = true)
  }

  @Test
  fun navigate_to_Profile_then_back_to_Dashboard() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Profile", ignoreCase = true)

    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_DASHBOARD).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Dashboard", ignoreCase = true)
  }

  @Test
  fun can_visit_all_tabs_in_sequence() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_MAP).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_DASHBOARD).performClick()

    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Dashboard", ignoreCase = true)
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV).assertIsDisplayed()
  }

  @Test
  fun navigate_to_contact_list_and_back_to_Dashboard() {
    setContent()
    // Go to Profile
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Profile", ignoreCase = true)
    // Go to contact list
    composeTestRule.onNodeWithTag(NavigationTestTags.CONTACT_LIST).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Contact List", ignoreCase = true)
    // Back to Profile
    composeTestRule.onNodeWithTag(NavigationTestTags.BUTTON_BACK).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Profile", ignoreCase = true)
    // Go to Dashboard
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_DASHBOARD).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Dashboard", ignoreCase = true)
  }

  private fun fillContactFormToAdd() {
    composeTestRule.enterAddFullName("Messi")
    composeTestRule.enterAddRelationship("Friend")
    composeTestRule.enterAddPhoneNumber("+41765365899")
  }

  private fun fillContactFormToEdit() {
    composeTestRule.enterEditFullName("Ronaldo")
    composeTestRule.enterEditRelationship("Player")
    composeTestRule.enterEditPhoneNumber("+41725315831")
  }

  @Test
  fun create_edit_and_delete_contact() {
    setContent()
    // Add a new contact
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.CONTACT_LIST).performClick()
    composeTestRule.onNodeWithTag(ContactListScreenTestTags.ADD_CONTACT_BUTTON).performClick()
    fillContactFormToAdd()
    composeTestRule.clickOnSaveContact(true, AddContactTestTags.SAVE_BUTTON)
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Contact List", ignoreCase = true)
    // Click on existing contact, enter text, but not save it
    composeTestRule.onNodeWithText("Messi", ignoreCase = true).assertIsDisplayed().performClick()
    fillContactFormToEdit()
    composeTestRule.onNodeWithTag(NavigationTestTags.BUTTON_BACK).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Contact List", ignoreCase = true)
    composeTestRule.onNodeWithText("Messi", ignoreCase = true).assertIsDisplayed()
    // Click on existing contact and edit it
    composeTestRule.onNodeWithText("Messi", ignoreCase = true).assertIsDisplayed().performClick()
    fillContactFormToEdit()
    composeTestRule.clickOnSaveContact(true, EditContactTestTags.SAVE_BUTTON)
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Contact List", ignoreCase = true)
    // Delete contact
    composeTestRule.onNodeWithText("Ronaldo", ignoreCase = true).assertIsDisplayed().performClick()
    composeTestRule
        .onNodeWithTag(EditContactTestTags.DELETE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitUntil(defaultTimeout) {
      composeTestRule
          .onAllNodesWithTag(EditContactTestTags.DELETE_BUTTON)
          .fetchSemanticsNodes()
          .isEmpty()
    }
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Contact List", ignoreCase = true)
  }

  private fun setContent(useFakeMap: Boolean = true) {
    if (useFakeMap) {
      composeTestRule.setContent { WarnastrophyApp(mockMapScreen = { FakeMapComponent() }) }
    } else {
      composeTestRule.setContent { WarnastrophyApp() }
    }
  }

  @Composable
  fun FakeMapComponent() {
    Box(Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)) {
      Text("Fake map for testing", Modifier.align(Alignment.Center))
    }
  }
}
