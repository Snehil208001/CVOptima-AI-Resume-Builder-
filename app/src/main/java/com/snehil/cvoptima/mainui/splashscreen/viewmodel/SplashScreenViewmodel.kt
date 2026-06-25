package com.snehil.cvoptima.mainui.splashscreen.viewmodel

import androidx.lifecycle.ViewModel
import com.snehil.cvoptima.data.local.dao.TokenDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewmodel @Inject constructor(
    private val tokenDao: TokenDao
) : ViewModel() {

    suspend fun checkAuthentication(): Boolean {
        val token = tokenDao.getTokenFlow().firstOrNull()
        return !token.isNullOrBlank()
    }
}
