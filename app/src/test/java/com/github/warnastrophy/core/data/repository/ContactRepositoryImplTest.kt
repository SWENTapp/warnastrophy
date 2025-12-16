package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.data.local.StorageException
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.util.CryptoUtils
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ContactRepositoryImplTest {

  private lateinit var firestore: FirebaseFirestore
  private lateinit var impl: ContactRepositoryImpl

  private val userId = "user123"
  private val contact =
      Contact(id = "c1", fullName = "John Doe", phoneNumber = "555", relationship = "brother")

  private val collection: CollectionReference = mockk(relaxed = true)
  private val doc: DocumentReference = mockk(relaxed = true)

  @Before
  fun setup() {
    firestore = mockk(relaxed = true)
    impl = ContactRepositoryImpl(firestore)

    mockkObject(CryptoUtils)

    every { firestore.collection("users") } returns collection
    every { collection.document(userId) } returns doc
    every { doc.collection("contacts") } returns collection
    every { collection.document(contact.id) } returns doc
    every { collection.document("c1") } returns doc
  }

  @Test
  fun `addContact encrypts and writes to firestore`() = runTest {
    val encrypted = "ENCRYPTED_DATA"

    every { CryptoUtils.encrypt(any()) } returns encrypted
    coEvery { doc.set(any()) } returns Tasks.forResult(null)

    val result = impl.addContact(userId, contact)

    assertTrue(result.isSuccess)

    verify { CryptoUtils.encrypt(any()) }

    coVerify { doc.set(match<Map<String, String>> { map -> map["encrypted"] == encrypted }) }
  }

  @Test
  fun `addContact throws EncryptionError on encryption failure`() = runTest {
    every { CryptoUtils.encrypt(any()) } throws Exception("Encryption failed")

    val result = impl.addContact(userId, contact)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.EncryptionError)

    coVerify(exactly = 0) { doc.set(any()) }
  }

  @Test
  fun `addContact throws DataStoreError on firestore failure`() = runTest {
    val encrypted = "ENCRYPTED_DATA"
    every { CryptoUtils.encrypt(any()) } returns encrypted
    coEvery { doc.set(any()) } returns Tasks.forException(Exception("Firestore error"))

    val result = impl.addContact(userId, contact)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.DataStoreError)
  }

  @Test
  fun `getContact decrypts and returns contact`() = runTest {
    val json = """{"id":"c1","fullName":"John Doe","phoneNumber":"555","relationship":"brother"}"""
    val encrypted = "ENCRYPTED_DATA"

    val snap = mockk<DocumentSnapshot>(relaxed = true)

    every { snap.exists() } returns true
    every { snap.getString("encrypted") } returns encrypted
    every { CryptoUtils.decrypt(encrypted) } returns json

    every { doc.get() } returns Tasks.forResult(snap)
    val result = impl.getContact(userId, "c1")

    assertTrue(result.isSuccess)
    assertEquals("c1", result.getOrThrow().id)
    assertEquals("John Doe", result.getOrThrow().fullName)
    assertEquals("555", result.getOrThrow().phoneNumber)
    assertEquals("brother", result.getOrThrow().relationship)
  }

  @Test
  fun `getContact returns error if document does not exist`() = runTest {
    val snap = mockk<DocumentSnapshot>()
    every { snap.exists() } returns false

    coEvery { doc.get() } returns Tasks.forResult(snap)

    val result = impl.getContact(userId, "c1")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.DataStoreError)
  }

  @Test
  fun `getContact throws DataStoreError on missing 'encrypted' field`() = runTest {
    val snap = mockk<DocumentSnapshot>()
    every { snap.exists() } returns true
    every { snap.getString("encrypted") } returns null
    every { doc.get() } returns Tasks.forResult(snap)

    val result = impl.getContact(userId, "c1")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.DataStoreError)
  }

  @Test
  fun `getContact throws DecryptionError on decryption failure`() = runTest {
    val encrypted = "ENCRYPTED_DATA"
    val snap = mockk<DocumentSnapshot>()

    every { snap.exists() } returns true
    every { snap.getString("encrypted") } returns encrypted
    every { CryptoUtils.decrypt(encrypted) } throws Exception("Decryption failed")
    every { doc.get() } returns Tasks.forResult(snap)

    val result = impl.getContact(userId, "c1")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.DecryptionError)
  }

  @Test
  fun `getContact throws DeserializationError on JSON parse failure`() = runTest {
    val encrypted = "ENCRYPTED_DATA"
    val invalidJson = """{"id":"c1","fullName":"John","phoneNumber":"555","relationship":"brother"""

    val snap = mockk<DocumentSnapshot>()
    every { snap.exists() } returns true
    every { snap.getString("encrypted") } returns encrypted
    every { CryptoUtils.decrypt(encrypted) } returns invalidJson
    every { doc.get() } returns Tasks.forResult(snap)

    val result = impl.getContact(userId, "c1")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.DeserializationError)
  }

  @Test
  fun `getContact throws DataStoreError on firestore failure`() = runTest {
    coEvery { doc.get() } returns Tasks.forException(Exception("Firestore error"))

    val result = impl.getContact(userId, "c1")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.DataStoreError)
  }

  @Test
  fun `getAllContacts decrypts and returns all contacts`() = runTest {
    val json = """{"id":"c1","fullName":"John","phoneNumber":"555","relationship":"brother"}"""
    val encrypted = "ENCRYPTED_DATA"

    val snap = mockk<DocumentSnapshot>()
    val querySnap = mockk<QuerySnapshot>()

    every { snap.getString("encrypted") } returns encrypted
    every { CryptoUtils.decrypt(encrypted) } returns json
    every { querySnap.documents } returns listOf(snap)

    coEvery { firestore.collection("users").document(userId).collection("contacts").get() } returns
        Tasks.forResult(querySnap)

    val result = impl.getAllContacts(userId)

    assertTrue(result.isSuccess)
    assertEquals(1, result.getOrThrow().size)
    assertEquals("c1", result.getOrThrow()[0].id)
  }

  @Test
  fun `getAllContacts skips contacts with missing encrypted field`() = runTest {
    val snap1 = mockk<DocumentSnapshot>()
    val snap2 = mockk<DocumentSnapshot>()
    val querySnap = mockk<QuerySnapshot>()

    every { snap1.id } returns "c1"
    every { snap1.getString("encrypted") } returns null

    every { snap2.id } returns "c2"
    every { snap2.getString("encrypted") } returns "ENCRYPTED_DATA"
    every { CryptoUtils.decrypt("ENCRYPTED_DATA") } returns
        """{"id":"c2","fullName":"Jane","phoneNumber":"666","relationship":"sister"}"""

    every { querySnap.documents } returns listOf(snap1, snap2)

    coEvery { firestore.collection("users").document(userId).collection("contacts").get() } returns
        Tasks.forResult(querySnap)

    val result = impl.getAllContacts(userId)

    assertTrue(result.isSuccess)
    assertEquals(1, result.getOrThrow().size)
    assertEquals("c2", result.getOrThrow()[0].id)
  }

  @Test
  fun `getAllContacts skips contacts with decryption errors`() = runTest {
    val snap1 = mockk<DocumentSnapshot>()
    val snap2 = mockk<DocumentSnapshot>()
    val querySnap = mockk<QuerySnapshot>()

    every { snap1.id } returns "c1"
    every { snap1.getString("encrypted") } returns "BAD_ENCRYPTED"
    every { CryptoUtils.decrypt("BAD_ENCRYPTED") } throws Exception("Decryption failed")

    every { snap2.id } returns "c2"
    every { snap2.getString("encrypted") } returns "GOOD_ENCRYPTED"
    every { CryptoUtils.decrypt("GOOD_ENCRYPTED") } returns
        """{"id":"c2","fullName":"Jane","phoneNumber":"666","relationship":"sister"}"""

    every { querySnap.documents } returns listOf(snap1, snap2)

    coEvery { firestore.collection("users").document(userId).collection("contacts").get() } returns
        Tasks.forResult(querySnap)

    val result = impl.getAllContacts(userId)

    assertTrue(result.isSuccess)
    assertEquals(1, result.getOrThrow().size)
    assertEquals("c2", result.getOrThrow()[0].id)
  }

  @Test
  fun `getAllContacts skips contacts with JSON parse errors`() = runTest {
    val snap1 = mockk<DocumentSnapshot>()
    val querySnap = mockk<QuerySnapshot>()

    every { snap1.id } returns "c1"
    every { snap1.getString("encrypted") } returns "ENCRYPTED"
    every { CryptoUtils.decrypt("ENCRYPTED") } returns """{"invalid json"""

    every { querySnap.documents } returns listOf(snap1)

    coEvery { firestore.collection("users").document(userId).collection("contacts").get() } returns
        Tasks.forResult(querySnap)

    val result = impl.getAllContacts(userId)

    assertTrue(result.isSuccess)
    assertEquals(0, result.getOrThrow().size)
  }

  @Test
  fun `getAllContacts throws DataStoreError on firestore failure`() = runTest {
    coEvery { firestore.collection("users").document(userId).collection("contacts").get() } returns
        Tasks.forException(Exception("Firestore error"))

    val result = impl.getAllContacts(userId)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.DataStoreError)
  }

  @Test
  fun `editContact encrypts and updates firestore`() = runTest {
    val encrypted = "ENCRYPTED_EDIT"
    every { CryptoUtils.encrypt(any()) } returns encrypted

    val updatedContact = contact.copy(fullName = "John Updated")

    coEvery { doc.set(any()) } returns Tasks.forResult(null)

    val result = impl.editContact(userId, "c1", updatedContact)

    assertTrue(result.isSuccess)

    coVerify { doc.set(match<Map<String, String>> { map -> map["encrypted"] == encrypted }) }
  }

  @Test
  fun `editContact fails when contact id does not match`() = runTest {
    val mismatchedContact = contact.copy(id = "different-id")

    val result = impl.editContact(userId, "c1", mismatchedContact)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.DataStoreError)

    coVerify(exactly = 0) { doc.set(any()) }
    verify(exactly = 0) { CryptoUtils.encrypt(any()) }
  }

  @Test
  fun `editContact throws EncryptionError on encryption failure`() = runTest {
    every { CryptoUtils.encrypt(any()) } throws Exception("Encryption failed")

    val result = impl.editContact(userId, "c1", contact)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.EncryptionError)

    coVerify(exactly = 0) { doc.set(any()) }
  }

  @Test
  fun `editContact throws DataStoreError on firestore failure`() = runTest {
    val encrypted = "ENCRYPTED_DATA"
    every { CryptoUtils.encrypt(any()) } returns encrypted
    coEvery { doc.set(any()) } returns Tasks.forException(Exception("Firestore error"))

    val result = impl.editContact(userId, "c1", contact)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.DataStoreError)
  }

  @Test
  fun `deleteContact deletes successfully`() = runTest {
    coEvery { doc.delete() } returns Tasks.forResult(null)

    val result = impl.deleteContact(userId, "c1")

    assertTrue(result.isSuccess)
    coVerify { doc.delete() }
  }

  @Test
  fun `deleteContact throws DataStoreError on failure`() = runTest {
    coEvery { doc.delete() } returns Tasks.forException(Exception("Firestore error"))

    val result = impl.deleteContact(userId, "c1")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is StorageException.DataStoreError)
  }
}
