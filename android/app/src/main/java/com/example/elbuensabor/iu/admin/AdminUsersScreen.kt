package com.example.elbuensabor.iu.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.elbuensabor.data.remote.dto.UserDto
import com.example.elbuensabor.utils.constants.AppConstants
import com.example.elbuensabor.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val users by viewModel.managedUsers.collectAsState()
    val currentUser by viewModel.userData.collectAsState()
    val isLoading by viewModel.adminLoading.collectAsState()
    val message by viewModel.adminMessage.collectAsState()
    val error by viewModel.adminError.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.clearAdminMessages()
        viewModel.loadUsersForAdmin()
    }

    LaunchedEffect(message) {
        if (message?.startsWith("Administrador ") == true &&
            message?.contains("creado correctamente") == true
        ) {
            nombre = ""
            email = ""
            password = ""
            confirmPassword = ""
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                ),
                title = { Text("Gestión de usuarios") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E8)),
                    border = BorderStroke(1.dp, Color(0xFFE7A834))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Crear otro administrador",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "La cuenta se crea directamente en Firebase Authentication y se registra con rol ADMIN sin cerrar tu sesión actual.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )

                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre completo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo electrónico") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading,
                            visualTransformation = if (passwordVisible) {
                                androidx.compose.ui.text.input.VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                TextButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    enabled = !isLoading
                                ) {
                                    Text(if (passwordVisible) "Ocultar" else "Ver")
                                }
                            }
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirmar contraseña") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading,
                            visualTransformation = PasswordVisualTransformation()
                        )

                        Button(
                            onClick = {
                                viewModel.createAdmin(
                                    nombre = nombre,
                                    email = email,
                                    pass = password,
                                    confirmPass = confirmPassword
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Text("Crear administrador")
                        }
                    }
                }
            }

            if (message != null) {
                item {
                    Text(
                        text = message.orEmpty(),
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (error != null) {
                item {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Usuarios registrados (${users.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    TextButton(
                        onClick = { viewModel.loadUsersForAdmin() },
                        enabled = !isLoading
                    ) {
                        Text("Actualizar")
                    }
                }
            }

            if (isLoading && users.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (users.isEmpty()) {
                item {
                    Text(
                        text = "No se encontraron usuarios en Firestore.",
                        color = Color.Gray
                    )
                }
            } else {
                items(users, key = { it.usuarioId }) { user ->
                    UserRoleCard(
                        user = user,
                        currentUserId = currentUser?.usuarioId.orEmpty(),
                        isLoading = isLoading,
                        onToggleRole = { viewModel.toggleAdminRole(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserRoleCard(
    user: UserDto,
    currentUserId: String,
    isLoading: Boolean,
    onToggleRole: () -> Unit
) {
    val isAdmin = user.rol == AppConstants.ROLE_ADMIN
    val isPrimaryAdmin = user.correo.trim().lowercase() == AppConstants.ADMIN_EMAIL
    val isCurrentUser = user.usuarioId == currentUserId
    val canChangeRole = !isPrimaryAdmin && !(isCurrentUser && isAdmin)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = user.nombre.ifBlank { "Sin nombre" },
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = user.correo,
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )
            Text(
                text = when {
                    isPrimaryAdmin -> "Rol: ADMIN · administrador principal"
                    isAdmin -> "Rol: ADMIN"
                    else -> "Rol: CLIENTE"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isAdmin) Color(0xFF8A5A00) else Color.DarkGray
            )

            if (canChangeRole) {
                OutlinedButton(
                    onClick = onToggleRole,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isAdmin) "Convertir en cliente" else "Hacer administrador")
                }
            } else if (isPrimaryAdmin) {
                Text(
                    text = "El administrador principal no puede ser degradado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            } else if (isCurrentUser) {
                Text(
                    text = "No puedes quitarte tu propio rol mientras estás conectado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
