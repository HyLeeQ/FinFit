package com.example.finfit.core.common

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String = exception.message ?: "An error occurred") : Result<Nothing>()
    object Loading : Result<Nothing>()
    object Idle : Result<Nothing>()

    val isSuccess get() = this is Success
    val isLoading get() = this is Loading
    val isError get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
}
