package com.pedro.sample

import android.content.ContentValues.TAG
import android.os.Build
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import com.pedro.rtspserver.RtspServerCamera1
import kotlinx.android.synthetic.main.activity_camera_demo.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.os.Environment
import android.graphics.Bitmap
import android.util.Log
import android.view.SurfaceView
import java.io.FileNotFoundException
import java.io.FileOutputStream


class CameraDemoActivity : AppCompatActivity(), ConnectCheckerRtsp, View.OnClickListener,
    SurfaceHolder.Callback {

  private lateinit var rtspServerCamera1: RtspServerCamera1
  private lateinit var button: Button
  private lateinit var bRecord: Button

  private var currentDateAndTime = ""
  private lateinit var folder: File

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContentView(R.layout.activity_camera_demo)
    folder = File(getExternalFilesDir(null)!!.absolutePath + "/rtmp-rtsp-stream-client-java")
    button = findViewById(R.id.b_start_stop)
    button.setOnClickListener(this)
    bRecord = findViewById(R.id.b_record)
    bRecord.setOnClickListener(this)
    capture_image.setOnClickListener(this)
    rtspServerCamera1 = RtspServerCamera1(surfaceView, this, 1935)
    surfaceView.holder.addCallback(this)
  }

  override fun onNewBitrateRtsp(bitrate: Long) {

  }

  override fun onConnectionSuccessRtsp() {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "Connection success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onConnectionFailedRtsp(reason: String) {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "Connection failed. $reason", Toast.LENGTH_SHORT)
          .show()
      rtspServerCamera1.stopStream()
      button.setText(R.string.start_button)
    }
  }

  override fun onConnectionStartedRtsp(rtspUrl: String) {
  }

  override fun onDisconnectRtsp() {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "Disconnected", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onAuthErrorRtsp() {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "Auth error", Toast.LENGTH_SHORT).show()
      rtspServerCamera1.stopStream()
      button.setText(R.string.start_button)
      tv_url.text = ""
    }
  }

  override fun onAuthSuccessRtsp() {
    runOnUiThread {
      Toast.makeText(this@CameraDemoActivity, "Auth success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onClick(view: View) {
    when (view.id) {
      R.id.b_start_stop -> if (!rtspServerCamera1.isStreaming) {
        if (rtspServerCamera1.isRecording || rtspServerCamera1.prepareAudio() && rtspServerCamera1.prepareVideo()) {
          button.setText(R.string.stop_button)
          rtspServerCamera1.startStream()
          tv_url.text = rtspServerCamera1.getEndPointConnection()
        } else {
          Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT)
              .show()
        }
      } else {
        button.setText(R.string.start_button)
        rtspServerCamera1.stopStream()
        tv_url.text = ""
      }
      R.id.capture_image -> try {
//        rtspServerCamera1.switchCamera()
        storeImage(getBitmap())
      } catch (e: CameraOpenException) {
        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
      }

      R.id.b_record -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          if (!rtspServerCamera1.isRecording) {
            try {
              if (!folder.exists()) {
                folder.mkdir()
              }
              val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
              currentDateAndTime = sdf.format(Date())
              if (!rtspServerCamera1.isStreaming) {
                if (rtspServerCamera1.prepareAudio() && rtspServerCamera1.prepareVideo()) {
                  rtspServerCamera1.startRecord(folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                  bRecord.setText(R.string.stop_record)
                  Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(
                    this, "Error preparing stream, This device cant do it",
                    Toast.LENGTH_SHORT
                  ).show()
                }
              } else {
                rtspServerCamera1.startRecord(folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                bRecord.setText(R.string.stop_record)
                Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
              }
            } catch (e: IOException) {
              rtspServerCamera1.stopRecord()
              bRecord.setText(R.string.start_record)
              Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
          } else {
            rtspServerCamera1.stopRecord()
            bRecord.setText(R.string.start_record)
            Toast.makeText(
              this, "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
              Toast.LENGTH_SHORT
            ).show()
          }
        } else {
          Toast.makeText(this, "You need min JELLY_BEAN_MR2(API 18) for do it...", Toast.LENGTH_SHORT).show()
        }
      }
      else -> {
      }
    }
  }

  private fun getBitmap(): Bitmap {
    var mPreview = findViewById<SurfaceView>(R.id.surfaceView)
    mPreview.isDrawingCacheEnabled = true
    mPreview.buildDrawingCache()
    val bitmap: Bitmap = Bitmap.createBitmap(mPreview.width, mPreview.height, Bitmap.Config.ARGB_8888)
    mPreview.isDrawingCacheEnabled = false
    return bitmap
  }


  private fun storeImage(image: Bitmap) {
    val pictureFile = getOutputMediaFile()
    if (pictureFile == null) {
      Log.d(
        TAG,
        "Error creating media file, check storage permissions: "
      ) // e.getMessage());
      return
    }
    try {
      val fos = FileOutputStream(pictureFile)
      image.compress(Bitmap.CompressFormat.PNG, 90, fos)
      fos.close()
    } catch (e: FileNotFoundException) {
      Log.d(TAG, "File not found: " + e.message)
    } catch (e: IOException) {
      Log.d(TAG, "Error accessing file: " + e.message)
    }
  }

  private fun getOutputMediaFile(): File? {
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.
    val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath)

    // This location works best if you want the created images to be shared
    // between applications and persist after your app has been uninstalled.

    // Create the storage directory if it does not exist
    if (!mediaStorageDir.exists()) {
      if (!mediaStorageDir.mkdirs()) {
        return null
      }
    }
    // Create a media file name
    val timeStamp = SimpleDateFormat("ddMMyyyy_HHmm").format(Date())
    val mediaFile: File
    val mImageName = "MI_$timeStamp.jpg"
    mediaFile = File(mediaStorageDir.path + File.separator + mImageName)
    Toast.makeText(this, mediaFile.absolutePath, Toast.LENGTH_SHORT).show()
    return mediaFile
  }

  override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
  }

  override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
    rtspServerCamera1.startPreview()
  }

  override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      if (rtspServerCamera1.isRecording) {
        rtspServerCamera1.stopRecord()
        bRecord.setText(R.string.start_record)
        Toast.makeText(this, "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath, Toast.LENGTH_SHORT).show()
        currentDateAndTime = ""
      }
    }
    if (rtspServerCamera1.isStreaming) {
      rtspServerCamera1.stopStream()
      button.text = resources.getString(R.string.start_button)
      tv_url.text = ""
    }
    rtspServerCamera1.stopPreview()
  }
}
