// This file is made by Dao Nguyen Ninh and AI assistant Gemini
package com.github.warnastrophy.core.ui.dashboard

import com.github.warnastrophy.core.domain.model.FetcherState
import com.github.warnastrophy.core.domain.model.HazardsDataService
import com.github.warnastrophy.core.ui.features.dashboard.LatestNewsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LatestNewsViewModelTest {
  private lateinit var mockHazardsService: HazardsDataService

  private lateinit var mockFetcherStateFlow: MutableStateFlow<FetcherState>

  private lateinit var viewModel: LatestNewsViewModel

  @Before
  fun setup() {
    mockFetcherStateFlow = MutableStateFlow(FetcherState(emptyList()))
    mockHazardsService = mockk(relaxed = true)
    every { mockHazardsService.fetcherState } returns mockFetcherStateFlow
    viewModel = LatestNewsViewModel(mockHazardsService)
  }

  @Test
  fun `initial fetcherState should mirror the service's initial state`() = runTest {
    assertEquals(true, viewModel.fetcherState.value.hazards.isEmpty())
  }

  @Test
  fun `fetcherState should reflect Loading state from the service`() = runTest {
    mockFetcherStateFlow.value = FetcherState(emptyList(), isLoading = true)
    assertEquals(mockFetcherStateFlow.value.isLoading, viewModel.fetcherState.value.isLoading)
  }
}
