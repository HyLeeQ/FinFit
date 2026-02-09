package com.example.finfit.data.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthReposeitory {
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    suspend fun signUp(email: String, password: String): FirebaseUser? {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            return result.user
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun signIn(email: String, password: String): FirebaseUser? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            return result.user
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        try {
            return auth.currentUser
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
