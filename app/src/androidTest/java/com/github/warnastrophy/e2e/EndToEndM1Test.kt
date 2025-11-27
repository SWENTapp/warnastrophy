package com.github.warnastrophy.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.IdlingRegistry
import com.github.warnastrophy.core.data.provider.HealthCardRepositoryProvider
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.data.service.DangerModeService
import com.github.warnastrophy.core.data.service.GeocodeService
import com.github.warnastrophy.core.data.service.MockNominatimService
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.data.service.StateManagerService.dangerModeService
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.features.map.MapViewModel
import com.github.warnastrophy.core.ui.map.GpsServiceMock
import com.github.warnastrophy.core.ui.map.HazardServiceMock
import com.github.warnastrophy.core.ui.map.MockPermissionManager
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import com.github.warnastrophy.core.util.AnimationIdlingResource
import com.google.android.gms.maps.MapsInitializer
import org.junit.After
import org.junit.Before
import org.junit.Test

class EndToEndM1Test : EndToEndUtils() {
  private lateinit var gpsService: GpsServiceMock
  private lateinit var hazardService: HazardServiceMock
  private lateinit var permissionManager: MockPermissionManager
  private lateinit var viewModel: MapViewModel
  private lateinit var nominatimRepository: GeocodeService
  private val animationIdlingResource = AnimationIdlingResource()

  @Before
  override fun setUp() {
    super.setUp()
    gpsService = GpsServiceMock()
    hazardService = HazardServiceMock()
    permissionManager = MockPermissionManager()
    nominatimRepository = MockNominatimService()
    StateManagerService.init(composeTestRule.activity.applicationContext)
    StateManagerService.permissionManager =
        MockPermissionManager(currentResult = PermissionResult.Granted)
    dangerModeService = DangerModeService(permissionManager = StateManagerService.permissionManager)

    viewModel = MapViewModel(gpsService, hazardService, permissionManager, nominatimRepository)
    IdlingRegistry.getInstance().register(animationIdlingResource)

    val context = composeTestRule.activity.applicationContext
    MapsInitializer.initialize(context)

    ContactRepositoryProvider.resetForTests()
    ContactRepositoryProvider.initLocal(context)
    repository = ContactRepositoryProvider.repository
    HealthCardRepositoryProvider.useLocalEncrypted(context)
    StateManagerService.initForTests(
        gpsService = gpsService,
        hazardsService = hazardService,
        permissionManager = permissionManager,
        dangerModeService = dangerModeService,
    )
  }

  @After
  override fun tearDown() {
    super.tearDown()
    ContactRepositoryProvider.resetForTests()
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
}
