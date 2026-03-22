package com.rtech.cartly

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore

class DealsFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val allDeals = mutableListOf<Map<String, String>>()
    private val favouriteNames = mutableSetOf<String>()
    private lateinit var dealsContainer: LinearLayout
    private lateinit var locationLabel: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var categoryContainer: LinearLayout
    private lateinit var loadingSpinner: ProgressBar
    private var selectedStore = "All"
    private var selectedCategory = "All"
    private val filterButtons = mutableListOf<TextView>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_deals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dealsContainer = view.findViewById(R.id.dealsContainer)
        locationLabel = view.findViewById(R.id.locationLabel)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        categoryContainer = view.findViewById(R.id.categoryContainer)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)

        swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.cartlyGreen)
        )

        swipeRefresh.setOnRefreshListener {
            allDeals.clear()
            favouriteNames.clear()
            loadFavourites()
        }

        val prefs = requireContext().getSharedPreferences("CartlyPrefs", Context.MODE_PRIVATE)
        locationLabel.text = prefs.getString("location", "Sandton, Johannesburg")

        val filterAll = view.findViewById<TextView>(R.id.filterAll)
        val filterCheckers = view.findViewById<TextView>(R.id.filterCheckers)
        val filterPnP = view.findViewById<TextView>(R.id.filterPnP)
        val filterShoprite = view.findViewById<TextView>(R.id.filterShoprite)
        val filterSpar = view.findViewById<TextView>(R.id.filterSpar)

        filterButtons.addAll(listOf(filterAll, filterCheckers, filterPnP, filterShoprite, filterSpar))
        val filterNames = listOf("All", "Checkers", "Pick n Pay", "Shoprite", "Spar")

        filterButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectedStore = filterNames[index]
                updateFilterButtons(index)
                filterDeals()
            }
        }

        loadFavourites()
    }

    override fun onResume() {
        super.onResume()
        val prefs = requireContext().getSharedPreferences("CartlyPrefs", Context.MODE_PRIVATE)
        locationLabel.text = prefs.getString("location", "Sandton, Johannesburg")
    }

    private fun isDarkMode(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateFilterButtons(activeIndex: Int) {
        filterButtons.forEachIndexed { index, button ->
            if (index == activeIndex) {
                button.setBackgroundResource(R.drawable.filter_active)
                button.setTextColor(Color.WHITE)
            } else {
                button.setBackgroundResource(R.drawable.filter_inactive)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.cartlyGreen))
            }
        }
    }

    private fun buildCategoryButtons(categories: List<String>) {
        categoryContainer.removeAllViews()
        val allCategories = listOf("All") + categories.distinct().sorted()
        for (category in allCategories) {
            val btn = TextView(requireContext()).apply {
                text = category
                textSize = 12f
                setTextColor(if (category == selectedCategory) Color.WHITE else Color.parseColor("#444444"))
                setBackgroundResource(if (category == selectedCategory) R.drawable.filter_active else R.drawable.filter_inactive)
                setPadding(24, 12, 24, 12)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 16, 0)
                layoutParams = params
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedCategory = category
                    buildCategoryButtons(categories)
                    filterDeals()
                }
            }
            categoryContainer.addView(btn)
        }
    }

    private fun loadFavourites() {
        db.collection("favourites")
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded) return@addOnSuccessListener
                favouriteNames.clear()
                for (document in result) {
                    val name = document.getString("name") ?: ""
                    if (name != "placeholder") {
                        favouriteNames.add(name)
                    }
                }
                loadDealsFromFirebase()
            }
    }

    private fun loadDealsFromFirebase() {
        loadingSpinner.visibility = View.VISIBLE
        dealsContainer.removeAllViews()
        db.collection("deals")
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded) return@addOnSuccessListener
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
                if (!isAdded) return@addOnSuccessListener
                loadingSpinner.visibility = View.GONE
                val categories = allDeals.map { it["category"] ?: "Other" }
                buildCategoryButtons(categories)
                swipeRefresh.isRefreshing = false
                filterDeals()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                loadingSpinner.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                android.util.Log.e("CARTLY", "Error: $e")
            }
    }

    private fun filterDeals() {
        dealsContainer.removeAllViews()
        val filtered = allDeals.filter { deal ->
            val matchesStore = selectedStore == "All" || deal["store"] == selectedStore
            val matchesCategory = selectedCategory == "All" || deal["category"] == selectedCategory
            matchesStore && matchesCategory
        }

        if (filtered.isEmpty()) {
            val noResults = TextView(requireContext()).apply {
                text = "No deals found"
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
                gravity = Gravity.CENTER
                setPadding(0, 60, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            dealsContainer.addView(noResults)
        } else {
            for (deal in filtered) {
                val card = createDealCard(
                    deal["name"]!!, deal["store"]!!, deal["distance"]!!,
                    deal["price_now"]!!, deal["price_was"]!!, deal["discount"]!!,
                    deal["emoji"]!!, deal["category"]!!
                )
                dealsContainer.addView(card)
            }
        }
    }

    private fun toggleFavourite(name: String, heartBtn: TextView) {
        if (favouriteNames.contains(name)) {
            favouriteNames.remove(name)
            heartBtn.text = "♡"
            heartBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
            db.collection("favourites")
                .whereEqualTo("name", name)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        document.reference.delete()
                    }
                }
        } else {
            favouriteNames.add(name)
            heartBtn.text = "♥"
            heartBtn.setTextColor(Color.parseColor("#E24B4A"))
            db.collection("favourites").add(mapOf("name" to name))
        }
    }

    private fun createDealCard(
        name: String, store: String, distance: String,
        priceNow: String, priceWas: String, discount: String,
        emoji: String, category: String
    ): LinearLayout {

        val cardBg = if (isDarkMode()) R.drawable.card_background_dark else R.drawable.card_background

        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(cardBg)
            setPadding(24, 24, 24, 16)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(28, 0, 28, 20)
            layoutParams = params
            elevation = 4f
        }

        val topRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
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
            textSize = 32f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 24, 0)
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
            text = "$store • $distance"
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
        }

        val categoryView = TextView(requireContext()).apply {
            text = category
            textSize = 10f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.cartlyGreen))
            setBackgroundColor(Color.parseColor("#E1F5EE"))
            setPadding(6, 3, 6, 3)
        }

        val discountView = TextView(requireContext()).apply {
            text = discount
            textSize = 11f
            setTextColor(Color.parseColor("#A32D2D"))
            setBackgroundColor(Color.parseColor("#FCEBEB"))
            setPadding(6, 4, 6, 4)
        }

        val tagRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 4, 0, 0)
            layoutParams = params
        }

        tagRow.addView(categoryView)
        val space = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(8, 1)
        }
        tagRow.addView(space)
        tagRow.addView(discountView)

        infoLayout.addView(nameView)
        infoLayout.addView(storeView)
        infoLayout.addView(tagRow)

        val priceLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        val priceNowView = TextView(requireContext()).apply {
            text = priceNow
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.cartlyGreen))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val priceWasView = TextView(requireContext()).apply {
            text = priceWas
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        priceLayout.addView(priceNowView)
        priceLayout.addView(priceWasView)

        topRow.addView(emojiView)
        topRow.addView(infoLayout)
        topRow.addView(priceLayout)

        val bottomRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val isFav = favouriteNames.contains(name)
        val heartBtn = TextView(requireContext()).apply {
            text = if (isFav) "♥" else "♡"
            textSize = 20f
            setTextColor(if (isFav) Color.parseColor("#E24B4A") else
                ContextCompat.getColor(requireContext(), R.color.textSecondary))
            setPadding(0, 8, 0, 0)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                toggleFavourite(name, this)
            }
        }

        bottomRow.addView(heartBtn)
        card.addView(topRow)
        card.addView(bottomRow)

        return card
    }
}