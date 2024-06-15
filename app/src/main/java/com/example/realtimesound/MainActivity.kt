package com.example.realtimesound

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.net.URISyntaxException

class MainActivity : AppCompatActivity() {
    private lateinit var socket: Socket
    private lateinit var resultTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private val RECORD_AUDIO_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTextView = findViewById(R.id.resultTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        try {
            socket = IO.socket("http://192.168.169.6:8000")
            socket.connect()
            socket.on("response", onNewMessage)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

        startButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
            } else {
                startAudioService()
            }
        }

        stopButton.setOnClickListener {
            stopAudioService()
        }
    }

    private fun startAudioService() {
        try {
            val intent = Intent(this, AudioRecordService::class.java)
            startService(intent)
            startButton.isEnabled = false
            stopButton.isEnabled = true
        } catch (e: SecurityException) {
            resultTextView.text = "Permission denied. Unable to start audio recording."
        }
    }

    private fun stopAudioService() {
        val intent = Intent(this, AudioRecordService::class.java)
        stopService(intent)
        startButton.isEnabled = true
        stopButton.isEnabled = false
    }

    private val onNewMessage = Emitter.Listener { args ->
        runOnUiThread {
            try {
                val data = args[0] as String
                val json = JSONObject(data)
                val label = json.getString("label")
                val confidence = json.getInt("confidence")

                resultTextView.text = "Detected: $label\nConfidence: $confidence%"
            } catch (e: Exception) {
                e.printStackTrace()
                resultTextView.text = "Error: ${e.message}"
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAudioService()
            } else {
                resultTextView.text = "Permission denied. Unable to start audio recording."
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
    }
}
