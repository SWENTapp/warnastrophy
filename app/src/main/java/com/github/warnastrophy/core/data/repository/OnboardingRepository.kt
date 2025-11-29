package com.github.warnastrophy.core.data.repository

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.warnastrophy.core.ui.onboard.AppStateManagerViewModel

interface IntroductionRepository{
    /**
     * Retrieves the current onboarding completion status from [SharedPreferences].
     *
     * The preference is stored under the file name "onboarding" with the key "completed".
     *
     * @return [Boolean] `true` if the onboarding process has been finished;
     * `false` if it is the user's first time.
     */
    fun isOnboardingCompleted(): Boolean

    /**
     * Sets the onboarding status to complete in [SharedPreferences].
     *
     * This function should be called after the user successfully finishes the final
     * onboarding screen.
     */
    fun setOnboardingCompleted()
}
/**
 * This class determines whether the user has completed the initial application
 * welcome screens.
 *
 * @property context The application [Context] needed to access [SharedPreferences].
 */
class OnboardingRepository(private val context: Context): IntroductionRepository {
    override fun isOnboardingCompleted(): Boolean {
        return context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
            .getBoolean("completed", false)
    }

    override fun setOnboardingCompleted() {
        context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
            .edit {
                putBoolean("completed", true)
            }
    }
}
