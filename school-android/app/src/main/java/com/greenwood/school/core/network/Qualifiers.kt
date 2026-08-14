package com.greenwood.school.core.network

import javax.inject.Qualifier

/** The environment-specific API base URL (`BuildConfig.BASE_URL`). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

/**
 * Bare OkHttp client with **no** interceptors or authenticator, used only for the
 * token-refresh call so it cannot recurse into [TokenAuthenticator].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshClient

/** Fully configured client used by Retrofit for every normal call. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiClient
