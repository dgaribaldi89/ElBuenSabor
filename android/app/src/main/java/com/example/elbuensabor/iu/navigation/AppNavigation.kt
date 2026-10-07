package com.example.elbuensabor.iu.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.elbuensabor.ElBuenSaborApplication
import com.example.elbuensabor.iu.admin.AddProductScreen
import com.example.elbuensabor.iu.admin.AdminOrdersScreen
import com.example.elbuensabor.iu.admin.AdminUsersScreen
import com.example.elbuensabor.iu.admin.EditProductScreen
import com.example.elbuensabor.iu.auth.LoginScreen
import com.example.elbuensabor.iu.auth.RegisterScreen
import com.example.elbuensabor.iu.cart.CartScreen
import com.example.elbuensabor.iu.menu.MenuScreen
import com.example.elbuensabor.iu.order.CheckoutScreen
import com.example.elbuensabor.iu.order.MyOrdersScreen
import com.example.elbuensabor.iu.profile.ProfileScreen
import com.example.elbuensabor.utils.constants.AppConstants
import com.example.elbuensabor.viewmodel.AuthViewModel
import com.example.elbuensabor.viewmodel.AuthViewModelFactory
import com.example.elbuensabor.viewmodel.CartViewModel
import com.example.elbuensabor.viewmodel.CartViewModelFactory
import com.example.elbuensabor.viewmodel.OrderViewModel
import com.example.elbuensabor.viewmodel.OrderViewModelFactory
import com.example.elbuensabor.viewmodel.ProductViewModel
import com.example.elbuensabor.viewmodel.ProductViewModelFactory

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Menu : Screen("menu")
    object Profile : Screen("profile")
    object AddProduct : Screen("add_product")
    object EditProduct : Screen("edit_product")
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")
    object MyOrders : Screen("my_orders")
    object AdminOrders : Screen("admin_orders")
    object AdminUsers : Screen("admin_users")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val appContext = LocalContext.current.applicationContext as ElBuenSaborApplication
    val uiContext = LocalContext.current

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(appContext.authRepository)
    )
    val productViewModel: ProductViewModel = viewModel(
        factory = ProductViewModelFactory(appContext.productRepository)
    )
    val cartViewModel: CartViewModel = viewModel(
        factory = CartViewModelFactory(appContext.cartRepository)
    )
    val orderViewModel: OrderViewModel = viewModel(
        factory = OrderViewModelFactory(appContext.orderRepository)
    )

    val user by authViewModel.userData.collectAsState()
    val selectedProduct by productViewModel.selectedProduct.collectAsState()

    LaunchedEffect(user?.usuarioId) {
        cartViewModel.setUserId(user?.usuarioId)
    }

    val startDestination = if (user != null) Screen.Menu.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Menu.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Menu.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Menu.route) {
            MenuScreen(
                viewModel = productViewModel,
                authViewModel = authViewModel,
                cartViewModel = cartViewModel,
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToAddProduct = { navController.navigate(Screen.AddProduct.route) },
                onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                onNavigateToMyOrders = { navController.navigate(Screen.MyOrders.route) },
                onNavigateToAdminOrders = { navController.navigate(Screen.AdminOrders.route) },
                onNavigateToAdminUsers = { navController.navigate(Screen.AdminUsers.route) },
                onNavigateToEditProduct = { product ->
                    productViewModel.selectProduct(product)
                    navController.navigate(Screen.EditProduct.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.AddProduct.route) {
            if (user?.rol == AppConstants.ROLE_ADMIN) {
                AddProductScreen(
                    viewModel = productViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Screen.EditProduct.route) {
            if (user?.rol == AppConstants.ROLE_ADMIN) {
                if (selectedProduct != null) {
                    EditProductScreen(
                        product = selectedProduct!!,
                        viewModel = productViewModel,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Screen.Cart.route) {
            if (user != null) {
                CartScreen(
                    viewModel = cartViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCheckout = { navController.navigate(Screen.Checkout.route) }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(Screen.Checkout.route) {
            val cartItems by cartViewModel.cartItems.collectAsState()
            val total by cartViewModel.total.collectAsState()
            val orderState by orderViewModel.orderState.collectAsState()
            val isProcessing by orderViewModel.isLoading.collectAsState()

            LaunchedEffect(orderState) {
                orderState?.onSuccess {
                    cartViewModel.clearCart()
                    orderViewModel.resetOrderState()
                    Toast.makeText(uiContext, "Pedido realizado con éxito", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.MyOrders.route) {
                        popUpTo(Screen.Cart.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }?.onFailure { error ->
                    Toast.makeText(
                        uiContext,
                        "No se pudo registrar el pedido: ${error.localizedMessage ?: "error desconocido"}",
                        Toast.LENGTH_LONG
                    ).show()
                    orderViewModel.resetOrderState()
                }
            }

            if (user != null) {
                CheckoutScreen(
                    total = total,
                    isProcessing = isProcessing,
                    onOrderPlaced = { lat, lon, address ->
                        val currentUser = user
                        when {
                            currentUser == null -> {
                                Toast.makeText(
                                    uiContext,
                                    "Debes iniciar sesión para realizar el pedido.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            cartItems.isEmpty() -> {
                                Toast.makeText(
                                    uiContext,
                                    "El carrito está vacío.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                orderViewModel.placeOrder(
                                    usuarioId = currentUser.usuarioId,
                                    total = total,
                                    lat = lat,
                                    lon = lon,
                                    address = address,
                                    cartItems = cartItems
                                )
                            }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(Screen.MyOrders.route) {
            if (user != null) {
                MyOrdersScreen(
                    viewModel = orderViewModel,
                    usuarioId = user!!.usuarioId,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(Screen.AdminOrders.route) {
            if (user?.rol == AppConstants.ROLE_ADMIN) {
                AdminOrdersScreen(
                    viewModel = orderViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Screen.AdminUsers.route) {
            if (user?.rol == AppConstants.ROLE_ADMIN) {
                AdminUsersScreen(
                    viewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
    }
}
