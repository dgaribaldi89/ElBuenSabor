package com.example.elbuensabor.iu.menu

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.elbuensabor.data.remote.dto.ProductDto
import com.example.elbuensabor.iu.menu.components.ProductList
import com.example.elbuensabor.utils.constants.AppConstants
import com.example.elbuensabor.viewmodel.AuthViewModel
import com.example.elbuensabor.viewmodel.CartViewModel
import com.example.elbuensabor.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    viewModel: ProductViewModel,
    authViewModel: AuthViewModel,
    cartViewModel: CartViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToMyOrders: () -> Unit,
    onNavigateToAdminOrders: () -> Unit,
    onNavigateToAdminUsers: () -> Unit,
    onNavigateToEditProduct: (ProductDto) -> Unit,
    onLogout: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val user by authViewModel.userData.collectAsState()
    val cartItems by cartViewModel.cartItems.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductDto?>(null) }

    val tabs = listOf("Entradas", "Bebidas", "Platos")
    val view = LocalView.current

    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view)
            .isAppearanceLightStatusBars = true
    }

    LaunchedEffect(selectedTab) {
        viewModel.loadByCategory(tabs[selectedTab])
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "🍔 El Buen Sabor",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Hola, ${user?.nombre ?: "Usuario"} · ${if (user?.rol == AppConstants.ROLE_ADMIN) "Administrador" else "Cliente"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Black
                            )

                            Row {
                                BadgedBox(
                                    badge = {
                                        if (cartItems.isNotEmpty()) {
                                            Badge { Text(cartItems.size.toString()) }
                                        }
                                    }
                                ) {
                                    IconButton(onClick = onNavigateToCart) {
                                        Icon(
                                            Icons.Default.ShoppingCart,
                                            contentDescription = "Carrito",
                                            tint = Color.Black
                                        )
                                    }
                                }

                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = "Perfil",
                                            tint = Color.Black
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        if (user?.rol == AppConstants.ROLE_ADMIN) {
                                            DropdownMenuItem(
                                                text = { Text("Gestionar Pedidos") },
                                                onClick = {
                                                    showMenu = false
                                                    onNavigateToAdminOrders()
                                                },
                                                leadingIcon = { Icon(Icons.Default.List, null) }
                                            )

                                            DropdownMenuItem(
                                                text = { Text("Gestionar Usuarios") },
                                                onClick = {
                                                    showMenu = false
                                                    onNavigateToAdminUsers()
                                                },
                                                leadingIcon = { Icon(Icons.Default.Person, null) }
                                            )
                                        }

                                        DropdownMenuItem(
                                            text = { Text("Mis Pedidos") },
                                            onClick = {
                                                showMenu = false
                                                onNavigateToMyOrders()
                                            },
                                            leadingIcon = { Icon(Icons.Default.List, null) }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Editar Perfil") },
                                            onClick = {
                                                showMenu = false
                                                onNavigateToProfile()
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Cerrar Sesión") },
                                            onClick = {
                                                showMenu = false
                                                authViewModel.logout()
                                                onLogout()
                                            },
                                            leadingIcon = { Icon(Icons.Default.ExitToApp, null) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (user?.rol == AppConstants.ROLE_ADMIN) {
                FloatingActionButton(
                    onClick = onNavigateToAddProduct,
                    containerColor = Color(0xFFE7A834)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Producto")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                ProductList(
                    products = products,
                    onAddToCart = { product -> cartViewModel.addProductToCart(product) },
                    isAdmin = user?.rol == AppConstants.ROLE_ADMIN,
                    onEdit = onNavigateToEditProduct,
                    onDelete = { product ->
                        productToDelete = product
                        showDeleteDialog = true
                    }
                )
            }
        }

        if (showDeleteDialog && productToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    productToDelete = null
                },
                title = { Text("Confirmar eliminación") },
                text = {
                    Text(
                        "¿Estás seguro de que deseas eliminar el producto \"${productToDelete?.nombre}\"? Esta acción no se puede deshacer."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            productToDelete?.let { product ->
                                viewModel.deleteProduct(product.productoId, product.categoria)
                            }
                            showDeleteDialog = false
                            productToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Eliminar", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showDeleteDialog = false
                            productToDelete = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                },
                containerColor = Color.White,
                titleContentColor = Color.Black,
                textContentColor = Color.Black
            )
        }
    }
}
