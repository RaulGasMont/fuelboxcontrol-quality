package com.gasmonsoft.fbccalidad.domain.session

import com.gasmonsoft.fbccalidad.data.model.database.UserEntity
import javax.inject.Inject

class SessionUseCase @Inject constructor() {

    private val sessionDurationDays = 7L
    private val millisPerDay = 24L * 60L * 60L * 1000L
    private val sessionDurationMillis = sessionDurationDays * millisPerDay

    operator fun invoke(user: UserEntity?): Result<Unit> {
        if (user == null) return Result.failure(Exception("No hay sesión activa"))
        if (isSessionExpired(user.timestamp)) {
            return Result.failure(Exception("La sesión ha expirado"))
        }
        return Result.success(Unit)
    }

    fun isSessionExpired(sessionTimestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        return now - sessionTimestamp >= sessionDurationMillis
    }
}
