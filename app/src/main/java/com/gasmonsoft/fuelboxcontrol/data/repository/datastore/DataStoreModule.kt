package com.gasmonsoft.fuelboxcontrol.data.repository.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

const val dataStoreName = "tank_selected"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = dataStoreName)

class DataStoreRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val selectedTank: Flow<TankSelection> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val tankId = preferences[TANK_ID] ?: -1
            val nameTankId = preferences[NAME_TANK_ID] ?: ""
            val tankType = preferences[TANK_TYPE] ?: false
            TankSelection(tankId, nameTankId, tankType)
        }

    suspend fun saveTank(tank: TankSelection) {
        dataStore.edit { preferences ->
            preferences[TANK_ID] = tank.tankId
            preferences[NAME_TANK_ID] = tank.nameTankId
            preferences[TANK_TYPE] = tank.tankType
        }
    }

    suspend fun clearTank() {
        dataStore.edit { it.clear() }
    }

    companion object {
        val TANK_ID = intPreferencesKey("tank_id")
        val NAME_TANK_ID = stringPreferencesKey("name_tank_id")
        val TANK_TYPE = booleanPreferencesKey("tank_type")
    }
}

data class TankSelection(
    val tankId: Int,
    val nameTankId: String,
    val tankType: Boolean
)