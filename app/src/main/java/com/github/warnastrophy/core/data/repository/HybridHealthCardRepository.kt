package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.domain.model.HealthCard
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.merge

class HybridHealthCardRepository(
    private val local: HealthCardRepository,
    private val remote: HealthCardRepository,
    private val auth: FirebaseAuth // you can even drop this param later
) : HealthCardRepository {

  override fun observeMyHealthCard(): Flow<HealthCard?> {
    return merge(local.observeMyHealthCard(), remote.observeMyHealthCard()).distinctUntilChanged()
  }

  override suspend fun getMyHealthCardOnce(fromCacheFirst: Boolean): HealthCard? {
    // Always try remote first, mirror into local.
    val card = remote.getMyHealthCardOnce(fromCacheFirst)
    if (card != null) local.upsertMyHealthCard(card)
    return card ?: local.getMyHealthCardOnce(true)
  }

  override suspend fun upsertMyHealthCard(card: HealthCard) {
    local.upsertMyHealthCard(card)
    remote.upsertMyHealthCard(card)
  }

  override suspend fun deleteMyHealthCard() {
    local.deleteMyHealthCard()
    remote.deleteMyHealthCard()
  }
}
