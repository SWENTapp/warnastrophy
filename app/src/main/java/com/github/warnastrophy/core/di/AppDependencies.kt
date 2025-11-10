package com.github.warnastrophy.core.di

import android.content.Context
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.data.repository.HazardRepositoryProvider
import com.github.warnastrophy.core.data.repository.usecase.RefreshHazardsIfMovedUseCase
import com.github.warnastrophy.core.model.ErrorHandler
import com.github.warnastrophy.core.model.GpsServiceFactory
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.HazardsServiceFactory
import com.github.warnastrophy.core.model.PermissionManager
import com.github.warnastrophy.core.model.PermissionManagerInterface
import com.github.warnastrophy.core.model.PositionService
import com.google.android.gms.location.LocationServices

object AppDependencies {
  private lateinit var _errorHandler: ErrorHandler
  val errorHandler
    get() = _errorHandler

  private lateinit var _gpsService: PositionService
  val gpsService
    get() = _gpsService

  private lateinit var _hazardsService: HazardsDataService
  val hazardsService
    get() = _hazardsService

  private lateinit var _permissionManager: PermissionManagerInterface
  val permissionManager
    get() = _permissionManager

  private lateinit var _refreshHazardsIfMovedUseCase: RefreshHazardsIfMovedUseCase
  val refreshHazardsIfMovedUseCase
    get() = _refreshHazardsIfMovedUseCase

  /**
   * Initializes all core application dependencies.
   *
   * This method should be called once, typically from the app's main entry point ([MainActivity]).
   *
   * It sets up and wires together:
   * - Hazard Repository
   * - Core services such as GPS and hazard monitoring
   * - Error handling infrastructure
   *
   * After this function completes, all dependencies in [AppDependencies] are ready to be accessed
   * across the app.
   *
   * @param appContext The application context used to initialize services and repositories.
   * @see ContactRepositoryProvider
   * @see HazardRepositoryProvider
   * @see GpsServiceFactory
   * @see HazardsServiceFactory
   * @see ErrorHandler
   */
  fun init(appContext: Context) {
    val context = appContext.applicationContext

    // Initialize repositories
    ContactRepositoryProvider.init(context)
    val hazardsRepository = HazardRepositoryProvider.repository

    // Core error handler
    _errorHandler = ErrorHandler()

    // Location client + GPS service
    val locationClient = LocationServices.getFusedLocationProviderClient(context)
    val gpsServiceFactory = GpsServiceFactory(locationClient, errorHandler)
    _gpsService = gpsServiceFactory.create()

    // Hazards service
    val hazardsServiceFactory = HazardsServiceFactory(hazardsRepository, gpsService, errorHandler)
    _hazardsService = hazardsServiceFactory.create()
    _permissionManager = PermissionManager(context)
  }

  fun initForTest(
      gpsServiceMock: PositionService,
      hazardsServiceMock: HazardsDataService,
      permissionManager: PermissionManagerInterface,
      mockUseCase: RefreshHazardsIfMovedUseCase,
      errorHandler: ErrorHandler = ErrorHandler()
  ) {
    _gpsService = gpsServiceMock
    _hazardsService = hazardsServiceMock
    _permissionManager = permissionManager
    _errorHandler = errorHandler
    _refreshHazardsIfMovedUseCase = mockUseCase
  }
}
