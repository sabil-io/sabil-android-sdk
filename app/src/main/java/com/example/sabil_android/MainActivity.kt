package com.example.sabil_android

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        val identifyBtn = findViewById<Button>(R.id.identify_btn)
//        identifyBtn.setOnClickListener {
//            Sabil.identify {
//                Toast.makeText(
//                    this,
//                    "Identity: ${it?.identity} with ${it?.confidence}",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//        Sabil.configure(
//            this,
//            "bf9ce83b-d44b-4da2-97df-1094decbdd56",
//            secret = null,
//            "18926a32-ede0-4dee-ad3f-8e7f7d0f3f75",
//            null,
//            SabilLimitConfig(1),
//            {
//                Toast.makeText(this, "This device should be logged out", Toast.LENGTH_LONG)
//                    .show()
//            },
//            null
//        ) {
//            Toast.makeText(this, "Limit exceeded", Toast.LENGTH_LONG).show()
//        }
//        Sabil.attach(supportFragmentManager, mapOf(Pair("test", "true"))) {
//            Toast.makeText(this, "Attached", Toast.LENGTH_LONG).show()
//        }

    }

}