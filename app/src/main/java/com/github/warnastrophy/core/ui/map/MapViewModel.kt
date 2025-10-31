package com.github.warnastrophy.core.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapUIState(
    val isLoading: Boolean = true,
    val userLocation: LatLng? = null,
    val permissionGranted: Boolean = false,
)

/**
 * ViewModel for [MapScreen].
 *
 * Currently uses static data. Will be updated to use data from a repository in the future.
 */
class MapViewModel() : ViewModel() {
  private val _uiState = MutableStateFlow(MapUIState())

  /** The UI state as a read-only [StateFlow]. */
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch(Dispatchers.IO) {}
  }
}
