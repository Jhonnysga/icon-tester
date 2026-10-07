package com.wally.icontest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Probador de iconos del T900ultra/LY736.
 * Conecta por BLE y envía notificaciones (18,18) con el iconId indicado
 * para identificar qué icono muestra el reloj para cada ID.
 */
class MainActivity : AppCompatActivity(), BleManager.Listener {

    private lateinit var ble: BleManager
    private lateinit var statusView: TextView
    private lateinit var logView: TextView
    private lateinit var iconInput: EditText
    private lateinit var connectBtn: Button
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ble = BleManager.get(this)
        ble.listener = this

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        root.addView(TextView(this).apply {
            text = "Icon Tester v1.0"
            textSize = 24f
        })
        statusView = TextView(this).apply {
            text = "Desconectado"
            textSize = 16f
            setPadding(0, 8, 0, 16)
        }
        root.addView(statusView)

        connectBtn = Button(this).apply {
            text = "CONECTAR"
            setOnClickListener { onConnectToggle() }
        }
        root.addView(connectBtn)

        root.addView(TextView(this).apply {
            text = "ID de icono:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        })
        iconInput = EditText(this).apply {
            hint = "Ej: 8"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("8")
        }
        root.addView(iconInput)

        root.addView(Button(this).apply {
            text = "ENVIAR PRUEBA"
            setOnClickListener { sendIconTest() }
        })

        root.addView(TextView(this).apply {
            text = "Prueba rápida (IDs 1–20):"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        })
        val grid = GridLayout(this).apply {
            columnCount = 5
            rowCount = 4
        }
        for (id in 1..20) {
            val b = Button(this).apply {
                text = id.toString()
                textSize = 14f
                setOnClickListener { sendIconId(id) }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }
            grid.addView(b, params)
        }
        root.addView(grid)

        root.addView(TextView(this).apply {
            text = "Log:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        })
        logView = TextView(this).apply {
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        val scroll = ScrollView(this).apply {
            addView(logView)
        }
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val outer = ScrollView(this).apply { addView(root) }
        setContentView(outer)

        addLog("Listo. Pulsa CONECTAR.")
    }

    private fun onConnectToggle() {
        if (ble.connected) {
            ble.disconnect()
            connectBtn.text = "CONECTAR"
        } else {
            if (!ensureBtPermissions()) return
            connectBtn.text = "CONECTANDO..."
            ble.targetMac = "9A:22:33:04:80:E8"
            ble.connectToKnown()
        }
    }

    private fun sendIconTest() {
        val id = iconInput.text.toString().toIntOrNull()
        if (id == null) {
            Toast.makeText(this, "Ingresa un número válido", Toast.LENGTH_SHORT).show()
            return
        }
        sendIconId(id)
    }

    private fun sendIconId(id: Int) {
        if (!ble.connected) {
            Toast.makeText(this, "Conecta el reloj primero", Toast.LENGTH_SHORT).show()
            return
        }
        iconInput.setText(id.toString())
        ble.sendNotification(id, "Icono $id", "Prueba de icono $id")
        addLog(">>> Prueba enviada con iconId=$id — mira el reloj")
    }

    // ---------- BleManager.Listener ----------

    override fun onStateChanged(connected: Boolean, deviceName: String?) {
        runOnUiThread {
            statusView.text = if (connected) "Conectado: $deviceName" else "Desconectado"
            connectBtn.text = if (connected) "DESCONECTAR" else "CONECTAR"
        }
    }

    override fun onLog(msg: String) {
        runOnUiThread { addLog(msg) }
    }

    private fun addLog(msg: String) {
        val ts = timeFmt.format(Date())
        logView.append("[$ts] $msg\n")
    }

    // ---------- Permisos BLE ----------

    private fun ensureBtPermissions(): Boolean {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                .filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
                .forEach { needed.add(it) }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1001)
            return false
        }
        return true
    }
}
