package com.example.realtimesound

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.realtimesound.databinding.ActivityRecordBinding
import org.json.JSONObject
import org.json.JSONException

class RecordActivity : AppCompatActivity() {
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200

    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)
    private lateinit var binding: ActivityRecordBinding
    private lateinit var serverResponseReceiver: BroadcastReceiver



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serviceIntent = Intent(this, AudioService::class.java)
        binding.switchAudio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startService(serviceIntent)
            } else {
                stopService(serviceIntent)
            }
        }

        serverResponseReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val response = intent.getStringExtra("response")
                try {
                    val jsonResponse = JSONObject(response)
                    val label = jsonResponse.getString("label")
                    val confidence = jsonResponse.getInt("confidence")
                    binding.tvPredict.text = "Label: $label, Confidence: $confidence%"
                } catch (e: JSONException) {
                    e.printStackTrace()
                    binding.tvPredict.text = "Failed to parse server response"
                }
            }
        }
//        registerReceiver(serverResponseReceiver, IntentFilter("com.example.realtimesound.SERVER_RESPONSE"))
//        val intentFilter = IntentFilter("com.example.realtimesound.SERVER_RESPONSE")
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
//            registerReceiver(serverResponseReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
//        } else {
//            registerReceiver(serverResponseReceiver, intentFilter)
//        }

        val intentFilter = IntentFilter("com.example.realtime sound.SERVER_RESPONSE")
        registerReceiver(serverResponseReceiver, intentFilter)

        requestPermissions()
        SocketManager.connect()
    }

    //Request Permission Audio
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
    }


    override fun onDestroy() {
        super.onDestroy()
        SocketManager.disconnect()
        unregisterReceiver(serverResponseReceiver)
    }
}