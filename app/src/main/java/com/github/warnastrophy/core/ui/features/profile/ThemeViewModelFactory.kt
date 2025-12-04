package com.github.warnastrophy.core.ui.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.warnastrophy.core.data.provider.UserPreferencesRepositoryProvider
import com.github.warnastrophy.core.data.repository.UserPreferencesRepository

/**
 * Factory class responsible for creating instances of the [ThemeViewModel].
 *
 * This factory implements the [ViewModelProvider.Factory] interface, which allows for the creation
 * of a [ThemeViewModel] with the required dependencies, such as the [UserPreferencesRepository].
 *
 * @param repository The [UserPreferencesRepository] that provides access to the user's preferences,
 *   including theme-related settings.
 */
class ThemeViewModelFactory(
    private val repository: UserPreferencesRepository = UserPreferencesRepositoryProvider.repository
) : ViewModelProvider.Factory {

  /**
   * Creates a [ThemeViewModel] instance.
   *
   * @param modelClass The class type of the ViewModel to be created. In this case, it is expected
   *   to be [ThemeViewModel].
   * @return A new instance of [ThemeViewModel] initialized with the provided
   *   [UserPreferencesRepository].
   * @throws IllegalArgumentException If the [modelClass] is not [ThemeViewModel].
   */
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
      return ThemeViewModel(repository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
