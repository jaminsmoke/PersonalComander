package com.jaminsmoke.personalcomander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.jaminsmoke.personalcomander.PersonalComanderApp
import com.jaminsmoke.personalcomander.data.Mesa
import kotlinx.coroutines.flow.Flow

class MesasViewModel(application: Application) : AndroidViewModel(application) {
    val mesas: Flow<List<Mesa>> =
        (application as PersonalComanderApp).db.mesaDao().observeAll()
}
