package com.github.warnastrophy.core.ui.mapPreviewCard

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.github.warnastrophy.WarnastrophyComposable
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.ui.features.dashboard.MapPreviewCard
import com.github.warnastrophy.core.ui.features.dashboard.MapPreviewTestTags
import com.github.warnastrophy.core.ui.features.map.MapScreen
import com.github.warnastrophy.core.ui.features.map.MapScreenTestTags
import com.github.warnastrophy.core.ui.features.map.MapViewModel
import com.github.warnastrophy.core.ui.map.GpsServiceMock
import com.github.warnastrophy.core.ui.map.HazardServiceMock
import com.github.warnastrophy.core.ui.map.MockPermissionManager
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import com.github.warnastrophy.core.util.AppConfig
import com.github.warnastrophy.core.util.AppConfig.defaultPosition
import com.google.android.gms.maps.MapsInitializer
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapPreviewCardTest : BaseAndroidComposeTest() {
  private lateinit var gpsService: GpsServiceMock
  private lateinit var hazardService: HazardServiceMock
  private lateinit var permissionManager: MockPermissionManager
  private lateinit var viewModel: MapViewModel

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  @Before
  override fun setUp() {
    super.setUp()
    setupMockFirebaseAuth()
    gpsService = GpsServiceMock()
    hazardService = HazardServiceMock()
    permissionManager = MockPermissionManager()
    val context = ApplicationProvider.getApplicationContext<Context>()
    MapsInitializer.initialize(context)
    viewModel = MapViewModel(gpsService, hazardService, permissionManager)
  }

  /**
   * Sets up a mocked Firebase authentication with a logged-in user. This ensures the app starts at
   * the Dashboard instead of the SignIn screen.
   */
  private fun setupMockFirebaseAuth() {
    // Mock FirebaseApp
    mockkStatic(FirebaseApp::class)
    val mockFirebaseApp: FirebaseApp = mockk(relaxed = true)
    every { FirebaseApp.getInstance() } returns mockFirebaseApp
    every { FirebaseApp.getApps(any()) } returns listOf(mockFirebaseApp)

    // Mock FirebaseAuth with an authenticated user
    mockkStatic(FirebaseAuth::class)
    val mockFirebaseAuth: FirebaseAuth = mockk(relaxed = true)
    val mockFirebaseUser: FirebaseUser =
        mockk(relaxed = true) {
          every { uid } returns "test-user-id"
          every { email } returns "test@example.com"
          every { displayName } returns "Test User"
          every { isAnonymous } returns false
        }

    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
  }

  @After
  override fun tearDown() {
    super.tearDown()

    unmockkStatic(FirebaseAuth::class)
    unmockkStatic(FirebaseApp::class)
    // Clear shared preferences
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val prefs = ctx.getSharedPreferences(AppConfig.PREF_FILE_NAME, Context.MODE_PRIVATE)
    prefs.edit().clear().apply()

    // Unregister idling resources
    IdlingRegistry.getInstance().resources.forEach { IdlingRegistry.getInstance().unregister(it) }
  }

  @Test
  fun showsPlaceholder_when_mapContentIsNull() {
    composeTestRule.setContent { MapPreviewCard(mapContent = null) }

    composeTestRule.onNodeWithTag(MapPreviewTestTags.PLACEHOLDER).assertExists().isDisplayed()
  }

  @Test
  fun showsMapContent_when_mapContentProvided() {
    // Initialize Contact Repository (really important for first time app launch during tests)
    ContactRepositoryProvider.init(ApplicationProvider.getApplicationContext())
    composeTestRule.setContent {
      WarnastrophyComposable(mockMapScreen = { MapScreen(viewModel = viewModel) })
    }

    composeTestRule.waitUntil(
        condition = { composeTestRule.onNodeWithTag(MapPreviewTestTags.MAP_CONTENT).isDisplayed() },
        timeoutMillis = 5_000)

    composeTestRule.onNodeWithTag(MapPreviewTestTags.MAP_CONTENT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_MAP).performClick()

    composeTestRule.waitUntil(
        condition = {
          composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).isDisplayed()
        },
        timeoutMillis = 5_000)
    composeTestRule.onNodeWithTag(MapScreenTestTags.TRACK_LOCATION_BUTTON).performClick()
    composeTestRule.waitUntil(
        condition = { gpsService.positionState.value.position != defaultPosition },
        timeoutMillis = 5_000)

    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_DASHBOARD).performClick()
    composeTestRule.waitUntil(
        condition = {
          composeTestRule.onNodeWithTag(MapScreenTestTags.TRACK_LOCATION_BUTTON).isDisplayed()
        },
        timeoutMillis = 5_000)

    composeTestRule.onNodeWithTag(MapPreviewTestTags.MAP_CONTENT).isDisplayed()
  }
}
