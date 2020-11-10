package com.example.androidstuff

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import android.widget.ToggleButton
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidstuff.MainActivity.BlueToothClass.ThreadConnection
import com.example.androidstuff.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.Exception
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

        // TODO : Turning Off / On Bluetooth
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.discover ->{
                enableDiscoverability()
            }
        }
        return true
    }
    private fun enableDiscoverability() {
         val discoverIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
             putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300)
         }
        startActivity(discoverIntent)
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
                           ThreadConnection(bluetoothAdapter,device)
                        }
                    })

                }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }


    private class  BlueToothClass(var handler : Handler) {
        inner class ThreadConnection(var bluetoothAdapter: BluetoothAdapter,device: BluetoothDevice) : Thread(){

            private  val blueToothSocket : BluetoothSocket? by lazy {
                device.createRfcommSocketToServiceRecord(UUID.randomUUID())
            }

            private var mmInputStream = blueToothSocket?.inputStream
            private var mmOutPutStream = blueToothSocket?.outputStream
            private var mmBuffer : ByteArray = ByteArray(1024)
            var numBytes : Int = 0
            override fun run() {
                super.run()


                bluetoothAdapter.cancelDiscovery()

                blueToothSocket.use {
                    it?.connect()

                    // TODO : Device Is Connected , manage work
                    numBytes = try {
                        mmInputStream?.read(mmBuffer)!!
                    }catch ( e : Exception){
                        Log.d("TAG","Exception OCCURED")
                    }

                    val readMsg  = handler.obtainMessage(MESSAGE_READ,numBytes,-1,mmBuffer)
                    readMsg.sendToTarget()
                    closeSocket()

                }

            }
            // Call this from the main activity to send data to the remote device.
            fun write(bytes: ByteArray) {
                try {
                    mmOutPutStream?.write(bytes)
                } catch (e: IOException) {
                    Log.e(TAG, "Error occurred when sending data", e)

                    // Send a failure message back to the activity.
                    val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                    val bundle = Bundle().apply {
                        putString("toast", "Couldn't send data to the other device")
                    }
                    writeErrorMsg.data = bundle
                    handler.sendMessage(writeErrorMsg)
                    return
                }

                // Share the sent message with the UI activity.
                val writtenMsg = handler.obtainMessage(
                    MESSAGE_WRITE, -1, -1, bytes)
                writtenMsg.sendToTarget()
            }


            fun closeSocket(){
                try {
                    blueToothSocket?.close()
                } catch (e: Exception){
                    Log.d("TAG","Could not close blutooth socket")
                }
            }
            private fun manageSomeOtherWork() {

            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val BLUETOOTH_REQUEST_CODE = 1001
        const val MESSAGE_READ = 0
        const val MESSAGE_WRITE = 1
        const val MESSAGE_TOAST = 2
    }

}