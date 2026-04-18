package ai.wenjuanpro.app.di

import ai.wenjuanpro.app.core.device.AndroidDeviceIdProvider
import ai.wenjuanpro.app.core.device.DeviceIdProvider
import ai.wenjuanpro.app.core.io.FileSystem
import ai.wenjuanpro.app.core.io.OkioFileSystem
import ai.wenjuanpro.app.core.time.Clock
import ai.wenjuanpro.app.core.time.SystemClock
import ai.wenjuanpro.app.data.config.ConfigRepository
import ai.wenjuanpro.app.data.config.ConfigRepositoryImpl
import ai.wenjuanpro.app.data.permission.CameraPermissionRepository
import ai.wenjuanpro.app.data.permission.CameraPermissionRepositoryImpl
import ai.wenjuanpro.app.data.permission.PermissionRepository
import ai.wenjuanpro.app.data.permission.PermissionRepositoryImpl
import ai.wenjuanpro.app.core.BuildConfigProvider
import ai.wenjuanpro.app.core.BuildConfigProviderImpl
import ai.wenjuanpro.app.data.result.ResultRepository
import ai.wenjuanpro.app.data.result.ResultRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindPermissionRepository(impl: PermissionRepositoryImpl): PermissionRepository

    @Binds
    @Singleton
    abstract fun bindCameraPermissionRepository(
        impl: CameraPermissionRepositoryImpl,
    ): CameraPermissionRepository

    @Binds
    @Singleton
    abstract fun bindFileSystem(impl: OkioFileSystem): FileSystem

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindDeviceIdProvider(impl: AndroidDeviceIdProvider): DeviceIdProvider

    @Binds
    @Singleton
    abstract fun bindResultRepository(impl: ResultRepositoryImpl): ResultRepository

    @Binds
    @Singleton
    abstract fun bindBuildConfigProvider(impl: BuildConfigProviderImpl): BuildConfigProvider

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemClock): Clock
}
