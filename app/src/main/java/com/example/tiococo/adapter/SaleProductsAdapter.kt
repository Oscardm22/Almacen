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
    private val onQuantityChange: (Product, Int) -> Unit
) : ListAdapter<Product, SaleProductsAdapter.SaleProductViewHolder>(SaleProductDiffCallback()) {

    inner class SaleProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val etQuantity: EditText = itemView.findViewById(R.id.etQuantity)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)
        private var currentProduct: Product? = null
        private var textWatcher: TextWatcher? = null
        private var isFirstChange = true

        fun bind(product: Product) {
            // 1. Configuración inicial
            currentProduct = product
            textWatcher?.let { etQuantity.removeTextChangedListener(it) }

            // 2. Mostrar datos del producto
            tvProductName.text = product.name
            etQuantity.setText(product.quantity.toString())

            // 3. Configurar TextWatcher para cambios de cantidad
            textWatcher = object : TextWatcher {
                private var previousText = ""
                private var isFirstChange = true // Bandera para primer cambio

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    previousText = s?.toString() ?: ""
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val newText = s?.toString() ?: ""

                    // 3.1. Manejar campo vacío
                    if (newText.isEmpty()) {
                        etQuantity.setText("0")
                        etQuantity.setSelection(1)
                        return
                    }

                    // 3.2. Validar y procesar cambios
                    if (newText != previousText) {
                        val newQuantity = newText.toIntOrNull() ?: 0
                        val validatedQuantity = newQuantity.coerceIn(0, 999)

                        // 3.3. Corregir si excede límites
                        if (newQuantity != validatedQuantity) {
                            etQuantity.setText(validatedQuantity.toString())
                            etQuantity.setSelection(etQuantity.text?.length ?: 0)
                        }

                        // 3.4. Notificar cambio al ViewModel
                        currentProduct?.let { product ->
                            onQuantityChange(product, validatedQuantity)
                        }

                        // 3.5. Marcar que ya no es la primera modificación
                        isFirstChange = false
                    }
                }
            }

            // 4. Añadir listener de cambios
            etQuantity.addTextChangedListener(textWatcher)

            // 5. Configurar botón de eliminar
            btnRemove.setOnClickListener {
                currentProduct?.let { onRemoveClick(it) }
            }

            // 6. Manejo inteligente del foco
            etQuantity.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    etQuantity.post {
                        when {
                            // 6.1. Seleccionar todo solo para productos nuevos (cantidad 0)
                            product.quantity == 0 && isFirstChange -> etQuantity.selectAll()

                            // 6.2. Para otros casos, cursor al final
                            else -> etQuantity.setSelection(etQuantity.text.length)
                        }
                    }
                }
            }
        }

        fun clear() {
            textWatcher?.let { etQuantity.removeTextChangedListener(it) }
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