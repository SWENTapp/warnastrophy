package com.github.warnastrophy.core.data.repository


object ContactRepositoryProvider {
    private val _repository: ContactsRepository by lazy { MockContactsRepository() }
    var repository: ContactsRepository = _repository

}
