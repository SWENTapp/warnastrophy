package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.data.interfaces.UserPreferencesRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

class UserPreferencesRepositoryRemoteTest {

  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var repository: UserPreferencesRepository

  @Before
  fun setup() {
    mockFirestore = mockk(relaxed = true)
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)

    every { mockFirestore.collection("userPreferences") } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test_user_123"

    val successTask = Tasks.forResult<Void>(null)
    every { mockDocument.update(any<String>(), any()) } returns successTask
    every { mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()) } returns successTask

    repository = UserPreferencesRepositoryRemote(mockFirestore)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `getUserId returns current user id when user is authenticated`() = runTest {
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test_user_456"

    repository.setAlertMode(true)

    verify { mockCollection.document("test_user_456") }
  }

  @Test
  fun `setAlertMode does nothing when user is not authenticated`() = runTest {
    every { mockAuth.currentUser } returns null

    repository.setAlertMode(true)

    verify(exactly = 0) { mockDocument.update(any<String>(), any()) }
    verify(exactly = 0) { mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()) }
  }

  @Test
  fun `getUserPreferences emits default preferences when user is not authenticated`() = runTest {
    every { mockAuth.currentUser } returns null

    val result = repository.getUserPreferences.first()

    assertEquals(UserPreferences.default(), result)
    assertFalse(result.dangerModePreferences.alertMode)
    assertFalse(result.dangerModePreferences.inactivityDetection)
    assertFalse(result.dangerModePreferences.automaticSms)
    assertFalse(result.dangerModePreferences.automaticCalls)
    assertFalse(result.dangerModePreferences.microphoneAccess)
    assertFalse(result.themePreferences)
  }

  @Test
  fun `getUserPreferences emits default preferences when document doesn't exist`() = runTest {
    val mockSnapshot = mockk<DocumentSnapshot>()
    every { mockSnapshot.exists() } returns false

    val listenerSlot = slot<EventListener<DocumentSnapshot>>()
    every { mockDocument.addSnapshotListener(capture(listenerSlot)) } answers
        {
          val registration = mockk<ListenerRegistration>(relaxed = true)
          listenerSlot.captured.onEvent(mockSnapshot, null)
          registration
        }

    val result = repository.getUserPreferences.first()

    assertFalse(result.dangerModePreferences.alertMode)
    assertFalse(result.dangerModePreferences.inactivityDetection)
    assertFalse(result.dangerModePreferences.automaticSms)
    assertFalse(result.dangerModePreferences.automaticCalls)
    assertFalse(result.dangerModePreferences.microphoneAccess)
    assertFalse(result.themePreferences)
  }

  @Test
  fun `getUserPreferences emits mapped preferences when document exists`() = runTest {
    val data =
        mapOf(
            "alertMode" to true,
            "inactivityDetection" to true,
            "automaticSms" to false,
            "automaticCalls" to false,
            "microphoneAccess" to true,
            "darkMode" to true)

    val mockSnapshot = mockk<DocumentSnapshot>()
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.data } returns data

    val listenerSlot = slot<EventListener<DocumentSnapshot>>()

    every { mockDocument.addSnapshotListener(capture(listenerSlot)) } answers
        {
          val registration = mockk<ListenerRegistration>(relaxed = true)
          listenerSlot.captured.onEvent(mockSnapshot, null)
          registration
        }

    val result = repository.getUserPreferences.first()

    assertTrue(result.dangerModePreferences.alertMode)
    assertTrue(result.dangerModePreferences.inactivityDetection)
    assertFalse(result.dangerModePreferences.automaticSms)
    assertFalse(result.dangerModePreferences.automaticCalls)
    assertTrue(result.dangerModePreferences.microphoneAccess)
    assertTrue(result.themePreferences)
  }

  @Test
  fun `getUserPreferences handles null values in document`() = runTest {
    val data = mapOf("alertMode" to null, "inactivityDetection" to true, "microphoneAccess" to null)

    val mockSnapshot = mockk<DocumentSnapshot>()
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.data } returns data

    val listenerSlot = slot<EventListener<DocumentSnapshot>>()
    every { mockDocument.addSnapshotListener(capture(listenerSlot)) } answers
        {
          val registration = mockk<ListenerRegistration>(relaxed = true)
          listenerSlot.captured.onEvent(mockSnapshot, null)
          registration
        }

    val result = repository.getUserPreferences.first()

    assertFalse(result.dangerModePreferences.alertMode)
    assertTrue(result.dangerModePreferences.inactivityDetection)
    assertFalse(result.dangerModePreferences.automaticSms)
    assertFalse(result.dangerModePreferences.microphoneAccess)
    assertFalse(result.themePreferences)
  }

  @Test
  fun `setAlertMode updates field successfully when authenticated`() = runTest {
    repository.setAlertMode(true)

    verify { mockDocument.update("alertMode", true) }
  }

  @Test
  fun `setAlertMode creates document with merge when update fails`() = runTest {
    val updatedTask = Tasks.forException<Void>(Exception("Document not found"))
    val setTask = Tasks.forResult<Void>(null)

    every { mockDocument.update("alertMode", false) } returns updatedTask
    every { mockDocument.set(any<Map<String, Any>>(), SetOptions.merge()) } returns setTask

    repository.setAlertMode(false)

    verify { mockDocument.update("alertMode", false) }

    val captor = slot<Map<String, Any>>()
    verify { mockDocument.set(capture(captor), SetOptions.merge()) }

    assertTrue(captor.captured["alertMode"] == false)
  }

  @Test
  fun `setInactivityDetection updates field successfully when authenticated`() = runTest {
    repository.setInactivityDetection(true)

    verify { mockDocument.update("inactivityDetection", true) }
  }

  @Test
  fun `setInactivityDetection does nothing when user is not authenticated`() = runTest {
    every { mockAuth.currentUser } returns null

    repository.setInactivityDetection(true)

    verify(exactly = 0) { mockDocument.update(any<String>(), any()) }
  }

  @Test
  fun `setAutomaticSms updates field successfully when authenticated`() = runTest {
    repository.setAutomaticSms(false)

    verify { mockDocument.update("automaticSms", false) }
  }

  @Test
  fun `setAutomaticCalls updates field successfully when authenticated`() = runTest {
    repository.setAutomaticCalls(true)

    verify { mockDocument.update("automaticCalls", true) }
  }

  @Test
  fun `setAutomaticSms does nothing when user is not authenticated`() = runTest {
    every { mockAuth.currentUser } returns null

    repository.setAutomaticSms(false)

    verify(exactly = 0) { mockDocument.update(any<String>(), any()) }
  }

  @Test
  fun `setDarkMode updates field successfully when authenticated`() = runTest {
    repository.setDarkMode(true)

    verify { mockDocument.update("darkMode", true) }
  }

  @Test
  fun `setDarkMode does nothing when user is not authenticated`() = runTest {
    every { mockAuth.currentUser } returns null

    repository.setDarkMode(true)

    verify(exactly = 0) { mockDocument.update(any<String>(), any()) }
  }

  @Test
  fun `setMicrophoneAccess updates field successfully when authenticated`() = runTest {
    repository.setMicrophoneAccess(true)

    verify { mockDocument.update("microphoneAccess", true) }
  }

  @Test
  fun `setMicrophoneAccess does nothing when user is not authenticated`() = runTest {
    every { mockAuth.currentUser } returns null

    repository.setMicrophoneAccess(true)

    verify(exactly = 0) { mockDocument.update(any<String>(), any()) }
  }

  @Test
  fun `multiple field updates work correctly when authenticated`() = runTest {
    repository.setAlertMode(true)
    repository.setInactivityDetection(false)
    repository.setAutomaticSms(true)
    repository.setDarkMode(false)
    repository.setMicrophoneAccess(true)

    verify { mockDocument.update("alertMode", true) }
    verify { mockDocument.update("inactivityDetection", false) }
    verify { mockDocument.update("automaticSms", true) }
    verify { mockDocument.update("darkMode", false) }
    verify { mockDocument.update("microphoneAccess", true) }
  }

  @Test
  fun `multiple field updates do nothing when not authenticated`() = runTest {
    every { mockAuth.currentUser } returns null

    repository.setAlertMode(true)
    repository.setInactivityDetection(false)
    repository.setAutomaticSms(true)
    repository.setDarkMode(false)
    repository.setMicrophoneAccess(true)

    verify(exactly = 0) { mockDocument.update(any<String>(), any()) }
    verify(exactly = 0) { mockDocument.set(any<Map<String, Any>>(), any<SetOptions>()) }
  }

  @Test
  fun `getUserPreferences closes listener on flow cancellation`() = runTest {
    val mockRegistration = mockk<ListenerRegistration>(relaxed = true)
    val mockSnapshot = mockk<DocumentSnapshot>()
    every { mockSnapshot.exists() } returns false

    val listenerSlot = slot<EventListener<DocumentSnapshot>>()
    every { mockDocument.addSnapshotListener(capture(listenerSlot)) } answers
        {
          listenerSlot.captured.onEvent(mockSnapshot, null)
          mockRegistration
        }

    repository.getUserPreferences.first()

    verify(timeout = 10000) { mockRegistration.remove() }
  }

  @Test
  fun `getUserPreferences does not add listener when not authenticated`() = runTest {
    every { mockAuth.currentUser } returns null

    repository.getUserPreferences.first()

    verify(exactly = 0) { mockDocument.addSnapshotListener(any()) }
  }

  @Test
  fun `mapDocumentToUserPreferences handles all boolean combinations`() = runTest {
    val allTrueData =
        mapOf(
            "alertMode" to true,
            "inactivityDetection" to true,
            "automaticSms" to true,
            "automaticCalls" to true,
            "microphoneAccess" to true,
            "darkMode" to true)

    val mockSnapshot = mockk<DocumentSnapshot>()
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.data } returns allTrueData

    val listenerSlot = slot<EventListener<DocumentSnapshot>>()
    every { mockDocument.addSnapshotListener(capture(listenerSlot)) } answers
        {
          val registration = mockk<ListenerRegistration>(relaxed = true)
          listenerSlot.captured.onEvent(mockSnapshot, null)
          registration
        }

    val result = repository.getUserPreferences.first()

    assertTrue(result.dangerModePreferences.alertMode)
    assertTrue(result.dangerModePreferences.inactivityDetection)
    assertTrue(result.dangerModePreferences.automaticSms)
    assertTrue(result.dangerModePreferences.automaticCalls)
    assertTrue(result.dangerModePreferences.microphoneAccess)
    assertTrue(result.themePreferences)
  }

  @Test
  fun `updateField preserves other fields with merge`() = runTest {
    val updateTask = Tasks.forException<Void>(Exception("Document not found"))
    val setTask = Tasks.forResult<Void>(null)

    every { mockDocument.update(any<String>(), any()) } returns updateTask
    every { mockDocument.set(any<Map<String, Any>>(), SetOptions.merge()) } returns setTask

    repository.setAlertMode(true)

    verify { mockDocument.set(any<Map<String, Any>>(), SetOptions.merge()) }
  }

  @Test
  fun `repository uses correct collection name when authenticated`() = runTest {
    repository.setAlertMode(true)

    verify { mockFirestore.collection("userPreferences") }
  }

  @Test
  fun `repository uses correct field names when authenticated`() = runTest {
    repository.setAlertMode(true)
    verify { mockDocument.update("alertMode", any()) }

    repository.setInactivityDetection(true)
    verify { mockDocument.update("inactivityDetection", any()) }

    repository.setAutomaticSms(true)
    verify { mockDocument.update("automaticSms", any()) }

    repository.setDarkMode(true)
    verify { mockDocument.update("darkMode", any()) }

    repository.setMicrophoneAccess(true)
    verify { mockDocument.update("microphoneAccess", any()) }
  }

  @Test
  fun `authentication state changes are reflected in subsequent calls`() = runTest {
    every { mockAuth.currentUser } returns mockUser
    repository.setAlertMode(true)
    verify { mockDocument.update("alertMode", true) }

    every { mockAuth.currentUser } returns null
    repository.setAlertMode(false)
    verify(exactly = 1) { mockDocument.update("alertMode", true) }
    verify(exactly = 0) { mockDocument.update("alertMode", false) }
  }
}
