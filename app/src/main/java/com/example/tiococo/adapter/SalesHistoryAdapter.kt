package com.example.tiococo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tiococo.R
import com.example.tiococo.data.model.SaleRecord

class SalesHistoryAdapter(
    private val onItemClick: (SaleRecord) -> Unit,
    private val onDeleteClick: (String, (Boolean) -> Unit) -> Unit
) : ListAdapter<SaleRecord, SalesHistoryAdapter.ViewHolder>(SaleHistoryDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvSaleDate)
        private val tvTotalUsd: TextView = itemView.findViewById(R.id.tvTotalUsd)
        private val tvTotalBs: TextView = itemView.findViewById(R.id.tvTotalBs)
        private val tvItemsCount: TextView = itemView.findViewById(R.id.tvItemsCount)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteSale)
        private var currentSale: SaleRecord? = null

        init {
            itemView.setOnClickListener {
                currentSale?.let { sale -> onItemClick(sale) }
            }
        }

        fun bind(sale: SaleRecord) {
            currentSale = sale

            // Fecha y cantidad de productos (se mantienen igual)
            tvDate.text = sale.date
            tvItemsCount.text = itemView.context.getString(R.string.products_count, sale.products.size)

            // Mostrar ambos montos (USD y Bs)
            tvTotalUsd.text = itemView.context.getString(
                R.string.total_usd_format,
                sale.totalDollars // Cambiado de 'total' a 'totalDollars'
            )

            tvTotalBs.text = itemView.context.getString(
                R.string.total_bs_format,
                sale.totalBs // Usamos la propiedad calculada
            )

            // Resto del código para el botón de eliminar (se mantiene igual)
            btnDelete.isEnabled = true
            itemView.isSelected = false
            itemView.alpha = 1f

            btnDelete.setOnClickListener {
                btnDelete.isEnabled = false
                onDeleteClick(sale.id) { isConfirmed ->
                    btnDelete.isEnabled = true
                    if (!isConfirmed) {
                        itemView.alpha = 1f
                        itemView.isSelected = false
                    }
                }
            }
        }
    }

    class SaleHistoryDiffCallback : DiffUtil.ItemCallback<SaleRecord>() {
        override fun areItemsTheSame(oldItem: SaleRecord, newItem: SaleRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SaleRecord, newItem: SaleRecord): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sale_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.clearAnimation()
        holder.bind(getItem(position))

        // Opcional: Agrega animación al aparecer
        holder.itemView.alpha = 0f
        holder.itemView.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }
}