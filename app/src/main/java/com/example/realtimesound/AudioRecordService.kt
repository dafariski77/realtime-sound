package com.example.realtimesound

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URISyntaxException

class AudioRecordService : Service() {
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    private lateinit var socket: Socket
    private var isRecording = false
    private lateinit var audioRecord: AudioRecord
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            socket = IO.socket("http://10.0.2.2:8000")
            socket.connect()
            socket.on(Socket.EVENT_CONNECT) {
                Log.d("Socket.IO", "Connected to server")
            }
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

        startRecording()
        return START_STICKY
    }

    private fun startRecording() {
        isRecording = true
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE)

        audioRecord.startRecording()
        scope.launch {
            val buffer = ByteArray(BUFFER_SIZE)
            val byteArrayOutputStream = ByteArrayOutputStream()

            while (isRecording) {
                val read = audioRecord.read(buffer, 0, BUFFER_SIZE)
                if (read > 0) {
                    byteArrayOutputStream.write(buffer, 0, read)
                }

                if (byteArrayOutputStream.size() >= SAMPLE_RATE * 2) {
                    val data = byteArrayOutputStream.toByteArray()
                    socket.emit("predict_audio", data)
                    byteArrayOutputStream.reset()
                }
            }

            audioRecord.stop()
            audioRecord.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        socket.disconnect()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}