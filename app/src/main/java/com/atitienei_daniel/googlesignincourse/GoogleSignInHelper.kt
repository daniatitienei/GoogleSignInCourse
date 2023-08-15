package com.atitienei_daniel.googlesignincourse

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GoogleSignInHelper(
    private val context: Context,
    private val oneTapClient: SignInClient,
    private val auth: FirebaseAuth
) {
    fun signInWithIntent(intent: Intent): Flow<Result<Unit>> = callbackFlow {
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)

        try {
            auth.signInWithCredential(googleCredentials).await()
            Log.d("Google", "success")
            trySend(Result.success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.d("Google", "error: ${e.message}")
            trySend(Result.failure(e))
        }

        awaitClose {
            close()
        }
    }

    fun createIntent(onSuccess: (IntentSenderRequest) -> Unit) {
        val request = GetSignInIntentRequest.builder()
            .setServerClientId(context.getString(R.string.web_client_id))
            .build()

        Identity.getSignInClient(context)
            .getSignInIntent(request)
            .addOnSuccessListener { result: PendingIntent ->
                try {
                    onSuccess(
                        IntentSenderRequest.Builder(
                            result.intentSender
                        ).build()
                    )

                } catch (e: IntentSender.SendIntentException) {
                    Log.e(ContentValues.TAG, "Google Sign-in failed")
                }
            }
            .addOnFailureListener { e: Exception? ->
                Log.e(
                    ContentValues.TAG,
                    "Google Sign-in failed",
                    e
                )
            }

    }
}
