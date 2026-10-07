package com.example.elbuensabor.data.repository

import android.content.Context
import com.example.elbuensabor.data.remote.dto.UserDto
import com.example.elbuensabor.utils.constants.AppConstants
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val appContext: Context? = null,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val ADMIN_CREATOR_APP = "elbuensabor-admin-creator"
    }

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    private fun normalizedRole(role: String?): String {
        return if (role?.trim()?.uppercase() == AppConstants.ROLE_ADMIN) {
            AppConstants.ROLE_ADMIN
        } else {
            AppConstants.ROLE_CLIENT
        }
    }

    private fun resolvedRole(email: String, existingRole: String?): String {
        return if (email.trim().lowercase() == AppConstants.ADMIN_EMAIL) {
            AppConstants.ROLE_ADMIN
        } else {
            normalizedRole(existingRole)
        }
    }

    suspend fun getUserData(uid: String): UserDto? {
        return try {
            val document = firestore.collection("usuarios").document(uid).get().await()
            if (document.exists()) document.toObject(UserDto::class.java) else null
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error obteniendo usuario: ${e.message}", e)
            null
        }
    }

    /**
     * Devuelve el perfil de la sesión actual y normaliza solo datos inválidos.
     *
     * Reglas:
     * - admin@elbuensabor.com es el administrador principal y siempre conserva ADMIN.
     * - Los demás usuarios conservan el rol guardado en Firestore (CLIENTE o ADMIN).
     * - Las cuentas nuevas creadas desde Registro nacen como CLIENTE.
     */
    suspend fun getCurrentUserData(): UserDto? {
        val firebaseUser = currentUser ?: return null
        return normalizeAuthenticatedUser(firebaseUser)
    }

    private suspend fun normalizeAuthenticatedUser(firebaseUser: FirebaseUser): UserDto {
        val normalizedEmail = firebaseUser.email?.trim()?.lowercase().orEmpty()
        val existingUser = getUserData(firebaseUser.uid)
        val expectedRole = resolvedRole(normalizedEmail, existingUser?.rol)

        val normalizedUser = if (existingUser != null) {
            existingUser.copy(
                usuarioId = firebaseUser.uid,
                nombre = existingUser.nombre.ifBlank { firebaseUser.displayName.orEmpty() },
                correo = normalizedEmail,
                rol = expectedRole
            )
        } else {
            UserDto(
                usuarioId = firebaseUser.uid,
                nombre = firebaseUser.displayName.orEmpty(),
                correo = normalizedEmail,
                rol = expectedRole
            )
        }

        val needsUpdate = existingUser == null ||
            existingUser.usuarioId != normalizedUser.usuarioId ||
            existingUser.nombre != normalizedUser.nombre ||
            existingUser.correo.trim().lowercase() != normalizedUser.correo ||
            existingUser.rol != normalizedUser.rol

        if (needsUpdate) {
            saveUserToFirestore(normalizedUser)
        }

        return normalizedUser
    }

    suspend fun login(email: String, pass: String): Result<UserDto> {
        return try {
            val normalizedEmail = email.trim().lowercase()
            val result = firebaseAuth
                .signInWithEmailAndPassword(normalizedEmail, pass)
                .await()

            val firebaseUser = result.user ?: throw Exception("Usuario no encontrado")
            Result.success(normalizeAuthenticatedUser(firebaseUser))
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error en login: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * El registro público crea únicamente cuentas CLIENTE.
     * El administrador principal se crea desde Firebase Console la primera vez.
     * Otros administradores pueden ser creados por un ADMIN desde Gestión de usuarios.
     */
    suspend fun register(
        email: String,
        pass: String,
        nombre: String
    ): Result<UserDto> {
        return try {
            val normalizedEmail = email.trim().lowercase()

            if (normalizedEmail == AppConstants.ADMIN_EMAIL) {
                throw IllegalArgumentException(
                    "Este correo está reservado para el administrador principal. Usa otro correo."
                )
            }

            val result = firebaseAuth
                .createUserWithEmailAndPassword(normalizedEmail, pass)
                .await()

            val firebaseUser = result.user ?: throw Exception("Error al crear usuario")

            val userData = UserDto(
                usuarioId = firebaseUser.uid,
                nombre = nombre.trim(),
                correo = normalizedEmail,
                rol = AppConstants.ROLE_CLIENT
            )

            firebaseUser.updateProfile(
                userProfileChangeRequest { displayName = nombre.trim() }
            ).await()

            try {
                saveUserToFirestore(userData)
            } catch (e: Exception) {
                try {
                    firebaseUser.delete().await()
                } catch (_: Exception) {
                }
                throw Exception("No se pudo guardar el perfil en Firebase. Inténtalo nuevamente.", e)
            }

            Result.success(userData)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error en registro: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Crea una cuenta ADMIN sin cerrar la sesión del administrador actual.
     * Para ello se usa una instancia secundaria de FirebaseAuth.
     */
    suspend fun createAdmin(
        nombre: String,
        email: String,
        pass: String
    ): Result<UserDto> {
        return try {
            ensureCurrentUserIsAdmin()

            val context = appContext
                ?: throw IllegalStateException("No se pudo inicializar el creador de administradores.")

            val normalizedEmail = email.trim().lowercase()
            val cleanName = nombre.trim()

            if (cleanName.isBlank()) {
                throw IllegalArgumentException("Ingresa el nombre del administrador.")
            }
            if (normalizedEmail.isBlank()) {
                throw IllegalArgumentException("Ingresa el correo del administrador.")
            }
            if (normalizedEmail == AppConstants.ADMIN_EMAIL) {
                throw IllegalArgumentException("El administrador principal ya tiene un correo reservado.")
            }
            if (pass.length < 6) {
                throw IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.")
            }

            val secondaryApp = try {
                FirebaseApp.getInstance(ADMIN_CREATOR_APP)
            } catch (_: IllegalStateException) {
                FirebaseApp.initializeApp(
                    context,
                    FirebaseApp.getInstance().options,
                    ADMIN_CREATOR_APP
                )
            }

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
            secondaryAuth.signOut()

            var createdUser: FirebaseUser? = null
            try {
                val createResult = secondaryAuth
                    .createUserWithEmailAndPassword(normalizedEmail, pass)
                    .await()

                createdUser = createResult.user
                    ?: throw IllegalStateException("Firebase no devolvió el nuevo usuario.")

                createdUser.updateProfile(
                    userProfileChangeRequest { displayName = cleanName }
                ).await()

                val userData = UserDto(
                    usuarioId = createdUser.uid,
                    nombre = cleanName,
                    correo = normalizedEmail,
                    rol = AppConstants.ROLE_ADMIN
                )

                try {
                    firestore.collection("usuarios")
                        .document(createdUser.uid)
                        .set(userData)
                        .await()
                } catch (e: Exception) {
                    try {
                        createdUser.delete().await()
                    } catch (_: Exception) {
                    }
                    throw Exception(
                        "La cuenta se creó en Authentication, pero no se pudo guardar como ADMIN. Se intentó revertir la creación.",
                        e
                    )
                }

                Result.success(userData)
            } finally {
                secondaryAuth.signOut()
            }
        } catch (e: FirebaseAuthException) {
            android.util.Log.e("AuthRepository", "Error creando admin: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error creando admin: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<UserDto>> {
        return try {
            ensureCurrentUserIsAdmin()
            val snapshot = firestore.collection("usuarios").get().await()
            val users = snapshot.toObjects(UserDto::class.java)
                .map { user ->
                    user.copy(
                        correo = user.correo.trim().lowercase(),
                        rol = resolvedRole(user.correo, user.rol)
                    )
                }
                .sortedWith(
                    compareByDescending<UserDto> { it.rol == AppConstants.ROLE_ADMIN }
                        .thenBy { it.nombre.lowercase() }
                        .thenBy { it.correo }
                )
            Result.success(users)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error listando usuarios: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: String, newRole: String): Result<Unit> {
        return try {
            ensureCurrentUserIsAdmin()

            val cleanRole = normalizedRole(newRole)
            val target = firestore.collection("usuarios").document(userId).get().await()
            if (!target.exists()) {
                throw IllegalArgumentException("El usuario seleccionado ya no existe.")
            }

            val targetUser = target.toObject(UserDto::class.java)
                ?: throw IllegalArgumentException("No se pudo leer el usuario seleccionado.")
            val targetEmail = targetUser.correo.trim().lowercase()

            if (targetEmail == AppConstants.ADMIN_EMAIL && cleanRole != AppConstants.ROLE_ADMIN) {
                throw IllegalArgumentException("El administrador principal no puede convertirse en cliente.")
            }

            val currentUid = currentUser?.uid
            if (currentUid == userId && cleanRole != AppConstants.ROLE_ADMIN) {
                throw IllegalArgumentException("No puedes quitarte tu propio rol de administrador.")
            }

            firestore.collection("usuarios")
                .document(userId)
                .update("rol", cleanRole)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error actualizando rol: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateProfileName(nombre: String): Result<UserDto> {
        return try {
            val firebaseUser = currentUser ?: throw IllegalStateException("No hay una sesión activa.")
            val cleanName = nombre.trim()
            if (cleanName.isBlank()) {
                throw IllegalArgumentException("El nombre no puede estar vacío.")
            }

            val currentData = normalizeAuthenticatedUser(firebaseUser)

            firebaseUser.updateProfile(
                userProfileChangeRequest { displayName = cleanName }
            ).await()

            val updatedData = currentData.copy(nombre = cleanName)
            saveUserToFirestore(updatedData)
            Result.success(updatedData)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error actualizando perfil: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun ensureCurrentUserIsAdmin() {
        val firebaseUser = currentUser
            ?: throw IllegalStateException("Debes iniciar sesión.")

        val email = firebaseUser.email?.trim()?.lowercase().orEmpty()
        if (email == AppConstants.ADMIN_EMAIL) return

        val data = firestore.collection("usuarios")
            .document(firebaseUser.uid)
            .get()
            .await()
            .toObject(UserDto::class.java)

        if (normalizedRole(data?.rol) != AppConstants.ROLE_ADMIN) {
            throw SecurityException("Esta opción es exclusiva para administradores.")
        }
    }

    private suspend fun saveUserToFirestore(user: UserDto) {
        firestore.collection("usuarios")
            .document(user.usuarioId)
            .set(user)
            .await()
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}
