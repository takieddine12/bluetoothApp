package com.example.androidstuff

import android.graphics.drawable.Drawable
import android.widget.ImageView

class DevicesModel
    (
    var deviceIcon : Int,
    var deviceName : String
) {
    lateinit var macAddress : String
}