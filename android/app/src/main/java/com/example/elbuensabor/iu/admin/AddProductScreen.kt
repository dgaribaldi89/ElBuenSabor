package com.example.elbuensabor.iu.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.elbuensabor.data.remote.dto.ProductDto
import com.example.elbuensabor.utils.helpers.ProductImageUtils
import com.example.elbuensabor.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: ProductViewModel,
    onNavigateBack: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Entradas") }
    var expanded by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imagenBase64 by remember { mutableStateOf("") }
    var imageError by remember { mutableStateOf<String?>(null) }
    var processingImage by remember { mutableStateOf(false) }

    val categorias = listOf("Entradas", "Bebidas", "Platos")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            imageError = null
            processingImage = true

            scope.launch {
                ProductImageUtils.uriToBase64(context, uri)
                    .onSuccess {
                        imagenBase64 = it
                        imageError = null
                    }
                    .onFailure {
                        imagenBase64 = ""
                        imageError = it.message ?: "No se pudo procesar la imagen."
                    }
                processingImage = false
            }
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
                title = { Text("Agregar Producto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.Black
                        )
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
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del Producto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = precio,
                onValueChange = { nuevoValor ->
                    precio = nuevoValor.filter { it.isDigit() || it == '.' }
                },
                label = { Text("Precio (S/)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Imagen del producto",
                style = MaterialTheme.typography.titleSmall,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7A834),
                    contentColor = Color.White
                )
            ) {
                Text(if (selectedImageUri == null) "Seleccionar imagen de la galería" else "Cambiar imagen")
            }

            Text(
                text = "La foto se optimiza automáticamente antes de guardarse.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 6.dp)
            )

            if (processingImage) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Procesando imagen...")
                }
            }

            imageError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            selectedImageUri?.let { uri ->
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Vista previa del producto",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF2F2F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No se pudo mostrar la imagen",
                                    color = Color.Red,
                                    textAlign = TextAlign.Center
                                )
                            }
                        },
                        success = { SubcomposeAsyncImageContent() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = categoria,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    categorias.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = Color.Black) },
                            onClick = {
                                categoria = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            val precioConvertido = precio.toDoubleOrNull()
            val formValid = nombre.isNotBlank() &&
                descripcion.isNotBlank() &&
                precioConvertido != null &&
                precioConvertido > 0.0 &&
                imagenBase64.isNotBlank() &&
                !processingImage

            Button(
                onClick = {
                    val p = precio.toDoubleOrNull() ?: return@Button
                    val nuevoProducto = ProductDto(
                        nombre = nombre.trim(),
                        descripcion = descripcion.trim(),
                        precio = p,
                        categoria = categoria,
                        imagenUrl = "",
                        imagenBase64 = imagenBase64
                    )
                    viewModel.saveProduct(nuevoProducto)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = formValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7A834),
                    contentColor = Color.White
                )
            ) {
                Text("Guardar en Menú")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
