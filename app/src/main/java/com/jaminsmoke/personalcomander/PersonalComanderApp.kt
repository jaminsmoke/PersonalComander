package com.jaminsmoke.personalcomander

import android.app.Application
import androidx.room.Room
import com.jaminsmoke.personalcomander.data.AppDatabase
import com.jaminsmoke.personalcomander.data.Seed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PersonalComanderApp : Application() {

    val db: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "personal_comander.db")
            .build()
            .also { seedIfEmpty(it) }
    }

    private fun seedIfEmpty(db: AppDatabase) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (db.mesaDao().count() == 0) {
                db.mesaDao().insertAll(Seed.mesas())
                db.productoDao().insertAll(Seed.productos())
            }
        }
    }
}
