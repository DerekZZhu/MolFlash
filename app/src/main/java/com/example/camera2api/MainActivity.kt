package com.example.camera2api

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Camera
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.util.*
import java.util.Timer.*
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate


class MainActivity : AppCompatActivity() {
//    val cameraM by lazy { getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    var camIdWithFlash: String = "0"

    lateinit var capReq: CaptureRequest.Builder

    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread

    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    lateinit var captureRequest: CaptureRequest

    lateinit var flashButton : Button
    var isActivated = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        get_permissions()
        Log.d("MainActivity", "This is a debug message");

        flashButton = findViewById(R.id.capture)

        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler((handlerThread).looper)

        val camList = cameraManager.cameraIdList
        camList.forEach {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            val hasFlash: Boolean? = characteristics.get(FLASH_INFO_AVAILABLE)
            if (camIdWithFlash == "0" && hasFlash == true) {
                camIdWithFlash = it
            }
        }


        cameraManager.setTorchMode(camIdWithFlash, true)
        message("Exist", this)



        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                open_camera()
//                cameraDevice.
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }
        }

        flashButton.setOnClickListener {
            Log.d("Flash", "Flash Button Fired");
            message("FIRE", this)
//            cameraManager.setTorchMode(camIdWithFlash, true)
//            pulse()
        }
    }

    @SuppressLint("MissingPermission")
    fun open_camera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object: CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera

                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                var surface = Surface(textureView.surfaceTexture)
                capReq.addTarget(surface)

//                captureRequest = capReq.build()


                cameraDevice.createCaptureSession(listOf(surface), object:
                    CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        // Setting up capture request modifications
                        capReq.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.FLASH_MODE_OFF)

                        cameraCaptureSession.setRepeatingRequest(capReq.build(), null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        TODO("Not yet implemented")
                    }

                }, handler)
            }

            override fun onDisconnected(camera: CameraDevice) {

            }

            override fun onError(camera: CameraDevice, error: Int) {

            }

        }, handler)
    }

    fun get_permissions() {
        var permissionsList = mutableListOf<String>()
        if(checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissionsList.add(android.Manifest.permission.CAMERA)
        if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) !=  PackageManager.PERMISSION_GRANTED)
            permissionsList.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionsList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permissionsList.size >0) {
            requestPermissions(permissionsList.toTypedArray(), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if(it != PackageManager.PERMISSION_GRANTED) {
                get_permissions()
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
                cameraManager.setTorchMode(camIdWithFlash, true)
                Timer().schedule(timeOn) {
                    cameraManager.setTorchMode(camIdWithFlash, false)
                }
                count++
                Log.d("Flashing", "000133505 Flashing");
            }
        }
    }

    fun message(s: String, c: Context) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show()
    }
}
