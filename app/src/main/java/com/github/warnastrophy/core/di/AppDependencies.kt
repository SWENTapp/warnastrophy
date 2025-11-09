package com.github.warnastrophy.core.di

import android.content.Context
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.data.repository.HazardRepositoryProvider
import com.github.warnastrophy.core.model.ErrorHandler
import com.github.warnastrophy.core.model.GpsService
import com.github.warnastrophy.core.model.GpsServiceFactory
import com.github.warnastrophy.core.model.HazardsService
import com.github.warnastrophy.core.model.HazardsServiceFactory
import com.github.warnastrophy.core.model.PermissionManager
import com.github.warnastrophy.core.model.PermissionManagerInterface
import com.google.android.gms.location.LocationServices

object AppDependencies {
  lateinit var errorHandler: ErrorHandler
    private set

  lateinit var gpsService: GpsService
    private set

  lateinit var hazardsService: HazardsService
    private set

  lateinit var permissionManager: PermissionManagerInterface

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
    errorHandler = ErrorHandler()

    // Location client + GPS service
    val locationClient = LocationServices.getFusedLocationProviderClient(context)
    val gpsServiceFactory = GpsServiceFactory(locationClient, errorHandler)
    gpsService = gpsServiceFactory.create()

    // Hazards service
    val hazardsServiceFactory = HazardsServiceFactory(hazardsRepository, gpsService, errorHandler)
    hazardsService = hazardsServiceFactory.create()
    permissionManager = PermissionManager(context)
  }
}
