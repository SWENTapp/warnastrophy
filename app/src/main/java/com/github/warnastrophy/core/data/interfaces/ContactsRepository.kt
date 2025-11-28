package com.github.warnastrophy.core.data.interfaces

import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.util.AppConfig

interface ContactsRepository {
  /**
   * Adds a new Contact item to the repository.
   *
   * @param contact The Contact item to add.
   * @return A [Result] containing Unit on success or a failure with the exception on error.
   */
  suspend fun addContact(userId: String = AppConfig.defaultUserId, contact: Contact): Result<Unit>

  /** Generates and returns a new unique identifier for a Contact item. */
  fun getNewUid(): String

  /**
   * Retrieves all Contact items from the repository.
   *
   * @return A [Result] containing a list of all Contact items on success, or a failure with the
   *   error.
   */
  suspend fun getAllContacts(userId: String = AppConfig.defaultUserId): Result<List<Contact>>

  /**
   * Retrieves a specific Contact item by its unique identifier.
   *
   * @param contactID The unique identifier of the Contact item to retrieve.
   * @return A [Result] containing the Contact item with the specified identifier on success, or a
   *   failure if not found or on error.
   */
  suspend fun getContact(
      userId: String = AppConfig.defaultUserId,
      contactID: String
  ): Result<Contact>

  /**
   * Edits an existing Contact item in the repository.
   *
   * @param contactID The unique identifier of the Contact item to edit.
   * @param newContact The new value for the Contact item.
   * @return A [Result] containing Unit on success or a failure with the exception if the edit
   *   failed.
   */
  suspend fun editContact(
      userId: String = AppConfig.defaultUserId,
      contactID: String,
      newContact: Contact
  ): Result<Unit>

  /**
   * Deletes a Contact item from the repository.
   *
   * @param contactID The unique identifier of the Contact item to delete.
   * @return A [Result] containing Unit on success or a failure with the exception if deletion
   *   failed.
   */
  suspend fun deleteContact(
      userId: String = AppConfig.defaultUserId,
      contactID: String
  ): Result<Unit>
}
