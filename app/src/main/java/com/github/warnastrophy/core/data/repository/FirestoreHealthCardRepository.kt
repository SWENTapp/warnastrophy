package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.util.CryptoUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreHealthCardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : HealthCardRepository {

    private val gson = Gson()

    override suspend fun upsertMyHealthCard(card: HealthCard) {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val json = gson.toJson(card)
        val payload = mapOf("ciphertext" to CryptoUtils.encrypt(json))
        db.collection("healthCards").document(uid).set(payload).await()
    }

    override suspend fun getMyHealthCardOnce(fromCacheFirst: Boolean): HealthCard? {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val source = if (fromCacheFirst) Source.CACHE else Source.SERVER
        val snap = db.collection("healthCards").document(uid).get(source).await()
        val c = snap.getString("ciphertext") ?: return null
        return gson.fromJson(CryptoUtils.decrypt(c), HealthCard::class.java)
    }

    override fun observeMyHealthCard(): Flow<HealthCard?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(null); close(); return@callbackFlow }
        val reg = db.collection("healthCards").document(uid)
            .addSnapshotListener { snap, _ ->
                val hc = snap?.getString("ciphertext")?.let { CryptoUtils.decrypt(it) }?.let {
                    gson.fromJson(it, HealthCard::class.java)
                }
                trySend(hc)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun deleteMyHealthCard() {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        db.collection("healthCards").document(uid).delete().await()
    }
}
