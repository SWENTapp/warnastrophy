package com.github.warnastrophy.core.ui.map

import com.github.warnastrophy.core.data.service.RefreshHazardsIfMovedService
import com.github.warnastrophy.core.model.Location
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RefreshHazardsIfMovedUseCaseTest {
  private val distanceThreshold = 5.0
  private lateinit var hazardsService: HazardServiceMock
  private lateinit var useCase: RefreshHazardsIfMovedService

  private val initialLocation = Location(51.5074, 0.1278) // London
  private val farAwayLocation = Location(51.6000, 0.2000)

  private var stubbedDistanceResult: Double = 0.0

  private val distanceCalculatorStub: (Location, Location) -> Double = { _, _ ->
    stubbedDistanceResult
  }

  @Before
  fun setup() {
    hazardsService = HazardServiceMock()
    useCase = RefreshHazardsIfMovedService(hazardsService, distanceThreshold, distanceCalculatorStub)
  }

  @Test
  fun shouldFetchHazardsReturnTrueAtBeginning() {
    val result = useCase.shouldFetchHazards(initialLocation)

    assertTrue("Should fetch hazards when location has not been set yet.", result)
  }

  @Test
  fun shouldFetchHazardsReturnsFalseIfDistanceIsThreshold() {
    useCase.execute(initialLocation)
    hazardsService.assertFetchCalled(1)

    stubbedDistanceResult = distanceThreshold

    val result = useCase.shouldFetchHazards(farAwayLocation)

    assertFalse(
        "Should not fetch if distance is exactly the threshold (5.0 > 5.0 is false)", result)
  }

  @Test
  fun testShouldFetchHazardsReturnsFalseWhenMovementIsLessThanDistanceThresholdKm() {
    useCase.execute(initialLocation)

    stubbedDistanceResult = distanceThreshold - 0.01

    val result = useCase.shouldFetchHazards(farAwayLocation)

    assertFalse("Should not fetch if movement is within the threshold", result)
  }

  @Test
  fun testShouldFetchHazardsReturnsTrueWhenMovementIsGreaterThanDistanceThresholdKm() {
    useCase.execute(initialLocation)

    stubbedDistanceResult = distanceThreshold + 0.01

    val result = useCase.shouldFetchHazards(farAwayLocation)

    assertTrue("Should fetch if movement exceeds the threshold", result)
  }

  @Test
  fun testExecuteTriggersFetchAndUpdatesLastFetchLocationOnFirstCall() {
    // Given lastFetchLocation is null
    // When
    useCase.execute(initialLocation)

    hazardsService.assertFetchCalled(1)

    stubbedDistanceResult = 0.0
    useCase.execute(initialLocation)

    hazardsService.assertFetchCalled(1)
  }

  @Test
  fun testExecuteDoesNotTriggerFetchIfDistanceIsWithinTheThreshold() {
    useCase.execute(initialLocation)
    hazardsService.assertFetchCalled(1)

    stubbedDistanceResult = distanceThreshold - 1.0

    useCase.execute(farAwayLocation)

    hazardsService.assertFetchCalled(1)
  }

  @Test
  fun testExecuteTriggersFetchAndUpdatesLastFetchLocationIfDistanceExceedsThreshold() {
    useCase.execute(initialLocation)
    hazardsService.assertFetchCalled(1)

    stubbedDistanceResult = distanceThreshold + 1.0

    useCase.execute(farAwayLocation)

    hazardsService.assertFetchCalled(2)

    stubbedDistanceResult = 0.1

    val subsequentLocation = Location(10.0, 10.0)
    useCase.execute(subsequentLocation)

    hazardsService.assertFetchCalled(2)
  }
}
