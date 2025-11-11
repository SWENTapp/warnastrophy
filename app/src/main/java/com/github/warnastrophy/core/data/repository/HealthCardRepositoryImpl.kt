package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.model.HealthCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
class HealthCardRepositoryImpl(
    private val auth: FirebaseAuth?,                  // can be null
    private val db: FirebaseFirestore,
    private val collectionName: String = HealthCardRepositoryProvider.COLLECTION,
    private val fallbackUidProvider: (() -> String)? = null  // device ID provider
) : HealthCardRepository {

    private fun resolvedUid(): String =
        auth?.currentUser?.uid ?: fallbackUidProvider?.invoke()
        ?: error("No auth user and no fallback UID")

    private fun docRef(id: String = resolvedUid()) =
        db.collection(collectionName).document(id)

    override fun observeMyHealthCard(): Flow<HealthCard?> = callbackFlow {
        val reg = docRef().addSnapshotListener { snap, _ ->
            val card = if (snap != null && snap.exists()) snap.toObject(HealthCard::class.java) else null
            trySend(card)
        }
        awaitClose { reg.remove() }
    }

    override suspend fun getMyHealthCardOnce(fromCacheFirst: Boolean): HealthCard? {
        val source = if (fromCacheFirst) Source.CACHE else Source.SERVER
        val snap = runCatching { docRef().get(source).await() }
            .getOrElse { if (fromCacheFirst) docRef().get(Source.SERVER).await() else throw it }
        return if (snap.exists()) snap.toObject(HealthCard::class.java) else null
    }

    override suspend fun upsertMyHealthCard(card: HealthCard) {
        docRef().set(card, SetOptions.merge()).await()
    }

    override suspend fun deleteMyHealthCard() {
        docRef().delete().await()
    }
}
