package com.greenwood.school.di

import com.greenwood.school.BuildConfig
import com.greenwood.school.core.network.ApiClient
import com.greenwood.school.core.network.AuthInterceptor
import com.greenwood.school.core.network.BaseUrl
import com.greenwood.school.core.network.RefreshClient
import com.greenwood.school.core.network.TokenAuthenticator
import com.greenwood.school.data.remote.api.AcademicApi
import com.greenwood.school.data.remote.api.AttendanceApi
import com.greenwood.school.data.remote.api.AccessApi
import com.greenwood.school.data.remote.api.AuthApi
import com.greenwood.school.data.remote.api.ClassroomApi
import com.greenwood.school.data.remote.api.CommunicationApi
import com.greenwood.school.data.remote.api.ExamApi
import com.greenwood.school.data.remote.api.FeeApi
import com.greenwood.school.data.remote.api.HostelApi
import com.greenwood.school.data.remote.api.LibraryApi
import com.greenwood.school.data.remote.api.MyClassApi
import com.greenwood.school.data.remote.api.PayrollApi
import com.greenwood.school.data.remote.api.PeopleApi
import com.greenwood.school.data.remote.api.ReportApi
import com.greenwood.school.data.remote.api.SettingsApi
import com.greenwood.school.data.remote.api.StudentApi
import com.greenwood.school.data.remote.api.TransportApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @BaseUrl
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    /**
     * Debug-only body logging with the token and every password field redacted.
     *
     * `redactHeader` covers the Authorization header; the body redaction below is
     * what stops `{"password":"..."}` from ever reaching logcat — the web client
     * has no equivalent exposure because browsers don't persist request bodies.
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = HttpLoggingInterceptor { message ->
            android.util.Log.d("HTTP", message.redactSecrets())
        }
        logger.level = if (BuildConfig.ENABLE_HTTP_LOGGING) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        logger.redactHeader("Authorization")
        logger.redactHeader("Cookie")
        return logger
    }

    @Provides
    @Singleton
    @RefreshClient
    fun provideRefreshClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

    @Provides
    @Singleton
    @ApiClient
    fun provideApiClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        logging: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Idempotent GETs are retried by OkHttp on a dead pooled connection; this is
        // the only automatic retry in the app. Mutations are never retried silently.
        .retryOnConnectionFailure(true)
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .authenticator(tokenAuthenticator)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        @ApiClient client: OkHttpClient,
        @BaseUrl baseUrl: String,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        // Retrofit requires the trailing slash; the flavour config always supplies one.
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides @Singleton fun provideAccessApi(retrofit: Retrofit): AccessApi =
        retrofit.create(AccessApi::class.java)

    @Provides @Singleton fun provideMyClassApi(retrofit: Retrofit): MyClassApi =
        retrofit.create(MyClassApi::class.java)

    @Provides @Singleton fun provideAcademicApi(retrofit: Retrofit): AcademicApi =
        retrofit.create(AcademicApi::class.java)

    @Provides @Singleton fun provideStudentApi(retrofit: Retrofit): StudentApi =
        retrofit.create(StudentApi::class.java)

    @Provides @Singleton fun providePeopleApi(retrofit: Retrofit): PeopleApi =
        retrofit.create(PeopleApi::class.java)

    @Provides @Singleton fun provideAttendanceApi(retrofit: Retrofit): AttendanceApi =
        retrofit.create(AttendanceApi::class.java)

    @Provides @Singleton fun provideFeeApi(retrofit: Retrofit): FeeApi = retrofit.create(FeeApi::class.java)

    @Provides @Singleton fun provideExamApi(retrofit: Retrofit): ExamApi = retrofit.create(ExamApi::class.java)

    @Provides @Singleton fun provideClassroomApi(retrofit: Retrofit): ClassroomApi =
        retrofit.create(ClassroomApi::class.java)

    @Provides @Singleton fun provideLibraryApi(retrofit: Retrofit): LibraryApi =
        retrofit.create(LibraryApi::class.java)

    @Provides @Singleton fun provideTransportApi(retrofit: Retrofit): TransportApi =
        retrofit.create(TransportApi::class.java)

    @Provides @Singleton fun provideHostelApi(retrofit: Retrofit): HostelApi =
        retrofit.create(HostelApi::class.java)

    @Provides @Singleton fun providePayrollApi(retrofit: Retrofit): PayrollApi =
        retrofit.create(PayrollApi::class.java)

    @Provides @Singleton fun provideCommunicationApi(retrofit: Retrofit): CommunicationApi =
        retrofit.create(CommunicationApi::class.java)

    @Provides @Singleton fun provideReportApi(retrofit: Retrofit): ReportApi = retrofit.create(ReportApi::class.java)

    @Provides @Singleton fun provideSettingsApi(retrofit: Retrofit): SettingsApi =
        retrofit.create(SettingsApi::class.java)
}

/** Strips credential-bearing JSON fields out of a log line before it is emitted. */
private fun String.redactSecrets(): String {
    var result = this
    SECRET_FIELDS.forEach { field ->
        result = result.replace(Regex("\"$field\"\\s*:\\s*\"[^\"]*\""), "\"$field\":\"***\"")
    }
    return result
}

private val SECRET_FIELDS = listOf(
    "password",
    "newPassword",
    "currentPassword",
    "accessToken",
    "refreshToken",
    "token",
)
