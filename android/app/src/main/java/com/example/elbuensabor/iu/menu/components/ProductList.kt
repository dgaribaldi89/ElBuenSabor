package com.example.elbuensabor.iu.menu.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.elbuensabor.data.remote.dto.ProductDto

@Composable
fun ProductList(
    products: List<ProductDto>,
    onAddToCart: (ProductDto) -> Unit,
    isAdmin: Boolean = false,
    onEdit: (ProductDto)-> Unit,
    onDelete: (ProductDto)-> Unit
) {
    if (products.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "No hay productos en esta categoría.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(products) { product ->
                ProductItem(
                    product = product,
                    onAddToCart = onAddToCart,
                    isAdmin = isAdmin,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}
