package com.rtech.cartly

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val allDeals = mutableListOf<Map<String, String>>()
    private lateinit var searchResults: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchResults = view.findViewById(R.id.searchResults)

        val searchBar = view.findViewById<EditText>(R.id.searchInput)
        searchBar.addTextChangedListener { text ->
            searchDeals(text.toString())
        }

        loadAllDeals()
    }

    private fun loadAllDeals() {
        db.collection("deals")
            .get()
            .addOnSuccessListener { result ->
                allDeals.clear()
                for (document in result) {
                    allDeals.add(mapOf(
                        "name" to (document.getString("name") ?: ""),
                        "store" to (document.getString("store") ?: ""),
                        "distance" to (document.getString("distance") ?: ""),
                        "price_now" to (document.getString("price_now") ?: ""),
                        "price_was" to (document.getString("price_was") ?: ""),
                        "discount" to (document.getString("discount") ?: ""),
                        "emoji" to (document.getString("emoji") ?: "🛒"),
                        "category" to (document.getString("category") ?: "Other")
                    ))
                }
            }
    }

    private fun searchDeals(query: String) {
        searchResults.removeAllViews()

        if (query.isEmpty()) {
            val hint = TextView(requireContext()).apply {
                text = "Type to search for deals..."
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                gravity = Gravity.CENTER
                setPadding(0, 60, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            searchResults.addView(hint)
            return
        }

        val filtered = allDeals.filter { deal ->
            deal["name"]!!.contains(query, ignoreCase = true) ||
                    deal["store"]!!.contains(query, ignoreCase = true) ||
                    deal["category"]!!.contains(query, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            val noResults = TextView(requireContext()).apply {
                text = "No results for \"$query\""
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                gravity = Gravity.CENTER
                setPadding(0, 60, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            searchResults.addView(noResults)
        } else {
            for (deal in filtered) {
                val card = createSearchCard(
                    deal["name"]!!, deal["store"]!!, deal["distance"]!!,
                    deal["price_now"]!!, deal["price_was"]!!, deal["discount"]!!,
                    deal["emoji"]!!, deal["category"]!!
                )
                searchResults.addView(card)
            }
        }
    }

    private fun isDarkMode(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun createSearchCard(
        name: String, store: String, distance: String,
        priceNow: String, priceWas: String, discount: String,
        emoji: String, category: String
    ): LinearLayout {

        val cardBg = if (isDarkMode()) R.drawable.card_background_dark else R.drawable.card_background

        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(cardBg)
            setPadding(24, 24, 24, 24)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(28, 0, 28, 16)
            layoutParams = params
            elevation = 4f
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val intent = Intent(requireContext(), DealDetailActivity::class.java)
                intent.putExtra("name", name)
                intent.putExtra("store", store)
                intent.putExtra("distance", distance)
                intent.putExtra("price_now", priceNow)
                intent.putExtra("price_was", priceWas)
                intent.putExtra("discount", discount)
                intent.putExtra("emoji", emoji)
                startActivity(intent)
            }
        }

        val emojiView = TextView(requireContext()).apply {
            text = emoji
            textSize = 28f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 20, 0)
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
        }

        val storeView = TextView(requireContext()).apply {
            text = "$store • $distance away"
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
        }

        val discountView = TextView(requireContext()).apply {
            text = discount
            textSize = 11f
            setTextColor(Color.parseColor("#A32D2D"))
            setBackgroundColor(Color.parseColor("#FCEBEB"))
            setPadding(6, 3, 6, 3)
        }

        infoLayout.addView(nameView)
        infoLayout.addView(storeView)
        infoLayout.addView(discountView)

        val priceView = TextView(requireContext()).apply {
            text = priceNow
            textSize = 15f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.cartlyGreen))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        card.addView(emojiView)
        card.addView(infoLayout)
        card.addView(priceView)

        return card
    }
}