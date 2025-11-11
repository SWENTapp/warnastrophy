package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.model.HealthCard
import kotlinx.coroutines.flow.Flow

/** Contract for CRUD + live updates of the current user's HealthCard. */
interface HealthCardRepository {
    /** Real-time stream of the signed-in user's HealthCard (null if it doesn't exist). */
    fun observeMyHealthCard(): Flow<HealthCard?>

    /** One-shot read (defaults to cache-first). */
    suspend fun getMyHealthCardOnce(fromCacheFirst: Boolean = true): HealthCard?

    /** Create or update the current user's HealthCard. */
    suspend fun upsertMyHealthCard(card: HealthCard)

    /** Delete the current user's HealthCard document. */
    suspend fun deleteMyHealthCard()
}