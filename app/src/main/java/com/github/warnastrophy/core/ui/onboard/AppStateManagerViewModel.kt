package com.github.warnastrophy.core.ui.onboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.IntroductionRepository
import com.github.warnastrophy.core.data.repository.OnboardingRepositoryProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * This ViewModel tracks whether the user has completed the initial onboarding screens and provides
 * a mechanism to mark the onboarding as completed.
 *
 * @property repository The [IntroductionRepository] used to read/write onboarding state. Defaults
 *   to [OnboardingRepositoryProvider.repository].
 * @property dispatcher The [CoroutineDispatcher] used for background operations. Defaults to
 *   [Dispatchers.IO] to perform repository calls off the main thread.
 */
class AppStateManagerViewModel(
    private val repository: IntroductionRepository = OnboardingRepositoryProvider.repository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
  private val _isOnboardingCompleted = MutableStateFlow(repository.isOnboardingCompleted())
  val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

  /** Executes the logic to mark the onboarding as completed. */
  fun completeOnboarding() {
    viewModelScope.launch(dispatcher) {
      repository.setOnboardingCompleted()
      _isOnboardingCompleted.value = true
    }
  }
}
