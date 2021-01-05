package com.deeosoft.samicsub.di

import android.content.Context
import com.deeosoft.samicsub.helper.AppPref
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
class AppPrefModule {
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context):Context{
        return context
    }
    @Provides
    @Singleton
    fun provideAppPrefConfig(context: Context):AppPref{
        return AppPref(context)
    }
}