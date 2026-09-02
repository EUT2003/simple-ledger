package com.example.simpleledger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout

class SplashActivity : Activity() {
    override fun onCreate(state: Bundle?) { super.onCreate(state)
        val image = ImageView(this).apply { setImageResource(R.drawable.pencil_cover); adjustViewBounds = true }
        setContentView(LinearLayout(this).apply { gravity = android.view.Gravity.CENTER; addView(image, LinearLayout.LayoutParams(520, -2)) })
        image.postDelayed({ startActivity(Intent(this, MainActivity::class.java)); finish() }, 1000)
    }
}
