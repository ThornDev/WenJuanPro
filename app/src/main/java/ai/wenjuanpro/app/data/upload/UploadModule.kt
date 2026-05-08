package ai.wenjuanpro.app.data.upload

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UploadModule {
    @Binds
    @Singleton
    abstract fun bindResultUploader(impl: OkHttpResultUploader): ResultUploader
}
