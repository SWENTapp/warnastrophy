package com.github.warnastrophy.core.ui.features.dashboard

import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.domain.model.FetcherState
import com.github.warnastrophy.core.domain.model.HazardsDataService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** ViewModel responsible for providing the latest news and hazard data state to the UI. */
@HiltViewModel
class LatestNewsViewModel @Inject constructor(private val hazardsService: HazardsDataService) :
    ViewModel() {
  /** Exposes the fetcherState flow from the service for the UI to collect */
  val fetcherState: StateFlow<FetcherState> = hazardsService.fetcherState
}
