package ai.wenjuanpro.app.core

import ai.wenjuanpro.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildConfigProviderImpl
    @Inject
    constructor() : BuildConfigProvider {
    override fun appVersion(): String = BuildConfig.VERSION_NAME
}
