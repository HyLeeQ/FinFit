package com.example.finfit.data.repository

import com.example.finfit.features.auth.domain.repository.IAuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository : IAuthRepository {
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    override suspend fun signUp(email: String, password: String): FirebaseUser? {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            return result.user
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override suspend fun signIn(email: String, password: String): FirebaseUser? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            return result.user
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun getCurrentUser(): FirebaseUser? {
        try {
            return auth.currentUser
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}
