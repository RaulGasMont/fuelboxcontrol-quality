package com.gasmonsoft.fbccalidad.data.service.utils
sealed class DataError : Exception() {
    data class Network(override val cause: Throwable) : DataError()
     open class Local : DataError() {
        data class Constraint(override val message: String?) : Local()
        data class Database(override val message: String?) : Local()
        class Unknown : Local()
    }
    data class Http(val code: Int, val body: String?) : DataError()
    class Unknown : DataError() {
        private fun readResolve(): Any = Unknown()
    }
}