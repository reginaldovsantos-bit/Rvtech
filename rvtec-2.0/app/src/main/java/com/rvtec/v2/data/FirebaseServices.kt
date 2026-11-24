package com.rvtec.v2.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Firebase bootstrap and authenticated cloud snapshot transport.
 * Local SharedPreferences remains the source of truth until a signed-in
 * user explicitly requests synchronization.
 */
class FirebaseServices(context: Context) {
    private val app: FirebaseApp? = FirebaseApp.initializeApp(context.applicationContext)

    val isConfigured: Boolean get() = app != null

    fun installAppCheck(): Boolean {
        if (app == null) return false
        FirebaseAppCheck.getInstance(app)
            .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        return true
    }

    private fun authenticatedUserId(): String =
        FirebaseAuth.getInstance(requireNotNull(app) { "Firebase não configurado para o pacote RVTEC 2.0." })
            .currentUser?.uid ?: error("Faça login antes de sincronizar os dados.")

    suspend fun uploadSnapshot(snapshot: JSONObject) {
        val firebaseApp = requireNotNull(app) {
            "Firebase não configurado. Registre com.rvtec.manutencao.v2 no Firebase e adicione google-services.json."
        }
        val uid = authenticatedUserId()
        FirebaseFirestore.getInstance(firebaseApp)
            .collection("users").document(uid)
            .collection("rvtec").document("snapshot")
            .set(mapOf(
                "schemaVersion" to 1,
                "updatedAt" to System.currentTimeMillis(),
                "payload" to snapshot.toString()
            ), SetOptions.merge()).await()
    }

    suspend fun downloadSnapshot(): JSONObject? {
        val firebaseApp = requireNotNull(app) {
            "Firebase não configurado. Registre com.rvtec.manutencao.v2 no Firebase e adicione google-services.json."
        }
        val uid = authenticatedUserId()
        val document = FirebaseFirestore.getInstance(firebaseApp)
            .collection("users").document(uid)
            .collection("rvtec").document("snapshot").get().await()
        val payload = document.getString("payload") ?: return null
        return JSONObject(payload)
    }
}
