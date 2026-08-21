package com.yueti.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "profile")

data class UserProfile(
    val name: String = "",
    val school: String = "",
    val avatarPath: String = "",
    val completed: Boolean = false,
    val dailyGoal: Int = 10,
)

class ProfileStore(private val context: Context) {
    private object Keys {
        val name = stringPreferencesKey("name")
        val school = stringPreferencesKey("school")
        val avatarPath = stringPreferencesKey("avatar_path")
        val completed = booleanPreferencesKey("completed")
        val dailyGoal = intPreferencesKey("daily_goal")
    }

    val profile: Flow<UserProfile> = context.profileDataStore.data.map { values ->
        UserProfile(
            name = values[Keys.name].orEmpty(),
            school = values[Keys.school].orEmpty(),
            avatarPath = values[Keys.avatarPath].orEmpty(),
            completed = values[Keys.completed] ?: false,
            dailyGoal = (values[Keys.dailyGoal] ?: 10).coerceIn(10, 50),
        )
    }

    suspend fun save(name: String, school: String, avatarPath: String) {
        context.profileDataStore.edit { values ->
            values[Keys.name] = name.trim()
            values[Keys.school] = school.trim()
            values[Keys.avatarPath] = avatarPath
            values[Keys.completed] = name.isNotBlank() && school.isNotBlank()
        }
    }

    suspend fun setDailyGoal(goal: Int) {
        context.profileDataStore.edit { values ->
            values[Keys.dailyGoal] = goal.coerceIn(10, 50)
        }
    }
}
