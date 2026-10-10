package com.shopflow.data.remote

sealed class DataState<out T> {
    object Loading : DataState<Nothing>()
    data class Success<out T>(val data: T) : DataState<T>()
    data class Error(val message: String) : DataState<Nothing>()


    fun dataOrNull(): T? = (this as? Success)?.data
    fun isLoading(): Boolean = this is Loading
    fun isError(): Boolean = this is Error
}
