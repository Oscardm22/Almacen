package com.example.tiococo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tiococo.R
import com.example.tiococo.data.model.Product

class SaleProductDetailAdapter : ListAdapter<Product, SaleProductDetailAdapter.ViewHolder>(ProductDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvDetails: TextView = itemView.findViewById(R.id.tvProductDetails)
        private val context = itemView.context

        fun bind(product: Product) {
            tvName.text = product.name

            // Formatear precios usando recursos
            val unitPrice = context.getString(R.string.currency_usd, product.priceDollars)
            val totalPrice = context.getString(R.string.currency_usd, product.priceDollars * product.quantity)

            // Usar un solo string resource con placeholders
            tvDetails.text = context.getString(
                R.string.product_detail_format,
                product.quantity,  // %1$d (cantidad)
                unitPrice,         // %2$s (precio unitario formateado)
                totalPrice         // %3$s (total formateado)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
    override fun areItemsTheSame(oldItem: Product, newItem: Product) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Product, newItem: Product) = oldItem == newItem
}