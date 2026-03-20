package com.rtech.cartly

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DealDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deal_detail)

        val name = intent.getStringExtra("name") ?: ""
        val store = intent.getStringExtra("store") ?: ""
        val distance = intent.getStringExtra("distance") ?: ""
        val priceNow = intent.getStringExtra("price_now") ?: ""
        val priceWas = intent.getStringExtra("price_was") ?: ""
        val discount = intent.getStringExtra("discount") ?: ""
        val emoji = intent.getStringExtra("emoji") ?: "🛒"

        findViewById<TextView>(R.id.detailEmoji).text = emoji
        findViewById<TextView>(R.id.detailName).text = name
        findViewById<TextView>(R.id.detailStore).text = "$store • $distance away"
        findViewById<TextView>(R.id.detailStoreName).text = store
        findViewById<TextView>(R.id.detailDistance).text = "$distance away"
        findViewById<TextView>(R.id.detailPriceNow).text = priceNow
        findViewById<TextView>(R.id.detailPriceWas).text = priceWas
        findViewById<TextView>(R.id.detailSaving).text = discount

        val btnBack = findViewById<TextView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }
}