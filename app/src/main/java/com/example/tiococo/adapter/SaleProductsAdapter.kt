package com.example.tiococo.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
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

            // Mostrar datos del producto
            tvProductName.text = product.name
            updatePriceDisplay(product.priceDollars, product.quantity)
            etQuantity.setText(product.quantity.toString())

            // Configurar TextWatcher
            textWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val newQuantity = s?.toString()?.toIntOrNull() ?: 0
                    currentProduct?.let {
                        onQuantityChange(it, newQuantity)
                        updatePriceDisplay(it.priceDollars, newQuantity)
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
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