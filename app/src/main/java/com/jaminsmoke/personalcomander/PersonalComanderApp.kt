package com.jaminsmoke.personalcomander

import android.app.Application
import androidx.room.Room
import com.jaminsmoke.personalcomander.data.AppDatabase
import com.jaminsmoke.personalcomander.data.Seed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PersonalComanderApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val db: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "personal_comander.db")
            .addMigrations(AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8)
            .fallbackToDestructiveMigration()
            .build()
            .also { seedIfEmpty(it) }
    }

    private fun seedIfEmpty(db: AppDatabase) {
        applicationScope.launch {
            if (db.mesaDao().count() == 0) {
                db.mesaDao().insertAll(Seed.mesas())
            }
            if (db.productoDao().count() == 0) {
                db.productoDao().insertAll(Seed.productos())
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }
}
