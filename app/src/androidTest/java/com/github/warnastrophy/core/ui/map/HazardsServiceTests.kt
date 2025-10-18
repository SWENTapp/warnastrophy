package com.github.warnastrophy.core.ui.map

import com.github.warnastrophy.core.data.repository.HazardsDataSource
import com.github.warnastrophy.core.model.GpsPositionState
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.HazardsService
import com.github.warnastrophy.core.model.PositionService
import com.github.warnastrophy.core.util.AppConfig
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class HazardsServiceTests {

  // Petit stub pour exposer la structure attendue par HazardsService
  private data class TestPositionHolder(val position: LatLng)

  private class TestGpsService(initial: LatLng) : PositionService {
    override val positionState = MutableStateFlow(GpsPositionState(position = initial))
  }

  private class HazardProviderTest() : HazardsDataSource {
    override suspend fun getAreaHazards(geometry: String, days: String): List<Hazard> {
      return listOf(Hazard(null, null, null, null, null, null, null, null, null))
    }
  }

  private lateinit var gps: TestGpsService

  private lateinit var hazardProvider: HazardsDataSource

  @Before
  fun setUp() {
    gps = TestGpsService(LatLng(48.8146, 2.3486))
    hazardProvider = HazardProviderTest()
    // réduire les délais pour accélérer les tests
    AppConfig.fetchDelayMs = 200L
  }

  @After
  fun tearDown() {
    // nothing here; chaque test doit fermer explicitement le service créé
  }

  @Test
  fun testHazardsLoadedIntoService() = runBlocking {
    // Préparer le mock pour retourner une liste non vide dès le premier appel

    val service = HazardsService(hazardProvider, gps)

    // attendre un peu pour que le coroutine d'init ait le temps d'appeler le repo
    delay(500)

    val hazards = service.currentHazardsState.value
    Assert.assertNotNull(hazards)
    Assert.assertTrue(hazards.isNotEmpty())

    service.close()
  }
}
