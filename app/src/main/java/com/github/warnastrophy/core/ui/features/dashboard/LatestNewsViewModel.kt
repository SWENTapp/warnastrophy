package com.github.warnastrophy.core.ui.features.dashboard

import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.data.service.FetcherState
import com.github.warnastrophy.core.data.service.HazardsDataService
import kotlinx.coroutines.flow.StateFlow

/** ViewModel responsible for providing the latest news and hazard data state to the UI. */
class LatestNewsViewModel(hazardsService: HazardsDataService) : ViewModel() {
  /** Exposes the fetcherState flow from the service for the UI to collect */
  val fetcherState: StateFlow<FetcherState> = hazardsService.fetcherState
}
