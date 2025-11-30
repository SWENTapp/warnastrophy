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
