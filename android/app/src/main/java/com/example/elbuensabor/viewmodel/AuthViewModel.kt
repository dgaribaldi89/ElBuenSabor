package com.example.elbuensabor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elbuensabor.data.remote.dto.UserDto
import com.example.elbuensabor.data.repository.AuthRepository
import com.example.elbuensabor.utils.constants.AppConstants
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _userData = MutableStateFlow<UserDto?>(null)
    val userData: StateFlow<UserDto?> = _userData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _profileMessage = MutableStateFlow<String?>(null)
    val profileMessage: StateFlow<String?> = _profileMessage

    private val _managedUsers = MutableStateFlow<List<UserDto>>(emptyList())
    val managedUsers: StateFlow<List<UserDto>> = _managedUsers

    private val _adminLoading = MutableStateFlow(false)
    val adminLoading: StateFlow<Boolean> = _adminLoading

    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage: StateFlow<String?> = _adminMessage

    private val _adminError = MutableStateFlow<String?>(null)
    val adminError: StateFlow<String?> = _adminError

    init {
        if (repository.currentUser != null) {
            viewModelScope.launch {
                _userData.value = repository.getCurrentUserData()
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    private fun translateError(exception: Throwable): String {
        if (exception is IllegalArgumentException ||
            exception is IllegalStateException ||
            exception is SecurityException
        ) {
            return exception.message ?: "Datos no válidos."
        }

        return if (exception is FirebaseAuthException) {
            when (exception.errorCode) {
                "ERROR_INVALID_EMAIL" -> "El formato del correo electrónico no es válido."
                "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Correo o contraseña incorrectos."
                "ERROR_USER_NOT_FOUND" -> "No existe una cuenta con este correo."
                "ERROR_USER_DISABLED" -> "Esta cuenta ha sido deshabilitada."
                "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Inténtalo más tarde."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo ya está registrado."
                "ERROR_WEAK_PASSWORD" -> "La contraseña es muy débil. Debe tener al menos 6 caracteres."
                "ERROR_NETWORK_REQUEST_FAILED" -> "Error de red. Revisa tu conexión a internet."
                else -> exception.message ?: "Ocurrió un error de autenticación."
            }
        } else {
            when (exception.message) {
                "Usuario no encontrado" -> "No se pudo encontrar la información del usuario."
                "Error al crear usuario" -> "No se pudo crear la cuenta. Inténtalo de nuevo."
                else -> exception.message ?: "Error desconocido"
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _error.value = "Por favor, completa todos los campos."
            return
        }
        if (!validateEmail(email)) {
            _error.value = "Por favor, ingresa un correo electrónico válido."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.login(email, pass)
            result.onSuccess {
                _userData.value = it
            }.onFailure {
                _error.value = translateError(it)
            }
            _isLoading.value = false
        }
    }

    fun register(
        email: String,
        pass: String,
        confirmPass: String,
        nombre: String
    ) {
        if (email.isBlank() || pass.isBlank() || confirmPass.isBlank() || nombre.isBlank()) {
            _error.value = "Por favor, completa todos los campos."
            return
        }
        if (!validateEmail(email)) {
            _error.value = "Por favor, ingresa un correo electrónico válido."
            return
        }
        if (pass.length < 6) {
            _error.value = "La contraseña debe tener al menos 6 caracteres."
            return
        }
        if (pass != confirmPass) {
            _error.value = "Las contraseñas no coinciden."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.register(
                email = email,
                pass = pass,
                nombre = nombre
            )
            result.onSuccess {
                _userData.value = it
            }.onFailure {
                _error.value = translateError(it)
            }
            _isLoading.value = false
        }
    }

    fun loadUsersForAdmin() {
        viewModelScope.launch {
            _adminLoading.value = true
            _adminError.value = null
            repository.getAllUsers()
                .onSuccess { _managedUsers.value = it }
                .onFailure { _adminError.value = translateError(it) }
            _adminLoading.value = false
        }
    }

    fun createAdmin(
        nombre: String,
        email: String,
        pass: String,
        confirmPass: String
    ) {
        if (nombre.isBlank() || email.isBlank() || pass.isBlank() || confirmPass.isBlank()) {
            _adminError.value = "Completa todos los datos del nuevo administrador."
            return
        }
        if (!validateEmail(email)) {
            _adminError.value = "Ingresa un correo electrónico válido."
            return
        }
        if (pass.length < 6) {
            _adminError.value = "La contraseña debe tener al menos 6 caracteres."
            return
        }
        if (pass != confirmPass) {
            _adminError.value = "Las contraseñas no coinciden."
            return
        }

        viewModelScope.launch {
            _adminLoading.value = true
            _adminError.value = null
            _adminMessage.value = null

            repository.createAdmin(nombre, email, pass)
                .onSuccess { newAdmin ->
                    _adminMessage.value = "Administrador ${newAdmin.correo} creado correctamente."
                    repository.getAllUsers()
                        .onSuccess { _managedUsers.value = it }
                        .onFailure { _adminError.value = translateError(it) }
                }
                .onFailure {
                    _adminError.value = translateError(it)
                }

            _adminLoading.value = false
        }
    }

    fun toggleAdminRole(user: UserDto) {
        val newRole = if (user.rol == AppConstants.ROLE_ADMIN) {
            AppConstants.ROLE_CLIENT
        } else {
            AppConstants.ROLE_ADMIN
        }

        viewModelScope.launch {
            _adminLoading.value = true
            _adminError.value = null
            _adminMessage.value = null

            repository.updateUserRole(user.usuarioId, newRole)
                .onSuccess {
                    _adminMessage.value = if (newRole == AppConstants.ROLE_ADMIN) {
                        "${user.correo} ahora es administrador."
                    } else {
                        "${user.correo} ahora es cliente."
                    }
                    repository.getAllUsers()
                        .onSuccess { _managedUsers.value = it }
                        .onFailure { _adminError.value = translateError(it) }
                }
                .onFailure {
                    _adminError.value = translateError(it)
                }

            _adminLoading.value = false
        }
    }

    fun updateProfileName(nombre: String) {
        val cleanName = nombre.trim()
        if (cleanName.isBlank()) {
            _error.value = "El nombre no puede estar vacío."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _profileMessage.value = null

            repository.updateProfileName(cleanName)
                .onSuccess { updatedUser ->
                    _userData.value = updatedUser
                    _profileMessage.value = "Perfil actualizado correctamente."
                }
                .onFailure {
                    _error.value = translateError(it)
                }

            _isLoading.value = false
        }
    }

    fun clearProfileMessage() {
        _profileMessage.value = null
    }

    fun clearAdminMessages() {
        _adminMessage.value = null
        _adminError.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun logout() {
        repository.logout()
        _userData.value = null
        _managedUsers.value = emptyList()
        _error.value = null
        _profileMessage.value = null
        _adminMessage.value = null
        _adminError.value = null
    }
}
