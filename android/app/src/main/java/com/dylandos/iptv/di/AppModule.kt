package com.dylandos.iptv.di

import android.content.Context
import com.dylandos.iptv.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)

    @Provides
    @Singleton
    fun provideAccountDao(db: AppDatabase) = db.accountDao()

    @Provides
    @Singleton
    fun provideChannelDao(db: AppDatabase) = db.channelDao()

    @Provides
    @Singleton
    fun provideCategoryDao(db: AppDatabase) = db.categoryDao()

    @Provides
    @Singleton
    fun provideProgramDao(db: AppDatabase) = db.programDao()

    @Provides
    @Singleton
    fun provideRecordingDao(db: AppDatabase) = db.recordingDao()
}
