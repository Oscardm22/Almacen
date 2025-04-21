package com.example.tiococo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tiococo.data.managers.ExchangeRateManager
import com.example.tiococo.data.model.Product
import com.example.tiococo.data.model.SaleRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    // Estados para la actualización de tasa
    enum class RateState { IDLE, LOADING, SUCCESS, ERROR }

    private val _rateState = MutableLiveData<RateState>(RateState.IDLE)
    val rateState: LiveData<RateState> = _rateState

    // Datos de productos y ventas
    private var originalProductList: List<Product> = emptyList()
    private val _products = MutableLiveData<List<Product>>(emptyList())
    val products: LiveData<List<Product>> = _products

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

    private val _newlyAddedProducts = mutableSetOf<String>()

    private val productRepository = ProductRepository()

    private val saleRepository = SaleRepository()

    init {
        loadInitialData()
        refreshExchangeRate()
    }

    private fun loadInitialData() {
        originalProductList = listOf(
            Product("1", "Arroz", 10, 2.5, calculateBsPrice(2.5)),
            Product("2", "Leche", 5, 3.0, calculateBsPrice(3.0)),
            Product("3", "Azúcar", 8, 1.8, calculateBsPrice(1.8))
        )
        _products.value = originalProductList
    }

    fun generateMockSales() {
        val mockSales = mutableListOf<SaleRecord>()

        // Generamos 5 ventas simuladas
        for (i in 1..5) {
            val sale = SaleRecord(
                id = "sale_$i",
                date = "2025-04-${i + 10}",  // Fechas de ejemplo
                total = (i * 100).toDouble(),
                products = listOf(
                    Product("1", "Arroz", 10, 2.5, 2.5 * i),
                    Product("2", "Leche", 5, 3.0, 3.0 * i)
                )
            )
            mockSales.add(sale)
        }

        // Establecemos las ventas simuladas en el LiveData
        _salesHistory.value = mockSales
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
        // Actualizar productos principales
        originalProductList = originalProductList.map {
            it.copy(priceBolivares = calculateBsPrice(it.priceDollars, newRate))
        }
        _products.value = originalProductList

        // Actualizar productos en venta
        _saleProducts.value = _saleProducts.value?.map {
            it.copy(priceBolivares = calculateBsPrice(it.priceDollars, newRate))
        }
    }

    private fun calculateBsPrice(dollarPrice: Double, rate: Double = _exchangeRate.value ?: 36.0): Double {
        return dollarPrice * rate
    }

    private fun formatRate(rate: Double): String {
        return "%.2f".format(rate)
    }

    // Funciones CRUD
    fun addProduct(newProduct: Product) {
        val productWithBs = newProduct.copy(
            priceBolivares = calculateBsPrice(newProduct.priceDollars)
        )
        originalProductList = originalProductList + productWithBs
        _products.value = originalProductList
        _saveSuccess.value = true
    }

    fun updateProduct(updatedProduct: Product) {
        originalProductList = originalProductList.map {
            if (it.id == updatedProduct.id) updatedProduct.copy(
                priceBolivares = calculateBsPrice(updatedProduct.priceDollars)
            ) else it
        }
        _products.value = originalProductList

        // Actualizar en lista de ventas si existe
        _saleProducts.value = _saleProducts.value?.map {
            if (it.id == updatedProduct.id) updatedProduct.copy(
                priceBolivares = calculateBsPrice(updatedProduct.priceDollars),
                quantity = it.quantity //
            // Mantener la cantidad actual
            ) else it
        }
    }

    fun deleteProduct(productId: String) {
        originalProductList = originalProductList.filter { it.id != productId }
        _products.value = originalProductList
        _saleProducts.value = _saleProducts.value?.filter { it.id != productId }
        calculateTotal()
    }

    // Funciones de Ventas
    fun addToSale(product: Product) {
        val currentSale = _saleProducts.value?.toMutableList() ?: mutableListOf()
        if (currentSale.none { it.id == product.id }) {
            _newlyAddedProducts.add(product.id)
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
    fun registerSale() {
        _saleProducts.value?.takeIf { it.isNotEmpty() }?.let { products ->
            val newSale = SaleRecord(
                id = System.currentTimeMillis().toString(),
                date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
                total = _totalAmount.value ?: 0.0,
                products = products.map { it.copy() } // Copia de los productos
            )

            // Actualizar inventario
            products.forEach { soldProduct ->
                _products.value?.find { it.id == soldProduct.id }?.let { original ->
                    updateProduct(original.copy(
                        quantity = original.quantity - soldProduct.quantity
                    ))
                }
            }

            _salesHistory.value = (_salesHistory.value.orEmpty()) + newSale
            clearSale()
            _saveSuccess.value = true
        }
    }

    fun deleteSaleRecord(saleId: String) {
        _salesHistory.value = _salesHistory.value?.filter { it.id != saleId }
    }

    // Búsqueda y filtrado
    fun filterSalesByDate(query: String): List<SaleRecord> {
        return _salesHistory.value?.filter { sale ->
            sale.date.contains(query, ignoreCase = true)
        } ?: emptyList()
    }

    fun clearSalesHistory() {
        _salesHistory.value = emptyList()
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
        _totalAmount.value = _saleProducts.value?.sumOf {
            it.priceDollars * it.quantity
        } ?: 0.0
    }
}