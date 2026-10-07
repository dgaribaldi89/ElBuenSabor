package com.example.elbuensabor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elbuensabor.data.repository.ProductRepository
import com.example.elbuensabor.data.remote.dto.ProductDto
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProductViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _products = MutableStateFlow<List<ProductDto>>(emptyList())
    private val _selectedProduct = MutableStateFlow<ProductDto?>(null)
    val products: StateFlow<List<ProductDto>> = _products
    val selectedProduct = _selectedProduct.asStateFlow()

    init {
        loadByCategory("Entradas")
    }

    fun loadByCategory(categoria: String) {
        viewModelScope.launch {
            _loading.value = true
            val remoteProducts = repository.getProductsFromFirestore(categoria)
            _products.value = remoteProducts
            _loading.value = false
        }
    }

    fun saveProduct(product: ProductDto) {
        viewModelScope.launch {
            _loading.value = true
            repository.addProductToFirestore(product)
            loadByCategory(product.categoria)
            _loading.value = false
        }
    }
    fun deleteProduct(productoId: String, categoria: String) {
        viewModelScope.launch {
            _loading.value = true

            repository.deleteProductFromFirestore(productoId)

            loadByCategory(categoria)

            _loading.value = false
        }
    }
    fun updateProduct(product: ProductDto) {
        viewModelScope.launch {
            _loading.value = true

            repository.updateProductFromFirestore(product)

            loadByCategory(product.categoria)

            _loading.value = false
        }
    }
    fun selectProduct(product: ProductDto) {
        _selectedProduct.value = product
    }
}
