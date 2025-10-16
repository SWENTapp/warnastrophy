package com.github.warnastrophy.core.model.contact

interface ContactsRepository {
  /**
   * Adds a new Contact item to the repository.
   *
   * @param contact The Contact item to add.
   */
  suspend fun addContact(contact: Contact)

  /** Generates and returns a new unique identifier for a Contact item. */
  fun getNewUid(): String

  /**
   * Retrieves all Contact items from the repository.
   *
   * @return A list of all Contact items.
   */
  suspend fun getAllContacts(): List<Contact>

  /**
   * Retrieves a specific Contact item by its unique identifier.
   *
   * @param contactID The unique identifier of the Contact item to retrieve.
   * @return The Contact item with the specified identifier.
   * @throws Exception if the Contact item is not found.
   */
  suspend fun getContact(contactID: String): Contact

  /**
   * Edits an existing ToDo item in the repository.
   *
   * @param contactID The unique identifier of the ToDo item to edit.
   * @param newContact The new value for the ToDo item.
   * @throws Exception if the ToDo item is not found.
   */
  suspend fun editContact(contactID: String, newContact: Contact)

  /**
   * Deletes a Contact item from the repository.
   *
   * @param contactID The unique identifier of the Contact item to delete.
   * @throws Exception if the ToDo item is not found.
   */
  suspend fun deleteContact(contactID: String)
}
