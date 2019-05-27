package com.jason.nettydemo.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.jason.nettydemo.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onClick(v: View?) {
        when (v) {
            btn_client -> {
                startActivity(Intent(this, ClientActivity::class.java))
            }
            btn_server -> {
                startActivity(Intent(this, ServerActivity::class.java))
            }

        }
    }

}
