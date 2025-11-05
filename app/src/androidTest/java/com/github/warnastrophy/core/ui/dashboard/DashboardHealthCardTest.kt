package com.github.warnastrophy.core.ui.dashboard

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.data.local.HealthCardStorage
import com.github.warnastrophy.core.data.local.StorageException
import com.github.warnastrophy.core.data.local.StorageResult
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.ui.components.LoadingTestTags
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.github.warnastrophy.core.ui.util.BaseSimpleComposeTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

class DashboardHealthCardTest : BaseSimpleComposeTest() {
  private lateinit var mockContext: Context

  @Before
  override fun setUp() {
    super.setUp()
    mockContext = mockk(relaxed = true)
    mockkObject(HealthCardStorage)
  }

  @After
  override fun tearDown() {
    super.tearDown()
    unmockkAll()
  }

  // ==== STATELESS TESTS ====
  @Test
  fun stateless_displaysLoading_whenIsLoadingIsTrue() {
    composeTestRule.setContent {
      MainAppTheme {
        DashboardHealthCardStateless(healthCard = null, onHealthCardClick = {}, isLoading = true)
      }
    }

    composeTestRule.onNodeWithTag(DashboardHealthCardTestTags.CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.TITLE, useUnmergedTree = true)
        .assertTextEquals("Health")
    composeTestRule
        .onNodeWithTag(LoadingTestTags.LOADING_INDICATOR, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_displaysEmptyState_whenNoHealthCard() {
    composeTestRule.setContent {
      DashboardHealthCardStateless(healthCard = null, onHealthCardClick = {}, isLoading = false)
    }

    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_displaysHealthData_withAllInformation() {
    val healthCard =
        HealthCard(
            fullName = "John Doe",
            birthDate = "1990-05-15",
            socialSecurityNumber = "123456789",
            bloodType = "A+",
            allergies = listOf("Peanuts", "Penicillin"),
            medications = listOf("Aspirin", "Ibuprofen"),
            chronicConditions = listOf("Asthma", "Diabetes"),
            organDonor = true)

    composeTestRule.setContent {
      MainAppTheme { DashboardHealthCardStateless(healthCard = healthCard, onHealthCardClick = {}) }
    }

    composeTestRule.onNodeWithTag(DashboardHealthCardTestTags.CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_displaysAllergies_withMoreThanTwo() {
    val healthCard =
        HealthCard(
            fullName = "John Doe",
            birthDate = "1990-05-15",
            socialSecurityNumber = "123456789",
            bloodType = "O-",
            allergies = listOf("Peanuts", "Penicillin", "Shellfish", "Latex"))

    composeTestRule.setContent {
      MainAppTheme { DashboardHealthCardStateless(healthCard = healthCard, onHealthCardClick = {}) }
    }

    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_displaysChronicConditions_withMoreThanTwo() {
    val healthCard =
        HealthCard(
            fullName = "User",
            birthDate = "1985-03-20",
            socialSecurityNumber = "123456779",
            bloodType = "AB+",
            chronicConditions = listOf("Diabetes", "Asthma", "Hypertension", "Arthritis"))

    composeTestRule.setContent {
      MainAppTheme { DashboardHealthCardStateless(healthCard = healthCard, onHealthCardClick = {}) }
    }

    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_displaysMedications_singular() {
    val healthCard =
        HealthCard(
            fullName = "User",
            birthDate = "1980-12-05",
            socialSecurityNumber = "123456789",
            bloodType = "B-",
            medications = listOf("Aspirin"),
            allergies = emptyList(),
            chronicConditions = emptyList(),
            organDonor = false)

    composeTestRule.setContent {
      MainAppTheme { DashboardHealthCardStateless(healthCard = healthCard, onHealthCardClick = {}) }
    }

    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_displaysNoCriticalInfo_whenOnlyBloodTypeMissing() {
    val healthCard =
        HealthCard(
            fullName = "User",
            birthDate = "1988-11-30",
            socialSecurityNumber = "123456789",
            bloodType = null,
            allergies = emptyList(),
            medications = emptyList(),
            chronicConditions = emptyList(),
            organDonor = false)

    composeTestRule.setContent {
      MainAppTheme { DashboardHealthCardStateless(healthCard = healthCard, onHealthCardClick = {}) }
    }

    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_triggersCallback_whenCardClicked() {
    var clicked = false

    composeTestRule.setContent {
      MainAppTheme {
        DashboardHealthCardStateless(healthCard = null, onHealthCardClick = { clicked = true })
      }
    }

    composeTestRule.onNodeWithTag(DashboardHealthCardTestTags.CARD).performClick()
    assert(clicked) { "onHealthCardClick callback was not triggered" }
  }

  // ===== STATEFUL TESTS =====

  @Test
  fun stateful_showsLoading_initially() {
    coEvery { HealthCardStorage.loadHealthCard(any(), any()) } coAnswers
        {
          kotlinx.coroutines.delay(1000)
          StorageResult.Success(null)
        }

    composeTestRule.setContent {
      MainAppTheme {
        DashboardHealthCardStateful(onHealthCardClick = {}, userId = "user", context = mockContext)
      }
    }

    composeTestRule
        .onNodeWithTag(LoadingTestTags.LOADING_INDICATOR, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateful_displaysHealthCard_whenLoadSucceeds() {
    val healthCard =
        HealthCard(
            fullName = "John Doe",
            birthDate = "1990-01-01",
            socialSecurityNumber = "123456789",
            bloodType = "A+",
            allergies = listOf("Peanuts"),
            medications = listOf("Aspirin"),
            chronicConditions = emptyList(),
            organDonor = true)

    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user") } returns
        StorageResult.Success(healthCard)

    composeTestRule.setContent {
      MainAppTheme {
        DashboardHealthCardStateful(onHealthCardClick = {}, userId = "user", context = mockContext)
      }
    }

    composeTestRule.waitForIdleWithTimeout()
    composeTestRule.onNodeWithTag(DashboardHealthCardTestTags.CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
    coVerify { HealthCardStorage.loadHealthCard(mockContext, "user") }
  }

  @Test
  fun stateful_displaysEmptyState_whenNoHealthCardFound() {
    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user") } returns
        StorageResult.Success(null)

    composeTestRule.setContent {
      MainAppTheme {
        DashboardHealthCardStateful(onHealthCardClick = {}, userId = "user", context = mockContext)
      }
    }

    composeTestRule.waitForIdleWithTimeout()
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
    coVerify { HealthCardStorage.loadHealthCard(mockContext, "user") }
  }

  @Test
  fun stateful_displaysEmptyState_onDataStoreError() {
    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user") } returns
        StorageResult.Error(StorageException.DataStoreError(Exception("DB error")))

    composeTestRule.setContent {
      MainAppTheme {
        DashboardHealthCardStateful(onHealthCardClick = {}, userId = "user", context = mockContext)
      }
    }

    composeTestRule.waitForIdleWithTimeout()
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateful_displaysEmptyState_onDecryptionError() {
    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user") } returns
        StorageResult.Error(StorageException.DecryptionError(Exception("Decryption failed")))

    composeTestRule.setContent {
      MainAppTheme {
        DashboardHealthCardStateful(onHealthCardClick = {}, userId = "user", context = mockContext)
      }
    }

    composeTestRule.waitForIdleWithTimeout()
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateful_triggersCallback_whenCardClicked() {
    var clicked = false
    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user") } returns
        StorageResult.Success(
            HealthCard(
                fullName = "Jane",
                birthDate = "1990-01-01",
                socialSecurityNumber = "123456789",
                bloodType = "B+",
                allergies = emptyList(),
                medications = emptyList(),
                chronicConditions = emptyList(),
                organDonor = false))

    composeTestRule.setContent {
      MainAppTheme {
        DashboardHealthCardStateful(
            onHealthCardClick = { clicked = true }, userId = "user", context = mockContext)
      }
    }

    composeTestRule.waitForIdleWithTimeout()
    composeTestRule.onNodeWithTag(DashboardHealthCardTestTags.CARD).performClick()
    assert(clicked) { "Callback not triggered" }
  }

  @Test
  fun stateful_usesDefaultUserId_whenNotProvided() {
    coEvery { HealthCardStorage.loadHealthCard(any(), "John Doe") } returns
        StorageResult.Success(null)

    composeTestRule.setContent {
      MainAppTheme { DashboardHealthCardStateful(onHealthCardClick = {}, context = mockContext) }
    }

    composeTestRule.waitForIdleWithTimeout()
    coVerify { HealthCardStorage.loadHealthCard(any(), "John Doe") }
  }
}
