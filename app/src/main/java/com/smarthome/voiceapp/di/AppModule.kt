package com.smarthome.voiceapp.di

import android.content.Context
import androidx.room.Room
import com.smarthome.voiceapp.data.local.AppDatabase
import com.smarthome.voiceapp.data.remote.TPLinkProtocol
import com.smarthome.voiceapp.data.repository.DeviceRepositoryImpl
import com.smarthome.voiceapp.domain.repository.DeviceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context) = 
        Room.databaseBuilder(ctx, AppDatabase::class.java, "smart_home.db").build()
    
    @Provides @Singleton
    fun provideTPLink() = TPLinkProtocol()
    
    @Provides @Singleton
    fun provideRepository(db: AppDatabase, tp: TPLinkProtocol): DeviceRepository = 
        DeviceRepositoryImpl(db, tp)
}
