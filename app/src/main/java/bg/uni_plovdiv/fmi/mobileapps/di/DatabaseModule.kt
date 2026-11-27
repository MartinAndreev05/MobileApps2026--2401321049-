package bg.uni_plovdiv.fmi.mobileapps.di

import android.content.Context
import bg.uni_plovdiv.fmi.mobileapps.data.BookRepository
import bg.uni_plovdiv.fmi.mobileapps.data.local.AppDatabase
import bg.uni_plovdiv.fmi.mobileapps.data.local.BookDao
import bg.uni_plovdiv.fmi.mobileapps.data.remote.BookRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Tells Hilt how to build the data layer. Installed in [SingletonComponent],
 * so each object lives as long as the app and is shared by every screen.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.create(context)

    @Provides
    fun provideBookDao(database: AppDatabase): BookDao = database.bookDao()

    @Provides
    @Singleton
    fun provideBookRepository(bookDao: BookDao, remote: BookRemoteDataSource): BookRepository =
        BookRepository(bookDao, remote)
}
