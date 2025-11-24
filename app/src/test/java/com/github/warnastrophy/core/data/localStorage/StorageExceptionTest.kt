package com.github.warnastrophy.core.data.localStorage

import org.junit.Assert.*
import org.junit.Test

class StorageExceptionTest {

  @Test
  fun encryptionError_behave_correctly() {
    val cause = RuntimeException("Encryption failure")
    val exception = StorageException.EncryptionError(cause)

    assertEquals("Error during encryption", exception.message)
    assertSame(cause, exception.cause)
  }

  @Test
  fun decryptionError_behave_correctly() {
    val cause = RuntimeException("Decryption failure")
    val exception = StorageException.DecryptionError(cause)

    assertEquals("Error during decryption", exception.message)
    assertSame(cause, exception.cause)
  }

  @Test
  fun serializationError_behave_correctly() {
    val cause = RuntimeException("Serialization failure")
    val exception = StorageException.SerializationError(cause)

    assertEquals("Error during serialization", exception.message)
    assertSame(cause, exception.cause)
  }

  @Test
  fun deserializationError_behave_correctly() {
    val cause = RuntimeException("Deserialization failure")
    val exception = StorageException.DeserializationError(cause)

    assertEquals("Error during deserialization", exception.message)
    assertSame(cause, exception.cause)
  }

  @Test
  fun dataStoreError_behave_correctly() {
    val cause = RuntimeException("DataStore failure")
    val exception = StorageException.DataStoreError(cause)

    assertEquals("Access error to DataStore", exception.message)
    assertSame(cause, exception.cause)
  }
}
