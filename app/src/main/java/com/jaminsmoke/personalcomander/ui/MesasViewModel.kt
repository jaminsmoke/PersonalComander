package com.jaminsmoke.personalcomander.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jaminsmoke.personalcomander.data.AppDatabase
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.MesaDao
import kotlinx.coroutines.flow.Flow

class MesasViewModel(mesaDao: MesaDao) : ViewModel() {
    val mesas: Flow<List<Mesa>> = mesaDao.observeAll()

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MesasViewModel(AppDatabase.get(context).mesaDao()) as T
                }
            }
    }
}
