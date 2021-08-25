package com.pedro.sample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.akki.circlemenu.CircleMenu
import com.akki.circlemenu.OnCircleMenuItemClicked
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnCircleMenuItemClicked {

  private val PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContentView(R.layout.activity_main)

    val circularMenu = findViewById<CircleMenu>(R.id.circle_menu)

    circularMenu.setOnMenuItemClickListener(this)

//    b_camera_demo.setOnClickListener {
//      if (!hasPermissions(this, *PERMISSIONS)) {
//        ActivityCompat.requestPermissions(this, PERMISSIONS, 1)
//      } else {
//        startActivity(Intent(this, CameraDemoActivity::class.java))
//      }
//    }
  }

  override fun onMenuItemClicked(id: Int) {
    when (id) {
      R.drawable.ic_baseline_delete_forever_24 -> showToast("Delete Button clicked")
      R.drawable.ic_baseline_person_search_24 -> showToast("Person Button clicked")
      R.drawable.ic_baseline_settings_24 -> showToast("Setting Button clicked")
      R.drawable.ic_baseline_edit_location_24 -> showToast("Location Button clicked")
    }
  }

  private fun showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
  }

  private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null) {
      for (permission in permissions) {
        if (ActivityCompat.checkSelfPermission(context,
              permission) != PackageManager.PERMISSION_GRANTED) {
          return false
        }
      }
    }
    return true
  }
}