package com.example.finfit.core.base

sealed class ResultState<out T> {
    object Idle : ResultState<Nothing>()
    object Loading : ResultState<Nothing>()
    data class Success<out T>(val data: T) : ResultState<T>()
    data class Error(val throwable: Throwable, val message: String = throwable.message ?: "Unknown error") : ResultState<Nothing>()

    val isSuccess get() = this is Success
    val isLoading get() = this is Loading
    val isError get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
}
