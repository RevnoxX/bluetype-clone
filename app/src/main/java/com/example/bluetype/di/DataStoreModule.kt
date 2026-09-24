package com.example.bluetype.di

import android.content.Context
import com.example.bluetype.data.settings.SettingsRepository

/**
 * DataStoreModule — Provider module for SettingsRepository.
 *
 * Responsibility: Supplies singleton instance of SettingsRepository.
 * Depends on: [Context], [SettingsRepository].
 * Notes: Ensures uniform access to DataStore preferences across ViewModels and services (§6).
 */
object DataStoreModule {
    fun provideSettingsRepository(context: Context): SettingsRepository {
        return SettingsRepository(context)
    }
}
