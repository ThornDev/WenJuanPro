package ai.wenjuanpro.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okio.FileSystem as OkioFs

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {
    @Provides
    @Singleton
    fun provideOkioFileSystem(): OkioFs = OkioFs.SYSTEM
}
