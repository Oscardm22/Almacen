package com.example.tiococo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tiococo.data.managers.ExchangeRateManager
import com.example.tiococo.data.model.Product
import com.example.tiococo.data.model.SaleRecord
import kotlinx.coroutines.launch
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.tiococo.data.repository.SaleRepository
import com.example.tiococo.data.repository.ProductRepository

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    // Cambia de Pair<String, Product> a Product
    private var originalProductList: List<Product> = emptyList()
    private val _products = MutableLiveData<List<Product>>(emptyList())
    val products: LiveData<List<Product>> = _products



    // Estados para la actualización de tasa
    enum class RateState { IDLE, LOADING, SUCCESS, ERROR }

    private val _rateState = MutableLiveData<RateState>(RateState.IDLE)
    val rateState: LiveData<RateState> = _rateState

    // Cambia saleProducts también
    private val _saleProducts = MutableLiveData<List<Product>>(emptyList())
    val saleProducts: LiveData<List<Product>> = _saleProducts

    private val _totalAmount = MutableLiveData<Double>(0.0)
    val totalAmount: LiveData<Double> = _totalAmount

    private val _salesHistory = MutableLiveData<List<SaleRecord>>(emptyList())
    val salesHistory: LiveData<List<SaleRecord>> = _salesHistory

    // Sistema de tasas
    private val _exchangeRate = MutableLiveData<Double>(36.0)
    val exchangeRate: LiveData<Double> = _exchangeRate

    private val _rateUpdateMessage = MutableLiveData<String?>()
    val rateUpdateMessage: LiveData<String?> = _rateUpdateMessage

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    // Declara primero ProductRepository
    private val productRepository: ProductRepository = ProductRepository()
    // Luego instancia SaleRepository con la dependencia
    private val saleRepository: SaleRepository = SaleRepository(productRepository)

    init {
        loadInitialData()
        refreshExchangeRate()
        loadSalesHistory()
    }

    // En ProductViewModel.kt
    private fun loadInitialData() {
        viewModelScope.launch {
            Log.d("ProductVM", "Cargando productos iniciales")
            productRepository.getProducts().collect { productos ->
                Log.d("ProductVM", "Productos recibidos: ${productos.size}")
                originalProductList = productos
                _products.postValue(productos)
            }
        }
    }

    fun refreshExchangeRate() {
        _rateState.value = RateState.LOADING
        viewModelScope.launch {
            try {
                val rateResult = ExchangeRateManager.getCurrentRateWithStatus(getApplication(), forceUpdate = true)
                _exchangeRate.value = rateResult.rate

                updateAllPrices(rateResult.rate)
                _rateState.value = RateState.SUCCESS

                // Mensaje contextual
                _rateUpdateMessage.value = if (rateResult.isFromCache) {
                    "Tasa almacenada: ${formatRate(rateResult.rate)} (sin conexión)"
                } else {
                    "Tasa actualizada: ${formatRate(rateResult.rate)}"
                }
            } catch (e: Exception) {
                _rateState.value = RateState.ERROR
                _rateUpdateMessage.value = "Error: ${e.message ?: "No se pudo actualizar"}"
            }
        }
    }

    private fun updateAllPrices(newRate: Double) {
        if (_exchangeRate.value != newRate) { // Solo actualizar si cambió
            _exchangeRate.value = newRate

            // Forzar actualización de las listas de forma eficiente
            _products.value = _products.value?.let { ArrayList(it) } // Crea nueva instancia
            _saleProducts.value = _saleProducts.value?.let { ArrayList(it) }
        }
    }

    private fun formatRate(rate: Double): String {
        return "%.2f".format(rate)
    }

    // Funciones CRUD
    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    fun addProduct(newProduct: Product) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val id = productRepository.saveProduct(newProduct)
                val updatedProduct = newProduct.copy(id = id)
                val updatedList = _products.value.orEmpty().toMutableList()
                updatedList.add(updatedProduct)
                _products.value = updatedList
                _saveSuccess.value = true
            } catch (e: Exception) {
                _saveSuccess.value = false
                Log.e("ProductVM", "Error al guardar producto", e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateProduct(id: String, updatedProduct: Product) {
        viewModelScope.launch {
            productRepository.updateProduct(id, updatedProduct)
            // Actualizar lista local
            _products.value = _products.value?.map {
                if (it.id == id) updatedProduct.copy(id = id) else it
            }
        }
    }



    fun deleteProduct(id: String) {
        viewModelScope.launch {
            productRepository.deleteProduct(id)
        }
    }



    // Funciones de Ventas
    fun addToSale(product: Product) {
        val currentSale = _saleProducts.value?.toMutableList() ?: mutableListOf()
        if (currentSale.none { it.id == product.id }) {
            currentSale.add(product.copy(quantity = 1)) // Cantidad inicial 1
            _saleProducts.value = currentSale
            calculateTotal()
        }
    }

    fun removeFromSale(product: Product) {
        _saleProducts.value = _saleProducts.value?.filter { it.id != product.id }
        calculateTotal()
    }

    fun updateSaleProductQuantity(product: Product, newQuantity: Int) {
        _saleProducts.value = _saleProducts.value?.map {
            if (it.id == product.id) it.copy(quantity = newQuantity) else it
        }
        calculateTotal()
    }

    fun clearSale() {
        _saleProducts.value = emptyList()
        _totalAmount.value = 0.0
    }

    // Historial de Ventas
    fun loadSalesHistory() {
        viewModelScope.launch {
            saleRepository.getSalesHistory().collect { sales ->
                _salesHistory.postValue(sales)
            }
        }
    }

    fun deleteSaleRecord(saleId: String) {
        viewModelScope.launch {
            saleRepository.deleteSale(saleId)
            loadSalesHistory() // Actualiza la lista tras eliminación
        }
    }
    fun returnSale(sale: SaleRecord, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = saleRepository.returnSaleAndRestoreStock(sale)
                if (success) {
                    loadSalesHistory() // Refrescar el historial
                }
                onComplete(success)
            } catch (e: Exception) {
                Log.e("ProductVM", "Error al procesar devolución", e)
                onComplete(false)
            }
        }
    }

    // Búsqueda y filtrado
    fun filterSalesByDate(query: String): List<SaleRecord> {
        return _salesHistory.value?.filter { sale ->
            sale.date.contains(query, ignoreCase = true)
        } ?: emptyList()
    }

    fun clearSalesHistory() {
        viewModelScope.launch {
            saleRepository.deleteAllSales { success ->
                if (success) {
                    _salesHistory.value = emptyList()
                } else {
                    Log.e("ProductVM", "Error al eliminar historial de ventas")
                }
            }
        }
    }


    fun searchProducts(query: String) {
        _products.value = if (query.isEmpty()) {
            originalProductList
        } else {
            originalProductList.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
    }

    fun clearRateUpdateMessage() {
        _rateUpdateMessage.value = null
    }

    private fun calculateTotal() {
        _totalAmount.value = _saleProducts.value?.sumOf { product ->
            product.priceDollars * product.quantity
        } ?: 0.0
    }
}