package com.example.androidstuff

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.deviceslayout.*

class DevicesAdapter(var devicesList : MutableList<DevicesModel>) : RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder>() {

    private lateinit var  mClickListener : BlueToothClickListener

    interface BlueToothClickListener {
        fun clicked(position : Int)
    }

    fun setClicked(clickListener: BlueToothClickListener) {
        mClickListener = clickListener
    }
    class DeviceViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        var imageView: ImageView = itemView.findViewById(R.id.deviceIcon)
        var deviceName : TextView = itemView.findViewById(R.id.deviceName)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
       val view = LayoutInflater.from(parent.context).inflate(R.layout.deviceslayout,parent,false)
        return DeviceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return devicesList.size
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val model = devicesList[position]
        holder.deviceName.text = model.deviceName
        holder.imageView.setImageResource(R.drawable.ic_baseline_phone_android_24)

        holder.itemView.setOnClickListener {
            var position = holder.adapterPosition
            position.let {
                if(it != RecyclerView.NO_POSITION){
                    mClickListener.clicked(position)
                }
            }
        }
    }
}