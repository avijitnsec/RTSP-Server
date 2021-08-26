package com.pedro.sample

import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.akki.circlemenu.CircleMenu
import com.akki.circlemenu.OnCircleMenuItemClicked
import kotlinx.android.synthetic.main.activity_main.*
import android.net.ConnectivityManager

class MainActivity : AppCompatActivity(), OnCircleMenuItemClicked {

    private var circularMenu: CircleMenu? = null

    private var isVideoRecordingEnabled = false
    private var isVideoStreamingEnabled = false

    private val PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        circularMenu = findViewById<CircleMenu>(R.id.circle_menu)
        circularMenu!!.setOnMenuItemClickListener(this)

        b_camera_demo.setOnClickListener {
            if (!hasPermissions(this, *PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, 1)
            } else {
                var intent = Intent(this, CameraDemoActivity::class.java)
                intent.putExtra("isVideoRecordingEnabled", isVideoRecordingEnabled)
                intent.putExtra("isVideoStreamingEnabled", isVideoStreamingEnabled)
                startActivity(intent)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("ResourceType", "UseCompatLoadingForDrawables")
    override fun onMenuItemClicked(id: Int) {
        when (id) {
            R.drawable.ic_baseline_rtsp_start_streaming_24 -> {
                isVideoStreamingEnabled = !isVideoStreamingEnabled
                if (isVideoStreamingEnabled) {
                    var drawable = findViewById<FloatingActionButton>(id)
                    drawable.setImageResource(R.drawable.ic_baseline_rtsp_stop_streaming_24)
                }
            }
            R.drawable.ic_baseline_video_start_recording_24 -> {
                isVideoRecordingEnabled = !isVideoRecordingEnabled
                if (isVideoRecordingEnabled) {
                    var drawable = findViewById<FloatingActionButton>(id)
                    drawable.setImageResource(R.drawable.ic_baseline_video_recording_24)
                }
            }
            R.drawable.ic_baseline_wifi_signal_strong_24 -> {
                isVideoRecordingEnabled = !isVideoRecordingEnabled
                if (isVideoRecordingEnabled) {
                    var drawable = findViewById<FloatingActionButton>(id)
                    drawable.setImageResource(R.drawable.ic_baseline_wifi_signal_no_24)
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    private fun checkIfWifiConnectionAvailable(): Boolean {
        val connManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        return mWifi!!.isConnected
    }
}