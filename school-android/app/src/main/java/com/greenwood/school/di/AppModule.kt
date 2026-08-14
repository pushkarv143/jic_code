package com.greenwood.school.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.greenwood.school.core.common.ApplicationScope
import com.greenwood.school.core.common.DefaultDispatcher
import com.greenwood.school.core.common.IoDispatcher
import com.greenwood.school.core.common.MainDispatcher
import com.greenwood.school.data.local.DashboardDao
import com.greenwood.school.data.local.NoticeDao
import com.greenwood.school.data.local.SchoolDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * `ignoreUnknownKeys` is essential: the backend adds response fields over time
     * and an older app build must keep working rather than throwing on an unknown
     * property. `explicitNulls = false` keeps request bodies free of `"field": null`
     * entries, which some of the Spring validators treat differently from absent.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.sessionDataStore

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SchoolDatabase =
        Room.databaseBuilder(context, SchoolDatabase::class.java, SchoolDatabase.NAME)
            // The cache is disposable by definition — rebuilding it is cheaper and
            // safer than shipping migrations for data we can always re-fetch.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNoticeDao(database: SchoolDatabase): NoticeDao = database.noticeDao()

    @Provides
    fun provideDashboardDao(database: SchoolDatabase): DashboardDao = database.dashboardDao()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate

    /** Outlives every screen; used for work that must not die with a ViewModel. */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)
}
