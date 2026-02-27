package isim.ia2y.myapplication

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OrdersHistoryActivity : AppCompatActivity() {
    data class OrderEntry(
        val id: String,
        val date: String,
        val items: String,
        val total: String,
        val status: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_orders_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.ivBack)?.setOnClickListener { finishWithMotion(isForward = false) }
        applyPressFeedback(R.id.ivBack)
        renderOrders()
    }

    private fun renderOrders() {
        val orders = listOf(
            OrderEntry("#FW-2026-018", "12 Feb 2026", "Chechia Traditionnelle x1", "45.500 DT", "Delivered"),
            OrderEntry("#FW-2026-015", "03 Feb 2026", "Bijoux de l'Argent x1", "120.000 DT", "On the way"),
            OrderEntry("#FW-2026-009", "25 Jan 2026", "Harissa Artisanale Bio x2", "25.800 DT", "Delivered"),
            OrderEntry("#FW-2026-004", "11 Jan 2026", "Balgha Cuir x1", "65.000 DT", "Delivered")
        )

        val container = findViewById<LinearLayout>(R.id.layoutOrdersContainer) ?: return
        val empty = findViewById<TextView>(R.id.tvEmptyOrders) ?: return
        container.removeAllViews()

        if (orders.isEmpty()) {
            empty.visibility = View.VISIBLE
            return
        }
        empty.visibility = View.GONE

        orders.forEachIndexed { index, order ->
            val row = layoutInflater.inflate(R.layout.item_order_history, container, false)
            row.findViewById<TextView>(R.id.tvOrderId)?.text = order.id
            row.findViewById<TextView>(R.id.tvOrderDate)?.text = order.date
            row.findViewById<TextView>(R.id.tvOrderItems)?.text = order.items
            row.findViewById<TextView>(R.id.tvOrderTotal)?.text = order.total
            row.findViewById<TextView>(R.id.tvOrderStatus)?.text = order.status
            container.addView(row)
            animateListItemEntry(row, index, startDelayMs = 35L)
        }
    }
}
