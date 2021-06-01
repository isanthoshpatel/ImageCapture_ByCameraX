package com.example.imagecapture_camerax

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_2.*

class Activity2 : AppCompatActivity() {

    lateinit var uri:Uri
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_2)

        intent.data.let {
            uri = it!!
            imageView.setImageURI(it)
        }
        bt_share.setOnClickListener {
            Intent().also {
                it.action = Intent.ACTION_SET_WALLPAPER
                it.putExtra(Intent.EXTRA_STREAM,uri)
                it.type = "image/*"
                it.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(it,"sharing image ...."))
            }
        }
    }
}
