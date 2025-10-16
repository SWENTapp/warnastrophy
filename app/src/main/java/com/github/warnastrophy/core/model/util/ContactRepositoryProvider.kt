package com.github.warnastrophy.core.model.util

import com.github.warnastrophy.core.model.contact.ContactsRepository
import com.github.warnastrophy.core.model.contact.MockContactsRepository

object ContactRepositoryProvider {
  private val _repository: ContactsRepository by lazy { MockContactsRepository() }
  var repository: ContactsRepository = _repository
}
