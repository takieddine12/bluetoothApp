package com.example.androidstuff

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.widget.CompoundButton
import android.widget.ToggleButton
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidstuff.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var devicesAdapter: DevicesAdapter
    private lateinit var devicesList : MutableList<DevicesModel>
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        setSupportActionBar(binding.toolbar)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.setHasFixedSize(true)

        devicesList = mutableListOf()

        // TODO : Use Discovery to find devices around

        val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        broadcastReceiver = object  : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                when(action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        // Discovery has found a device. Get the BluetoothDevice
                        // object and its info from the Intent.
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        val deviceName = device?.name
                        val deviceAddress = device?.address

                        val devicesModel = DevicesModel(R.drawable.ic_baseline_phone_android_24,deviceName!!)
                        devicesList.add(devicesModel)
                        devicesModel.macAddress = deviceAddress!!

                        devicesList.clear()
                        devicesAdapter = DevicesAdapter(devicesList)
                        binding.recyclerView.adapter = devicesAdapter

                        devicesAdapter.setClicked(object  : DevicesAdapter.BlueToothClickListener{
                            override fun clicked(position: Int) {
                                val bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.randomUUID())
                                CoroutineScope(Dispatchers.IO).launch {
                                    bluetoothSocket.connect()
                                }
                            }
                        })

                    }
                }
            }
        }
        registerReceiver(broadcastReceiver,intentFilter)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.devicesmenu,menu)
        val menuItem = menu?.findItem(R.id.onOffBlueTooth)
        val toggleButton = menuItem?.actionView as AppCompatToggleButton
        toggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked){
                showBluetoothDevices()
            }
        }
        return true
    }
    private fun showBluetoothDevices(){
             // TODO : Show All Devices With BlueTooth Turned On
             if(bluetoothAdapter != null ) {
                if(!bluetoothAdapter.isEnabled){
                    val enableBlueTooth =  Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBlueTooth,BLUETOOTH_REQUEST_CODE)
                }
             }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == BLUETOOTH_REQUEST_CODE &&  resultCode == Activity.RESULT_OK){
            // TODO : Check If Devices are already paired using BoundDevices
            val boundDevices = bluetoothAdapter.bondedDevices
            boundDevices.forEach { device ->
                    val name = device.name
                    val macAddress = device.address

                    val devicesModel = DevicesModel(R.drawable.ic_baseline_phone_android_24,name)
                    devicesList.clear()
                    devicesList.add(devicesModel)
                    devicesModel.macAddress = macAddress
                    devicesAdapter = DevicesAdapter(devicesList)
                    binding.recyclerView.adapter = devicesAdapter

                    devicesAdapter.setClicked(object  : DevicesAdapter.BlueToothClickListener{
                        override fun clicked(position: Int) {
                            val bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.randomUUID())
                            CoroutineScope(Dispatchers.IO).launch {
                                bluetoothSocket.connect()
                            }
                        }
                    })

                }


        }
    }
    companion object {
        const val BLUETOOTH_REQUEST_CODE = 1001
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

}