package com.mc.mateamhf.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mc.mateamhf.MaTeamHFApp
import com.mc.mateamhf.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepo: AuthRepository,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data object SignedOut : State
        data class SignedIn(val uid: String, val email: String?, val displayName: String?, val photoUrl: String?) : State
    }

    val state: StateFlow<State> = authRepo.currentUser
        .map { user ->
            if (user == null) State.SignedOut
            else State.SignedIn(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName,
                photoUrl = user.photoUrl?.toString(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            val outcome = authRepo.signIn(activityContext)
            outcome.onFailure { _error.value = it.message ?: it::class.simpleName }
            _busy.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _busy.value = true
            authRepo.signOut()
            _busy.value = false
        }
    }

    fun clearError() { _error.value = null }

    companion object {
        fun factory(app: MaTeamHFApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(app.authRepository) }
        }
    }
}
