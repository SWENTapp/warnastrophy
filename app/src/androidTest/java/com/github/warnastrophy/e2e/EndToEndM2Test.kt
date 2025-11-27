package com.github.warnastrophy.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.IdlingRegistry
import com.github.warnastrophy.core.data.provider.HealthCardRepositoryProvider
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.data.service.DangerModeService
import com.github.warnastrophy.core.data.service.GeocodeService
import com.github.warnastrophy.core.data.service.MockNominatimService
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.data.service.StateManagerService.dangerModeService
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.features.dashboard.DashboardScreenTestTags
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

class EndToEndM2Test : EndToEndUtils() {

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

    // Check Dashboard tags
    composeTestRule.onNodeWithTag(DashboardScreenTestTags.ROOT_SCROLL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DashboardScreenTestTags.LATEST_NEWS_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DashboardScreenTestTags.MAP_PREVIEW_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DashboardScreenTestTags.ROW_TWO_SMALL_CARDS).assertIsDisplayed()
  }

  @Test
  fun create_edit_and_delete_contact() {
    setContent()
    addNewContact()
    editContact(saveChanges = false)
    editContact(saveChanges = true)
    deleteContact()
  }
}
