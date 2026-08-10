package com.example.finfit.features.auth.domain.usecase

import com.example.finfit.features.auth.domain.repository.IAuthRepository
import com.google.firebase.auth.FirebaseUser

class LoginUseCase(private val authRepository: IAuthRepository) {
    suspend operator fun invoke(email: String, pass: String): FirebaseUser? = authRepository.signIn(email, pass)
}

class RegisterUseCase(private val authRepository: IAuthRepository) {
    suspend operator fun invoke(email: String, pass: String): FirebaseUser? = authRepository.signUp(email, pass)
}

class GetCurrentUserUseCase(private val authRepository: IAuthRepository) {
    operator fun invoke(): FirebaseUser? = authRepository.getCurrentUser()
}

class SignOutUseCase(private val authRepository: IAuthRepository) {
    operator fun invoke() = authRepository.signOut()
}
