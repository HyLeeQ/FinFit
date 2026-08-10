package com.example.finfit.features.auth.domain.repository

import com.google.firebase.auth.FirebaseUser

interface IAuthRepository {
    suspend fun signUp(email: String, password: String): FirebaseUser?
    suspend fun signIn(email: String, password: String): FirebaseUser?
    fun getCurrentUser(): FirebaseUser?
    fun signOut()
}
