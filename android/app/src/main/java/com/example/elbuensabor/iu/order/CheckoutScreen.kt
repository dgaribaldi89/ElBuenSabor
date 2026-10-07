package com.example.elbuensabor.iu.order

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    total: Double,
    isProcessing: Boolean,
    onOrderPlaced: (Double, Double, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var direccionText by remember { mutableStateOf("") }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var isLocationCaptured by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf<String?>(null) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun fetchLastLocation() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        isFetching = true
        locationMessage = null

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                        isLocationCaptured = true
                        locationMessage = "Ubicación capturada correctamente."
                    } else {
                        isLocationCaptured = false
                        locationMessage = "No se pudo obtener la ubicación. Activa el GPS e inténtalo nuevamente."
                    }
                    isFetching = false
                }
                .addOnFailureListener {
                    isLocationCaptured = false
                    isFetching = false
                    locationMessage = "No se pudo obtener la ubicación: ${it.localizedMessage ?: "error desconocido"}"
                }
        } catch (_: SecurityException) {
            isFetching = false
            locationMessage = "No se otorgó permiso de ubicación."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchLastLocation()
        } else {
            locationMessage = "Permiso de ubicación denegado. Puedes continuar ingresando la dirección manualmente."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finalizar Pedido") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isProcessing) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Detalles de Entrega",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Total: S/ ${String.format(java.util.Locale.US, "%.2f", total)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = direccionText,
                onValueChange = { direccionText = it },
                label = { Text("Dirección exacta") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej.: Av. Brasil 456, Lima") },
                enabled = !isProcessing,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fetchLastLocation()
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFetching && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                if (isFetching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFetching) "Localizando..." else "Usar mi ubicación GPS")
            }

            locationMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isLocationCaptured) "✓ $message" else message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLocationCaptured) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (isLocationCaptured) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lat: %.5f  Lon: %.5f".format(
                        java.util.Locale.US,
                        latitude,
                        longitude
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    onOrderPlaced(latitude, longitude, direccionText.trim())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = direccionText.isNotBlank() && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Procesando pedido...")
                } else {
                    Text("Confirmar y Realizar Pedido")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "La ubicación GPS es opcional; la dirección escrita sí es obligatoria.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
