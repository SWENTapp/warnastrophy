package com.github.warnastrophy.core.data.localStorage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object LocalUserIdStore {
  private val KEY = stringPreferencesKey("local_user_id")

  suspend fun getOrCreate(context: Context): String {
    val ds = context.healthCardDataStore
    val existing = ds.data.map { it[KEY] }.first()
    if (existing != null) return existing
    val fresh = UUID.randomUUID().toString()
    ds.edit { it[KEY] = fresh }
    return fresh
  }
}
