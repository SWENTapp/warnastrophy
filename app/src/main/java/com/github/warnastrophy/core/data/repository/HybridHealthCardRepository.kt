package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.model.HealthCard
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.merge

class HybridHealthCardRepository(
    private val local: HealthCardRepository,
    private val remote: HealthCardRepository,
    private val auth: FirebaseAuth
) : HealthCardRepository {

    override fun observeMyHealthCard(): Flow<HealthCard?> {
        // Emit local immediately for instant UX, then remote (realtime) when signed in.
        return merge(local.observeMyHealthCard(), remote.observeMyHealthCard())
            .distinctUntilChanged()
    }

    override suspend fun getMyHealthCardOnce(fromCacheFirst: Boolean): HealthCard? {
        // Prefer remote if signed in; mirror into local for offline bootstrap.
        val hasUser = auth.currentUser != null
        val card = if (hasUser) remote.getMyHealthCardOnce(fromCacheFirst) else null
        if (card != null) local.upsertMyHealthCard(card)
        return card ?: local.getMyHealthCardOnce(true)
    }

    override suspend fun upsertMyHealthCard(card: HealthCard) {
        // Always persist locally; mirror to remote if signed in.
        local.upsertMyHealthCard(card)
        if (auth.currentUser != null) remote.upsertMyHealthCard(card)
    }

    override suspend fun deleteMyHealthCard() {
        // Delete both (best-effort remote).
        local.deleteMyHealthCard()
        if (auth.currentUser != null) remote.deleteMyHealthCard()
    }
}
