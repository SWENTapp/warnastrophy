package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.data.localStorage.StorageException
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.util.CryptoUtils
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ContactRepositoryImplTest {

  private lateinit var firestore: FirebaseFirestore
  private lateinit var impl: ContactsRepositoryImpl

  private val userId = "user123"
  private val contact =
      Contact(id = "c1", fullName = "John", phoneNumber = "555", relationship = "brother")

  private val collection: CollectionReference = mockk(relaxed = true)
  private val doc: DocumentReference = mockk(relaxed = true)

  @Before
  fun setup() {
    firestore = mockk(relaxed = true)
    impl = ContactsRepositoryImpl(firestore)

    mockkObject(CryptoUtils)

    every { firestore.collection("users") } returns collection
    every { collection.document(userId) } returns doc
    every { doc.collection("contacts") } returns collection
    every { collection.document(contact.id) } returns doc
    every { collection.document("c1") } returns doc
  }

  @Test
  fun `addContact encrypts and writes to firestore`() = runTest {
    val json = """{"id":"c1","fullName":"John","phoneNumber":"555","relationship":"brother"}"""

    coEvery { doc.set(any()) } returns Tasks.forResult(null)

    val result = impl.addContact(userId, contact)

    assertTrue(result.isSuccess)

    coVerify { doc.set(match<Map<String, String>> { map -> map["json"] == json }) }
  }

  @Test
  fun `getContact returns contact`() = runTest {
    val json = """{"id":"c1","fullName":"John","phoneNumber":"555","relationship":"brother"}"""

    val snap = mockk<DocumentSnapshot>(relaxed = true)

    every { snap.exists() } returns true
    every { snap.getString("json") } returns json

    every { doc.get() } returns Tasks.forResult(snap)
    val result = impl.getContact(userId, "c1")

    assertTrue(result.isSuccess)
    assertEquals("c1", result.getOrThrow().id)
  }

  @Test
  fun `getContact returns error if missing`() = runTest {
    val snap = mockk<DocumentSnapshot>()
    every { snap.exists() } returns false

    coEvery { doc.get() } returns Tasks.forResult(snap)

    val result = impl.getContact(userId, "c1")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.DataStoreError)
  }

  @Test
  fun `getAllContacts succeeds and returns contacts`() = runTest {
    val json = """{"id":"c1","fullName":"John","phoneNumber":"555","relationship":"brother"}"""

    val snap = mockk<DocumentSnapshot>()
    val querySnap = mockk<QuerySnapshot>()

    every { snap.getString("json") } returns json
    every { querySnap.documents } returns listOf(snap)

    coEvery { firestore.collection("users").document(userId).collection("contacts").get() } returns
        Tasks.forResult(querySnap)

    coEvery { firestore.collection("users").document(userId).collection("contacts").get() } returns
        Tasks.forResult(querySnap)

    val result = impl.getAllContacts(userId)

    assertTrue(result.isSuccess)
    assertEquals(1, result.getOrThrow().size)
  }

  @Test
  fun `deleteContact deletes successfully`() = runTest {
    coEvery { doc.delete() } returns Tasks.forResult(null)

    val result = impl.deleteContact(userId, "c1")

    assertTrue(result.isSuccess)
    coVerify { doc.delete() }
  }

  @Test
  fun `editContact succeeds and updates firestore`() = runTest {
    // Arrange
    val encrypted = "ENCRYPTED_EDIT"
    every { CryptoUtils.encrypt(any()) } returns encrypted

    // contact.id == contactID -> no ID mismatch
    val updatedContact = contact.copy(fullName = "John Updated") // same id = "c1"

    coEvery { doc.set(any()) } returns Tasks.forResult(null)

    // Act
    val result = impl.editContact(userId, "c1", updatedContact)

    // Assert
    assertTrue(result.isSuccess)

    coVerify {
      // ensure we are writing the encrypted payload
      doc.set(match<Map<String, String>> { map -> map["encrypted"] == encrypted })
    }
  }

  @Test
  fun `editContact fails when contact id does not match`() = runTest {
    // Arrange: newContact.id != contactID
    val mismatchedContact = contact.copy(id = "different-id")

    // Act
    val result = impl.editContact(userId, "c1", mismatchedContact)

    // Assert
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.DataStoreError)

    // Firestore should never be called if IDs don't match
    coVerify(exactly = 0) { doc.set(any()) }
  }
}
