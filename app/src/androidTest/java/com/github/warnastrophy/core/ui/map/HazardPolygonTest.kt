package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.rule.GrantPermissionRule
import com.github.warnastrophy.core.domain.model.Hazard
import com.github.warnastrophy.core.domain.model.MockNominatimService
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.features.map.MapScreen
import com.github.warnastrophy.core.ui.features.map.MapScreenTestTags
import com.github.warnastrophy.core.ui.features.map.MapViewModel
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import com.github.warnastrophy.core.util.AnimationIdlingResource
import com.github.warnastrophy.core.util.AppConfig.defaultPosition
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry

@OptIn(ExperimentalCoroutinesApi::class)
class HazardPolygonTest : BaseAndroidComposeTest() {
  private lateinit var gpsService: GpsServiceMock
  private lateinit var hazardService: HazardServiceMock
  private lateinit var permissionManager: MockPermissionManager
  private lateinit var viewModel: MapViewModel
  /**
   * An idling resource to wait for camera animations to complete during tests. This is crucial for
   * Espresso tests involving map camera movements, as it prevents test actions from executing
   * prematurely while the map is still animating.
   */
  private val animationIdlingResource = AnimationIdlingResource()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  @Before
  override fun setUp() {
    super.setUp()

    gpsService = GpsServiceMock()
    hazardService = HazardServiceMock()

    val coordsA =
        arrayOf(
            Coordinate(9.9, 9.9),
            Coordinate(9.9, 10.1),
            Coordinate(10.1, 10.1),
            Coordinate(10.1, 9.9),
            Coordinate(9.9, 9.9) // Must close the ring!
            )
    val geometryA: Geometry = factory.createPolygon(factory.createLinearRing(coordsA))

    hazardService.setHazards(
        listOf(
            Hazard(
                id = 1001,
                alertLevel = 3.0,
                type = "TC",
                description = "Test Hazard A",
                country = "Testland",
                date = "2025-01-01",
                severity = 2.0,
                severityUnit = "unit",
                articleUrl = "http://example.test/A",
                centroid = Coordinate(10.0, 10.0).let { point -> factory.createPoint(point) },
                bbox = listOf(9.9, 9.9, 10.1, 10.1),
                affectedZone = geometryA)))

    permissionManager = MockPermissionManager()
    val context = ApplicationProvider.getApplicationContext<Context>()
    MapsInitializer.initialize(context)

    viewModel = MapViewModel(gpsService, hazardService, permissionManager, MockNominatimService())
    IdlingRegistry.getInstance().register(animationIdlingResource)
  }

  @After
  override fun tearDown() {
    super.tearDown()

    // Clear shared preferences
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()

    // Unregister idling resources
    IdlingRegistry.getInstance().resources.forEach { IdlingRegistry.getInstance().unregister(it) }
  }

  /** Applies a given [PermissionResult] to the view model. */
  private fun applyPerm(permissionResult: PermissionResult) {
    // Use mock permission manager to simulate different permission states deterministically
    permissionManager.setPermissionResult(permissionResult)
    viewModel.applyPermissionsResult(composeTestRule.activity)
  }

  @Test
  fun testHazardPolygonRendering() {
    lateinit var cameraPositionState: CameraPositionState

    composeTestRule.setContent {
      cameraPositionState = rememberCameraPositionState()
      cameraPositionState.position =
          CameraPosition.fromLatLngZoom(
              LatLng(defaultPosition.latitude + 1, defaultPosition.longitude + 1), 1f)
      MapScreen(viewModel = viewModel, cameraPositionState = cameraPositionState)
      applyPerm(PermissionResult.Granted)
    }

    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).isDisplayed()
    assert(!hazardService.fetcherState.value.hazards.first().affectedZone?.isEmpty!!)
  }
}
