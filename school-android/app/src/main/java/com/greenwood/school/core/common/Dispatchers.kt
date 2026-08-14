package com.greenwood.school.core.common

import javax.inject.Qualifier

/**
 * Injecting dispatchers instead of hard-coding `Dispatchers.IO` is what makes the
 * repository tests deterministic — they swap in a `StandardTestDispatcher`.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/** Application-scoped [kotlinx.coroutines.CoroutineScope] for fire-and-forget work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
