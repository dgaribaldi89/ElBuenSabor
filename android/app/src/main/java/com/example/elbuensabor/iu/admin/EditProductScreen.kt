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
fun EditProductScreen(
    product: ProductDto,
    viewModel: ProductViewModel,
    onBack: () -> Unit
) {
    var nombre by remember(product.productoId) { mutableStateOf(product.nombre) }
    var descripcion by remember(product.productoId) { mutableStateOf(product.descripcion) }
    var precio by remember(product.productoId) { mutableStateOf(product.precio.toString()) }
    var categoria by remember(product.productoId) { mutableStateOf(product.categoria) }
    var imagenUrl by remember(product.productoId) { mutableStateOf(product.imagenUrl) }
    var imagenBase64 by remember(product.productoId) { mutableStateOf(product.imagenBase64) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var processingImage by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val categorias = listOf("Entradas", "Bebidas", "Platos")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val existingBitmap = remember(imagenBase64) {
        ProductImageUtils.base64ToBitmap(imagenBase64)
    }

    val previewData: Any? = selectedImageUri
        ?: existingBitmap
        ?: imagenUrl.trim().ifBlank { null }

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
                        imagenUrl = ""
                        imageError = null
                    }
                    .onFailure {
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
                title = { Text("Editar Producto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

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

            Text(
                text = "Imagen del producto",
                style = MaterialTheme.typography.titleSmall,
                color = Color.DarkGray
            )

            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7A834),
                    contentColor = Color.White
                )
            ) {
                Text(if (previewData == null) "Seleccionar imagen de la galería" else "Cambiar imagen")
            }

            if (processingImage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (previewData != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(previewData)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Vista previa de $nombre",
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

                TextButton(
                    onClick = {
                        selectedImageUri = null
                        imagenBase64 = ""
                        imagenUrl = ""
                        imageError = null
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Quitar imagen")
                }
            }

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

            Spacer(modifier = Modifier.height(12.dp))

            val precioConvertido = precio.toDoubleOrNull()
            val hasImage = imagenBase64.isNotBlank() || imagenUrl.isNotBlank()
            val formValid = nombre.isNotBlank() &&
                descripcion.isNotBlank() &&
                precioConvertido != null &&
                precioConvertido > 0.0 &&
                hasImage &&
                !processingImage

            Button(
                onClick = {
                    val p = precio.toDoubleOrNull() ?: return@Button
                    val updatedProduct = product.copy(
                        nombre = nombre.trim(),
                        descripcion = descripcion.trim(),
                        precio = p,
                        imagenUrl = imagenUrl.trim(),
                        imagenBase64 = imagenBase64,
                        categoria = categoria
                    )
                    viewModel.updateProduct(updatedProduct)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = formValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7A834),
                    contentColor = Color.White
                )
            ) {
                Text("Guardar cambios")
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
            ) {
                Text("Cancelar")
            }
        }
    }
}
