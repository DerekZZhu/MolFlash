package com.example.molflash

import android.content.Context
import android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate

class MainActivity : AppCompatActivity() {
    val cameraM by lazy { getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    var camIdWithFlash: String = "0"

    var isActivated: Boolean = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val camList = cameraM.cameraIdList
        camList.forEach {
            val characteristics = cameraM.getCameraCharacteristics(it)
            val hasFlash: Boolean? = characteristics.get(FLASH_INFO_AVAILABLE)
            if (camIdWithFlash == "0" && hasFlash == true) {
                camIdWithFlash = it
            }
        }

        val flashButton: Button = findViewById(R.id.startFlash)
        val actText : TextView = findViewById(R.id.isAct)

        val cycle = findViewById<EditText>(R.id.cycles)
        val period = findViewById<EditText>(R.id.period)
        val timeon = findViewById<EditText>(R.id.timeon)


        flashButton.setOnClickListener {
            if(!isActivated) {
                message("Pulse Activated", this)
                actText.text = "Activated"
                isActivated = !isActivated
                pulse(
                    period = (if (period.text == null) 2000 else period.text.toString().toLong()),
                    timeOn = (if (timeon.text == null) 0 else timeon.text.toString().toLong()),
                    cycles = (if (cycle.text == null) 5 else cycle.text.toString().toInt())
                )
            } else {
                message("Pulse Still Running!", this)
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
