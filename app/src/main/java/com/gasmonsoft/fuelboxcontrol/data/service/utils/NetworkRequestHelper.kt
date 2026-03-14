package com.gasmonsoft.fuelboxcontrol.data.service.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

suspend fun <T> networkRequestHelper(request: suspend () -> Response<T>): Result<T> =
    withContext(Dispatchers.IO) {
        try {
            val res = request()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(DataError.Http(res.code(), res.errorBody()?.string()))
            }
        } catch (e: IOException) {
            Result.failure(DataError.Network(e))
        } catch (e: HttpException) {
            Result.failure(DataError.Http(e.code(), e.message()))
        } catch (e: Exception) {
            Log.e("Error", e.message.toString())
            Result.failure(DataError.Unknown())
        }
    }