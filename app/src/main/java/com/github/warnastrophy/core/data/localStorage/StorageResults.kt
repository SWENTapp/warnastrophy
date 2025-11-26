package com.github.warnastrophy.core.data.localStorage

import com.github.warnastrophy.core.model.HealthCard

/**
 * Represents the result of a storage operation.
 *
 * @param T The type of the successful result, if any.
 */
sealed class StorageResult<out T> {
  /** Indicates the operation completed successfully with [data]. */
  data class Success<T>(val data: T) : StorageResult<T>()

  /** Indicates the operation failed with the provided [exception]. */
  data class Error(val exception: StorageException) : StorageResult<Nothing>()
}

/**
 * Represents different types of storage-related errors, including encryption, decryption,
 * serialization, deserialization, and access issues.
 *
 * Each error type wraps the original cause to preserve stack traces.
 */
sealed class StorageException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
  /** Thrown when encryption fails before writing to storage. */
  class EncryptionError(cause: Throwable) : StorageException("Error during encryption", cause)

  /** Thrown when decryption fails while reading from storage. */
  class DecryptionError(cause: Throwable) : StorageException("Error during decryption", cause)

  /** Thrown when serialization of [HealthCard] to JSON fails. */
  class SerializationError(cause: Throwable) :
      StorageException("Error during serialization", cause)

  /** Thrown when deserialization of JSON to [HealthCard] fails. */
  class DeserializationError(cause: Throwable) :
      StorageException("Error during deserialization", cause)

  /** Thrown when reading or writing to DataStore fails. */
  class DataStoreError(cause: Throwable) : StorageException("Access error to DataStore", cause)
}
