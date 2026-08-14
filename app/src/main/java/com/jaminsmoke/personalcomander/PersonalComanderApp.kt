package com.jaminsmoke.personalcomander

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import com.jaminsmoke.personalcomander.data.AppDatabase
import com.jaminsmoke.personalcomander.data.Sala
import com.jaminsmoke.personalcomander.data.Seed
import com.jaminsmoke.personalcomander.data.sesion.RecogerServicio
import com.jaminsmoke.personalcomander.data.sesion.SesionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PersonalComanderApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val db: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "personal_comander.db")
            .addMigrations(
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
            )
            .fallbackToDestructiveMigration(false)
            .build()
            .also { seedIfEmpty(it) }
    }

    val sesion: SesionRepository by lazy { SesionRepository(this, applicationScope) }

    val recoger: RecogerServicio by lazy {
        RecogerServicio(this, db, sesion, applicationScope)
    }

    override fun onCreate() {
        super.onCreate()
        recoger
    }

    private fun seedIfEmpty(db: AppDatabase) {
        applicationScope.launch {
            if (db.mesaDao().count() == 0) {
                db.withTransaction {
                    if (db.salaDao().count() == 0) {
                        db.salaDao().insertAll(Seed.salas())
                    }
                    val ids = db.salaDao().getAll().associate { it.nombre to it.id }.toMutableMap()
                    Seed.salas().forEach { plantilla ->
                        if (plantilla.nombre !in ids) {
                            val id = db.salaDao().insert(
                                Sala(nombre = plantilla.nombre, orden = db.salaDao().getMaxOrden() + 1)
                            )
                            ids[plantilla.nombre] = id
                        }
                    }
                    db.mesaDao().insertAll(Seed.mesas(ids))
                }
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
