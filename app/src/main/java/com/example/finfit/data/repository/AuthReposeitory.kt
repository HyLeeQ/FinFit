package com.example.finfit.data.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthReposeitory {
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    suspend fun signUp(email: String, password: String): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user
    }

    suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password)
        return result.
    }
}
