package com.phelat.poolakeysample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.phelat.poolakey.Payment

class MainActivity : AppCompatActivity() {

    private val payment by lazy(LazyThreadSafetyMode.NONE) {
        Payment(context = this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        payment.initialize {}
    }

}
