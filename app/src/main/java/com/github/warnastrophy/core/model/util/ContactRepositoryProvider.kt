package com.github.warnastrophy.core.model.util

import android.content.Context
import com.github.warnastrophy.core.model.contact.ContactsRepository
import com.github.warnastrophy.core.model.contact.ContactsRepositoryLocal
import com.github.warnastrophy.core.model.contact.MockContactsRepository
import com.github.warnastrophy.core.model.contact.contactDataStore

object ContactRepositoryProvider {
  lateinit var repository: ContactsRepository

  fun init(context: Context) {
    repository = ContactsRepositoryLocal(context.contactDataStore)
  }

  private val _repository: ContactsRepository by lazy { MockContactsRepository() }
}
