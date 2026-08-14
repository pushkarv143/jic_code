package com.greenwood.school

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt entry point. Nothing is injected here on purpose.
 *
 * Session restore is owned by [com.greenwood.school.ui.MainViewModel], which calls
 * `SessionManager.warmUp()` in its `init` and holds the first composition back
 * until it resolves. Doing it here as well would duplicate the work, and
 * field-injecting a qualified dependency into an Application is a well-known
 * Hilt/Kotlin sharp edge (the qualifier lands on the property, not the field).
 */
@HiltAndroidApp
class SchoolApplication : Application()
