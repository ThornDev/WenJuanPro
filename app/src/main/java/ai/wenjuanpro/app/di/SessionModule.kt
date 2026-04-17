package ai.wenjuanpro.app.di

import ai.wenjuanpro.app.domain.session.SessionStateHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object SessionModule {
    @Provides
    @ActivityRetainedScoped
    fun provideSessionStateHolder(): SessionStateHolder = SessionStateHolder()
}
