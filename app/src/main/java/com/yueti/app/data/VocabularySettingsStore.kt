package com.yueti.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vocabularySettingsDataStore by preferencesDataStore(name = "vocabulary_settings_v1")

const val DefaultVocabularyPrompt =
    "为雅思学习者使用 {word} 生成一个自然、具体、12到24词的英文例句；结合释义 {translation}，同时给出中文翻译、常见搭配和一句简短用法提醒。"

data class VocabularySettings(
    val groupSize: Int = 20,
    val includeMastered: Boolean = false,
    val autoExample: Boolean = true,
    val wifiAutoImages: Boolean = true,
    val promptTemplate: String = DefaultVocabularyPrompt,
)

class VocabularySettingsStore(private val context: Context) {
    private object Keys {
        val groupSize = intPreferencesKey("group_size")
        val includeMastered = booleanPreferencesKey("include_mastered")
        val autoExample = booleanPreferencesKey("auto_example")
        val wifiAutoImages = booleanPreferencesKey("wifi_auto_images")
        val promptTemplate = stringPreferencesKey("prompt_template")
    }

    val settings: Flow<VocabularySettings> = context.vocabularySettingsDataStore.data.map { values ->
        VocabularySettings(
            groupSize = (values[Keys.groupSize] ?: 20).coerceIn(5, 50),
            includeMastered = values[Keys.includeMastered] ?: false,
            autoExample = values[Keys.autoExample] ?: true,
            wifiAutoImages = values[Keys.wifiAutoImages] ?: true,
            promptTemplate = values[Keys.promptTemplate]?.takeIf(String::isNotBlank) ?: DefaultVocabularyPrompt,
        )
    }

    suspend fun save(value: VocabularySettings) {
        context.vocabularySettingsDataStore.edit { values ->
            values[Keys.groupSize] = value.groupSize.coerceIn(5, 50)
            values[Keys.includeMastered] = value.includeMastered
            values[Keys.autoExample] = value.autoExample
            values[Keys.wifiAutoImages] = value.wifiAutoImages
            values[Keys.promptTemplate] = value.promptTemplate.ifBlank { DefaultVocabularyPrompt }
        }
    }
}
