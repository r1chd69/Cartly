package com.rtech.cartly

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class BasketFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val basketItems = mutableListOf<Map<String, String>>()
    private lateinit var basketContainer: LinearLayout
    private lateinit var totalView: TextView
    private lateinit var emptyView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_basket, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        basketContainer = view.findViewById(R.id.basketContainer)
        totalView = view.findViewById(R.id.totalView)
        emptyView = view.findViewById(R.id.emptyView)

        val btnClear = view.findViewById<TextView>(R.id.btnClearBasket)
        btnClear.setOnClickListener {
            db.collection("basket")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        document.reference.delete()
                    }
                    basketItems.clear()
                    renderBasket()
                }
        }

        loadBasket()
    }

    override fun onResume() {
        super.onResume()
        loadBasket()
    }

    private fun loadBasket() {
        db.collection("basket")
            .get()
            .addOnSuccessListener { result ->
                basketItems.clear()
                for (document in result) {
                    val name = document.getString("name") ?: ""
                    if (name != "placeholder") {
                        basketItems.add(mapOf(
                            "id" to document.id,
                            "name" to name,
                            "price" to (document.getString("price") ?: ""),
                            "store" to (document.getString("store") ?: ""),
                            "emoji" to (document.getString("emoji") ?: "🛒"),
                            "checked" to (document.getString("checked") ?: "false")
                        ))
                    }
                }
                renderBasket()
            }
    }

    private fun renderBasket() {
        basketContainer.removeAllViews()

        if (basketItems.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            totalView.text = "Total: R0.00"
            return
        }

        emptyView.visibility = View.GONE

        var total = 0.0

        for (item in basketItems) {
            val itemView = createBasketItem(
                item["id"]!!, item["name"]!!, item["price"]!!,
                item["store"]!!, item["emoji"]!!, item["checked"] == "true"
            )
            basketContainer.addView(itemView)

            if (item["checked"] != "true") {
                val price = item["price"]!!.replace("R", "").toDoubleOrNull() ?: 0.0
                total += price
            }
        }

        totalView.text = "Total: R%.2f".format(total)
    }

    private fun isDarkMode(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun createBasketItem(
        id: String, name: String, price: String,
        store: String, emoji: String, checked: Boolean
    ): LinearLayout {

        val cardBg = if (isDarkMode()) R.drawable.card_background_dark else R.drawable.card_background

        val item = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(cardBg)
            setPadding(20, 16, 20, 16)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(28, 0, 28, 12)
            layoutParams = params
            elevation = 2f
            gravity = Gravity.CENTER_VERTICAL
        }

        val emojiView = TextView(requireContext()).apply {
            text = emoji
            textSize = 26f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 16, 0)
            layoutParams = params
        }

        val infoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameView = TextView(requireContext()).apply {
            text = name
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            if (checked) {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
            }
        }

        val storeView = TextView(requireContext()).apply {
            text = store
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
        }

        infoLayout.addView(nameView)
        infoLayout.addView(storeView)

        val priceView = TextView(requireContext()).apply {
            text = price
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.cartlyGreen))
            setTypeface(null, android.graphics.Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 16, 0)
            layoutParams = params
        }

        val checkBtn = TextView(requireContext()).apply {
            text = if (checked) "✓" else "○"
            textSize = 20f
            setTextColor(if (checked) ContextCompat.getColor(requireContext(), R.color.cartlyGreen)
            else ContextCompat.getColor(requireContext(), R.color.textSecondary))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val newChecked = !checked
                db.collection("basket").document(id)
                    .update("checked", newChecked.toString())
                    .addOnSuccessListener { loadBasket() }
            }
        }

        val deleteBtn = TextView(requireContext()).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#E24B4A"))
            setPadding(16, 0, 0, 0)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                db.collection("basket").document(id)
                    .delete()
                    .addOnSuccessListener { loadBasket() }
            }
        }

        item.addView(emojiView)
        item.addView(infoLayout)
        item.addView(priceView)
        item.addView(checkBtn)
        item.addView(deleteBtn)

        return item
    }
}