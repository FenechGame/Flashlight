package com.barrera.flashlight

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var switchOff: ImageButton
    private lateinit var switchOn: ImageButton
    private lateinit var switchBlinking: SwitchCompat
    private lateinit var switchCallBlinking: SwitchCompat
    private var hasCameraFlash = false
    private lateinit var threadBlinking: Thread

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: MainActivity
    }

    init {
        instance = this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchOff = findViewById(R.id.switch_off)
        switchOn = findViewById(R.id.switch_on)
        switchBlinking = findViewById(R.id.switch_blinking)
        switchCallBlinking = findViewById(R.id.switch_call_blinking)
        hasCameraFlash = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        switchOff.setOnClickListener {
            if (hasCameraFlash) {
                switchOff.visibility = View.INVISIBLE
                switchOn.visibility = View.VISIBLE
                if (switchBlinking.isChecked)
                    blinkingOn()
                else
                    flashLightOn()
            } else
                Toast.makeText(this, "Нет фонарика", Toast.LENGTH_LONG).show()
        }

        switchOn.setOnClickListener {
            if (hasCameraFlash) {
                switchOff.visibility = View.VISIBLE
                switchOn.visibility = View.INVISIBLE
                blinkingOff()
                flashLightOff()
            } else
                Toast.makeText(this, "Нет фонарика", Toast.LENGTH_LONG).show()
        }

        if (isMyServiceRunning())
            switchCallBlinking.isChecked = true

        switchCallBlinking.setOnCheckedChangeListener { buttonView, isChecked ->
            if (hasCameraFlash) {
                if (isChecked) {
                    if (permissions())
                        blinkingCallOn()
                } else {
                    blinkingCallOff()
                }
            } else {
                switchCallBlinking.isChecked = false
                Toast.makeText(this, "Нет фонарика", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Включение фонарика
     */
    private fun flashLightOn() {
        val cameraManager: CameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        cameraManager.setTorchMode(cameraId, true)
    }

    /**
     * Выключение фонарика
     */
    fun flashLightOff() {
        val cameraManager: CameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        cameraManager.setTorchMode(cameraId, false)
    }

    /**
     * Включение мигания фонарика
     */
    fun blinkingOn() {
        if (::threadBlinking.isInitialized)
            threadBlinking.interrupt()
        threadBlinking = Thread() {
            run {
                try {
                    while (true) {
                        flashLightOn()
                        Thread.sleep(500)
                        flashLightOff()
                        Thread.sleep(500)
                    }
                } catch (e: InterruptedException) {
                    threadBlinking.interrupt()
                }
            }
        }
        threadBlinking.start()
    }

    /**
     * Выключение мигания фонарика
     */
    fun blinkingOff() {
        try {
            threadBlinking.interrupt()
        } catch (ignore: UninitializedPropertyAccessException) {
        }
    }

    /**
     *Включение мигания фонарика при звонке
     */
    private fun blinkingCallOn() {
        switchCallBlinking.isChecked = true
        val intent = Intent(this, ServiceBackground::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    /**
     *Выключение мигания фонарика при звонке
     */
    private fun blinkingCallOff() {
        switchCallBlinking.isChecked = false
        stopService(Intent(this, ServiceBackground::class.java))
    }

    /**
     *Проверка на запущенный в фоне процесс для мигания фонариком при звонке.
     */
    private fun isMyServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { "class ".plus(it.service.className) == ServiceBackground::class.java.toString() }
    }

    /**
     * Запрос разрешений на доступ к ссстоянию звонков.
     * Звонит телефон, идет разговор, положена трубка и т.д.
     */
    private fun permissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this@MainActivity,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,
                    Manifest.permission.READ_PHONE_STATE)
            ) {
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.READ_PHONE_STATE), 456)
            } else {
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.READ_PHONE_STATE), 456)
            }
        } else
            return true
        return false
    }

    /**
     * Результат запроса на доступ к состоянию телефона.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            456 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    blinkingCallOn()
                } else {
                    switchCallBlinking.isChecked = false
                }
            }
        }
    }
}