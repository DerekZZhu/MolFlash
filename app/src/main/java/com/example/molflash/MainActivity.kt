import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Camera
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.molflash.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate

class MainActivity : AppCompatActivity() {
    val cameraM by lazy { getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    lateinit var capReq: CaptureRequest.Builder
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var cameraManager : CameraManager
    lateinit var textureView: TextureView
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    lateinit var captureRequest: CaptureRequest

    var camIdWithFlash: String = "0"

    var isActivated: Boolean = false


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getPermissions()

        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler((handlerThread).looper)

        textureView.surfaceTextureListener = object: TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2:Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2:Int) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

            }
        }



        val camList = cameraM.cameraIdList
        camList.forEach {
            val characteristics = cameraM.getCameraCharacteristics(it)
            val hasFlash: Boolean? = characteristics.get(FLASH_INFO_AVAILABLE)
            if (camIdWithFlash == "0" && hasFlash == true) {
                camIdWithFlash = it
            }
        }

//        val flashButton: Button = findViewById(R.id.startFlash)
//        val actText : TextView = findViewById(R.id.isAct)
//
//        val cycle = findViewById<EditText>(R.id.cycles)
//        val period = findViewById<EditText>(R.id.period)
//        val timeon = findViewById<EditText>(R.id.timeon)


//        flashButton.setOnClickListener {
//            if(!isActivated) {
//                message("Pulse Activated", this)
//                actText.text = "Activated"
//                isActivated = !isActivated
//                pulse(
//                    period = (if (period.text == null || period.text.toString() == "") 2000 else period.text.toString().toLong()),
//                    timeOn = (if (timeon.text == null || timeon.text.toString() == "") 0 else timeon.text.toString().toLong()),
//                    cycles = (if (cycle.text == null || cycle.text.toString() == "") 5 else cycle.text.toString().toInt())
//                )
//            } else {
//                message("Pulse Still Running!", this)
//            }
//        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object: CameraDevice.StateCallback() {
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0

                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                var surface = Surface(textureView.surfaceTexture)
                capReq.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        cameraCaptureSession = p0
                        cameraCaptureSession.setRepeatingRequest(capReq.build(), null, null)
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {

                    }
                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {

            }

            override fun onError(p0: CameraDevice, p1:Int) {

            }
        }, handler)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getPermissions() {
        var permissionsList = mutableListOf<String>()
        if(checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) permissionsList.add(android.Manifest.permission.CAMERA)
        if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissionsList.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            if(checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) permissionsList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if(permissionsList.size > 0) {
            requestPermissions(permissionsList.toTypedArray(), 101)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if(it != PackageManager.PERMISSION_GRANTED) {
                getPermissions()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun pulse(period: Long = 2000, timeOn: Long = 0, cycles: Int = 5) {
        val timer = Timer()
        var count = 1

        timer.scheduleAtFixedRate(0, period) {
            if (count == cycles) {
                isActivated = false
                timer.cancel();
            }
            if (timer != null) {
                cameraM.setTorchMode(camIdWithFlash, true)
                Timer().schedule(timeOn) {
                    cameraM.setTorchMode(camIdWithFlash, false)
                }
                count++
            }
        }
    }

    fun message(s: String, c: Context) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show()
    }
}
