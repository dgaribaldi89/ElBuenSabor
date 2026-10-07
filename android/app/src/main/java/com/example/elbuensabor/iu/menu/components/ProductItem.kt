package com.example.elbuensabor.iu.menu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.elbuensabor.data.remote.dto.ProductDto
import com.example.elbuensabor.utils.helpers.ProductImageUtils

@Composable
fun ProductItem(
    product: ProductDto,
    onAddToCart: (ProductDto) -> Unit = {},
    isAdmin: Boolean = false,
    onEdit: (ProductDto) -> Unit = {},
    onDelete: (ProductDto) -> Unit = {}
) {
    val context = LocalContext.current

    val base64Bitmap = remember(product.imagenBase64) {
        ProductImageUtils.base64ToBitmap(product.imagenBase64)
    }

    val imageData: Any? = base64Bitmap
        ?: product.imagenUrl.trim().ifBlank { null }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE7A834)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageData)
                    .crossfade(true)
                    .build(),
                contentDescription = product.nombre,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin imagen",
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                success = {
                    SubcomposeAsyncImageContent()
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = product.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.DarkGray
                )

                Text(
                    text = "S/ %.2f".format(product.precio),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.DarkGray
                )

                if (isAdmin) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onEdit(product) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.DarkGray
                            )
                        ) {
                            Text("Editar")
                        }

                        TextButton(
                            onClick = { onDelete(product) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.DarkGray
                            )
                        ) {
                            Text("Eliminar")
                        }
                    }
                } else {
                    Button(
                        onClick = { onAddToCart(product) },
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text("Agregar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
