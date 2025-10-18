// package com.github.warnastrophy.core.ui.map
//
// import androidx.lifecycle.ViewModel
// import androidx.lifecycle.viewModelScope
// import com.github.warnastrophy.core.model.GpsPositionState
// import com.github.warnastrophy.core.model.GpsService
// import com.github.warnastrophy.core.model.Hazard
// import com.github.warnastrophy.core.model.HazardsService
// import com.github.warnastrophy.core.util.AppConfig
// import com.google.android.gms.maps.model.LatLng
// import kotlinx.coroutines.flow.MutableStateFlow
// import kotlinx.coroutines.flow.StateFlow
// import kotlinx.coroutines.flow.asStateFlow
// import kotlinx.coroutines.flow.collect
// import kotlinx.coroutines.flow.combine
// import kotlinx.coroutines.flow.update
// import kotlinx.coroutines.isActive
// import kotlinx.coroutines.launch
// import kotlinx.coroutines.delay
//
// data class MapUiState(
//    val position: LatLng = AppConfig.defaultPosition,
//    val hazards: List<Hazard> = emptyList(),
//    val isLoading: Boolean = false,
//    val errorMsg: String? = null
// )
//
// class MapViewModel(
//    private val gpsService: GpsService,
//    private val hazardsService: HazardsService
// ) : ViewModel() {
//
//    private val _uiState = MutableStateFlow(MapUiState())
//    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
//
//    init {
//        // Observer les changements de position et de dangers
//        viewModelScope.launch {
//            combine(
//                gpsService.positionState,
//                hazardsService.currentHazardsState
//            ) { positionState, hazards ->
//                MapUiState(
//                    position = positionState.position,
//                    hazards = hazards,
//                    isLoading = positionState.isLoading,
//                    errorMsg = positionState.errorMessage
//                )
//            }.collect { newState ->
//                _uiState.value = newState
//            }
//        }
//
//        // Mettre à jour périodiquement les dangers en fonction de la position
//        viewModelScope.launch {
//            while (isActive) {
//                val currPosition = Location(
//                    latitude = gpsService.positionState.value.position.latitude,
//                    longitude = gpsService.positionState.value.position.longitude
//                )
//
//                val polygon = Location.getPolygon(
//                    currPosition,
//                    AppConfig.rectangleHazardZone.first,
//                    AppConfig.rectangleHazardZone.first
//                )
//
//                val wktPolygon = Location.locationsToWktPolygon(polygon)
//                hazardsService.fetchHazards(wktPolygon)
//
//                delay(AppConfig.fetchDelayMs)
//            }
//        }
//    }
//
//    fun clearErrorMsg() {
//        _uiState.update { it.copy(errorMsg = null) }
//    }
//
//    fun resetHazards() {
//        // Force une mise à jour immédiate des dangers
//        viewModelScope.launch {
//            val currPosition = Location(
//                latitude = gpsService.positionState.value.position.latitude,
//                longitude = gpsService.positionState.value.position.longitude
//            )
//
//            val polygon = Location.getPolygon(
//                currPosition,
//                AppConfig.rectangleHazardZone.first,
//                AppConfig.rectangleHazardZone.first
//            )
//
//            val wktPolygon = Location.locationsToWktPolygon(polygon)
//            hazardsService.fetchHazards(wktPolygon)
//        }
//    }
// }
