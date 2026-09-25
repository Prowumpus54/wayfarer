package com.wayfarer.rpg

import android.app.Activity
import android.os.CancellationSignal
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class GoogleAccount(private val activity: Activity) {
    private val manager = CredentialManager.create(activity)
    private var cancellation = CancellationSignal()

    fun signIn(
        serverClientId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        cancellation = CancellationSignal()
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val main = android.os.Handler(activity.mainLooper)

        manager.getCredentialAsync(
            activity,
            request,
            cancellation,
            { command -> main.post(command) },
            object : androidx.credentials.CredentialManagerCallback<
                GetCredentialResponse,
                GetCredentialException
            > {
                override fun onResult(result: GetCredentialResponse) {
                    finishGoogleCredential(result, onSuccess, onFailure)
                }

                override fun onError(e: GetCredentialException) {
                    val message = if (e is NoCredentialException) {
                        "No Google account is available on this device."
                    } else {
                        "Google sign-in was cancelled or unavailable."
                    }
                    onFailure(message)
                }
            }
        )
    }

    private fun finishGoogleCredential(
        response: GetCredentialResponse,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val credential = response.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                onFailure("Google did not return a usable sign-in.")
                return
            }
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(
                google.idToken,
                null
            )
            FirebaseAuth.getInstance()
                .signInWithCredential(firebaseCredential)
                .addOnSuccessListener(activity) { onSuccess() }
                .addOnFailureListener(activity) {
                    onFailure("Firebase could not complete Google sign-in.")
                }
        } catch (_: Exception) {
            onFailure("Google sign-in could not be completed.")
        }
    }

    fun close() {
        cancellation.cancel()
    }
}
