package com.pedro.sample

import android.content.ContentValues.TAG
import android.graphics.*
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
import android.util.Log
import android.view.SurfaceView
import java.io.FileNotFoundException
import java.io.FileOutputStream

import android.view.PixelCopy

import android.graphics.Bitmap
import android.os.*
import androidx.annotation.RequiresApi
import android.os.Build


class CameraDemoActivity : AppCompatActivity(), ConnectCheckerRtsp, View.OnClickListener,
    SurfaceHolder.Callback {

    private lateinit var rtspServerCamera1: RtspServerCamera1
    private lateinit var button: Button
    private lateinit var bRecord: Button

    private var vRecoding = false
    private var vStreaming = false

    private var fname: String = "default"
    private var currentDateAndTime = ""
    private lateinit var folder: File

    enum class SupportedFileType {
        TYPE_JPG, TYPE_MP4
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_camera_demo)

        if (savedInstanceState == null) {
            val extras = intent.extras

            if (extras != null) {
                vRecoding = extras.getBoolean("isVideoRecordingEnabled")
                vStreaming = extras.getBoolean("isVideoStreamingEnabled")
            }
        }

        rtspServerCamera1 = RtspServerCamera1(surfaceView, this, 1935)
        surfaceView.holder.addCallback(this)

        hideNavigationBar()

        surfaceView.setOnClickListener(View.OnClickListener {
            captureScreenShot()
        })

        when(vStreaming){
            true -> rtspStreaming()
        }

        when(vRecoding){
            true->videoRecording()
        }
    }

    private fun hideNavigationBar() {
        val currentApiVersion = android.os.Build.VERSION.SDK_INT;

        // Hide navigation bar
        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = flags

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            val decorView = window.decorView
            decorView
                .setOnSystemUiVisibilityChangeListener { visibility ->
                    if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                        decorView.systemUiVisibility = flags
                    }
                }
        }
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
            Toast.makeText(
                this@CameraDemoActivity,
                "Connection failed. $reason",
                Toast.LENGTH_SHORT
            ).show()
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

    private fun isRTCWorking(): Boolean {
        // TODO: Need implementation
        return true
    }

    override fun onAuthSuccessRtsp() {
        runOnUiThread {
            Toast.makeText(this@CameraDemoActivity, "Auth success", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun captureScreenShot() {
        try {
            val mPreview = findViewById<SurfaceView>(R.id.surfaceView)
            this.usePixelCopy(mPreview) { bitmap: Bitmap? ->
                processBitMap(bitmap)
            }
        } catch (e: CameraOpenException) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onClick(view: View) {
    }

    private fun videoRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (!rtspServerCamera1.isRecording) {
                try {
                    if (!rtspServerCamera1.isStreaming) {
                        if (rtspServerCamera1.prepareAudio() && rtspServerCamera1.prepareVideo()) {
                            rtspServerCamera1.startRecord(getOutputMediaFile(SupportedFileType.TYPE_MP4).toString())
                            Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                this, "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        rtspServerCamera1.startRecord(getOutputMediaFile(SupportedFileType.TYPE_MP4).toString())
                        Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    rtspServerCamera1.stopRecord()
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
            } else {
            }
        } else {
            Toast.makeText(
                this,
                "You need min JELLY_BEAN_MR2(API 18) for do it...",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun rtspStreaming() {
        if (!rtspServerCamera1.isStreaming) {
            if (rtspServerCamera1.isRecording || rtspServerCamera1.prepareAudio() && rtspServerCamera1.prepareVideo()) {
                rtspServerCamera1.startStream()
                tv_url.text = rtspServerCamera1.getEndPointConnection()
                val t = rtspServerCamera1.getEndPointConnection()
Log.d("Avijit", t)
            } else {
                Toast.makeText(
                    this,
                    "Error preparing stream, This device cant do it",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        } else {
            tv_url.text = ""
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun usePixelCopy(videoView: SurfaceView, callback: (Bitmap?) -> Unit) {
        val bitmap: Bitmap = Bitmap.createBitmap(
            videoView.width,
            videoView.height,
            Bitmap.Config.ARGB_8888
        );
        try {
            // Create a handler thread to offload the processing of the image.
            val handlerThread = HandlerThread("PixelCopier");
            handlerThread.start();
            PixelCopy.request(
                videoView, bitmap,
                PixelCopy.OnPixelCopyFinishedListener { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        callback(bitmap)
                    }
                    handlerThread.quitSafely();
                },
                Handler(handlerThread.looper)
            )
        } catch (e: IllegalArgumentException) {
            callback(null)
            // PixelCopy may throw IllegalArgumentException, make sure to handle it
            e.printStackTrace()
        }
    }

    private fun processBitMap(bitmap: Bitmap?) {
        Log.d("PixelCopy", "Store the bitmap to a file")
        storeImage(bitmap)
    }

    // This wont work on SurfaceView
    private fun storeImage(image: Bitmap?) {
        val pictureFile = getOutputMediaFile(SupportedFileType.TYPE_JPG)
        if (pictureFile == null) {
            Log.d(
                TAG,
                "Error creating media file, check storage permissions: "
            ) // e.getMessage());
            return
        }
        try {
            val fos = FileOutputStream(pictureFile)
            image!!.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.fd.sync()
            fos.close()
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "File not found: " + e.message)
        } catch (e: IOException) {
            Log.d(TAG, "Error accessing file: " + e.message)
        }
    }

    private fun outDirectoryName(): File {
        var failedToCreate: Boolean = false
        var directoryExists: Boolean = false
        var rtcWorking: Boolean = false
        rtcWorking = isRTCWorking()

        var currentDate =
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) ?: fname

        var mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            currentDate
        )

        if (!mediaStorageDir!!.exists()) {
            if (!mediaStorageDir!!.mkdirs()) {
                failedToCreate = true
                Log.d("dir", "Failed to create directory")
            }
        } else {
            directoryExists = true
        }
        return mediaStorageDir
    }

    private fun getOutputMediaFile(outputType: SupportedFileType): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        val mediaStorageDir = outDirectoryName()

        // Create a media file name
        val timeStamp = SimpleDateFormat("ddMMyyyy_HHmmss").format(Date())
        val mediaFile: File

        // Decide the file extension based on the argument
        // Please make sure the file type is supported in SupportedFileType
        var fileExtension: String = when (outputType) {
            SupportedFileType.TYPE_JPG -> ".jpg"
            SupportedFileType.TYPE_MP4 -> ".mp4"
        }

        val mImageName = "MI_$timeStamp$fileExtension"
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
                Toast.makeText(
                    this,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
                    Toast.LENGTH_SHORT
                ).show()
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
