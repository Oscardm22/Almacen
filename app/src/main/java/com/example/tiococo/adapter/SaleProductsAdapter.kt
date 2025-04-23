package com.example.tiococo.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.text.InputFilter
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tiococo.R
import com.example.tiococo.data.model.Product

class SaleProductsAdapter(
    private val onRemoveClick: (Product) -> Unit,
    private val onQuantityChange: (Product, Int) -> Unit,
    private var exchangeRate: Double = 1.0 // Nuevo parámetro para la tasa de cambio
) : ListAdapter<Product, SaleProductsAdapter.SaleProductViewHolder>(SaleProductDiffCallback()) {

    // Objeto para identificar cambios en la tasa
    private object PayloadExchangeRateChange

    inner class SaleProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotal)
        private val etQuantity: EditText = itemView.findViewById(R.id.etQuantity)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)
        private var currentProduct: Product? = null
        private var textWatcher: TextWatcher? = null

        fun bind(product: Product) {
            currentProduct = product
            textWatcher?.let { etQuantity.removeTextChangedListener(it) }

            // Configuración inicial
            tvProductName.text = product.name
            updatePriceDisplay(product.priceDollars, product.quantity)

            // Configuración del EditText
            etQuantity.apply {
                filters = arrayOf(InputFilter.LengthFilter(3))
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(product.quantity.toString())

                // Seleccionar automáticamente si el valor es 1
                if (text.toString() == "1") {
                    setSelectAllOnFocus(true) // Selección automática solo para "1"
                } else {
                    setSelectAllOnFocus(false)
                    setSelection(text.length) // Cursor al final para otros valores
                }

                // Manejar cambios de foco
                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus && text.toString() != "1") {
                        post { setSelection(text.length) }
                    }
                }
            }

            // Configurar TextWatcher
            textWatcher = object : TextWatcher {
                private var isUserEditing = false

                override fun afterTextChanged(s: Editable?) {
                    if (isUserEditing) {
                        s?.let {
                            val newQuantity = it.toString().toIntOrNull()?.coerceIn(1, 999) ?: 1
                            if (it.toString() != newQuantity.toString()) {
                                etQuantity.apply {
                                    removeTextChangedListener(this@SaleProductViewHolder.textWatcher)
                                    setText(newQuantity.toString())
                                    // Actualizar configuración de selección
                                    if (newQuantity == 1) {
                                        setSelectAllOnFocus(true)
                                        selectAll()
                                    } else {
                                        setSelectAllOnFocus(false)
                                        setSelection(text.length)
                                    }
                                    addTextChangedListener(this@SaleProductViewHolder.textWatcher)
                                }
                            }

                            currentProduct?.let { product ->
                                onQuantityChange(product, newQuantity)
                                updatePriceDisplay(product.priceDollars, newQuantity)
                            }
                        }
                        isUserEditing = false
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    isUserEditing = true
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }

            etQuantity.addTextChangedListener(textWatcher)

            btnRemove.setOnClickListener {
                currentProduct?.let { onRemoveClick(it) }
            }
        }

        internal fun updatePriceDisplay(priceDollars: Double, quantity: Int) {
            val totalDollars = priceDollars * quantity
            tvSubtotal.text = itemView.context.getString(
                R.string.Productos_venta, // Usando el nuevo nombre
                totalDollars
            )
        }

        fun clear() {
            textWatcher?.let { etQuantity.removeTextChangedListener(it) }
        }
    }

    // Método para actualizar la tasa de cambio
    fun updateExchangeRate(newRate: Double) {
        if (exchangeRate != newRate) {
            exchangeRate = newRate
            notifyItemRangeChanged(0, itemCount, PayloadExchangeRateChange)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_sale, parent, false)
        return SaleProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaleProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: SaleProductViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads[0] == PayloadExchangeRateChange) {
            val product = getItem(position)
            holder.updatePriceDisplay(product.priceDollars, product.quantity)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onViewRecycled(holder: SaleProductViewHolder) {
        holder.clear()
        super.onViewRecycled(holder)
    }
}

class SaleProductDiffCallback : DiffUtil.ItemCallback<Product>() {
    override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
        return oldItem == newItem
    }
}