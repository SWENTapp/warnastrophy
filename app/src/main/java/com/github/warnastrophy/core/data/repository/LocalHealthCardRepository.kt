package com.github.warnastrophy.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.warnastrophy.core.data.local.HealthCardStorage
import com.github.warnastrophy.core.data.local.StorageResult
import com.github.warnastrophy.core.data.local.healthCardDataStore
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.util.CryptoUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalHealthCardRepository(private val context: Context) : HealthCardRepository {

  // No auth → pick a stable local user id
  private val localUid = "local"
  private val gson = Gson()

  override suspend fun upsertMyHealthCard(card: HealthCard) {
    when (val r = HealthCardStorage.saveHealthCard(context, localUid, card)) {
      is StorageResult.Success -> Unit
      is StorageResult.Error -> throw r.exception
    }
  }

  override suspend fun getMyHealthCardOnce(fromCacheFirst: Boolean): HealthCard? {
    return when (val r = HealthCardStorage.loadHealthCard(context, localUid)) {
      is StorageResult.Success -> r.data
      is StorageResult.Error -> throw r.exception
    }
  }

  override fun observeMyHealthCard(): Flow<HealthCard?> {
    // Stream updates directly from DataStore
    val key = stringPreferencesKey("health_card_$localUid")
    return context.healthCardDataStore.data.map { prefs ->
      val enc = prefs[key] ?: return@map null
      val json = CryptoUtils.decrypt(enc)
      gson.fromJson(json, HealthCard::class.java)
    }
  }

  override suspend fun deleteMyHealthCard() {
    when (val r = HealthCardStorage.deleteHealthCard(context, localUid)) {
      is StorageResult.Success -> Unit
      is StorageResult.Error -> throw r.exception
    }
  }
}
