package ai.wenjuanpro.app.core.time

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemClock
    @Inject
    constructor() : Clock {
        override fun nowMs(): Long = System.currentTimeMillis()
    }
