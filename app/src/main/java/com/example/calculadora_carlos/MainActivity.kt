package com.example.calculadora_carlos

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.math.BigDecimal
import java.math.MathContext

class MainActivity:AppCompatActivity() {
    private var pantalla = "inicio"
    private var entrada = "0"
    private var acumulado: BigDecimal? = null
    private var operador = ""
    private var nuevaEntrada = true
    private var error = false
    private var visor: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        savedInstanceState?.let {
            pantalla = it.getString("pantalla", "inicio")
            entrada = it.getString("entrada", "0")
            acumulado = it.getString("acumulado")?.toBigDecimalOrNull()
            operador = it.getString("operador", "")
            nuevaEntrada = it.getBoolean("nuevaEntrada", true)
            error = it.getBoolean("error", false)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (pantalla) {
                    "principal" -> mostrar("salida")
                    "salida" -> mostrar("principal")
                    else -> finish()
                }
            }
        })
        mostrar(pantalla)
    }

    private fun mostrar(destino: String) {
        pantalla = destino
        visor = null
        setContentView(when (destino) {
            "principal" -> R.layout.activity_pantalla_principal
            "salida" -> R.layout.activity_pantalla_salida
            else -> R.layout.activity_pantalla_inicio
        })
        val contenido = findViewById<android.view.ViewGroup>(android.R.id.content)
        val raiz = contenido.getChildAt(0)
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(raiz)
        when (destino) {
            "inicio" -> conectar(R.id.r4ozv8cg53u7) { mostrar("principal") }
            "salida" -> {
                conectar(R.id.rha7kedgirg9) { mostrar("principal") }
                findViewById<TextView>(R.id.rty85nskdn).text = "Cerrar aplicación"
                conectar(R.id.rd4nidk8bjvl) { finish() }
            }
            "principal" -> configurarCalculadora()
        }
    }

    private fun conectar(id: Int, accion: () -> Unit) {
        findViewById<View>(id).setOnClickListener { accion() }
    }

    private fun configurarCalculadora() {
        // El diseño de CodeTea tiene un contenedor vacío para el resultado.
        val contenedor = findViewById<LinearLayout>(R.id.rqzuef29h4xc)
        visor = TextView(this).apply {
            textSize = 36f
            setTextColor(Color.rgb(31, 42, 31))
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            val margen = (16 * resources.displayMetrics.density).toInt()
            setPadding(margen, margen, margen, margen)
        }
        contenedor.addView(visor, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))
        val digitos = mapOf(
            R.id.ro4vj2a34rqs to "0", R.id.rgty5tz1sh7q to "1",
            R.id.rh4327odnwc to "2", R.id.re9dfjdqtyas to "3",
            R.id.r2uorohbmp9o to "4", R.id.rw1odwubecan to "5",
            R.id.r95i6rw511r4 to "6", R.id.rjhv48q7bw3a to "7",
            R.id.r12ypcc34nfua to "8", R.id.rnjihywxnjsi to "9"
        )
        digitos.forEach { (id, digito) -> conectar(id) {
            if (error) limpiar()
            entrada = if (nuevaEntrada || entrada == "0") digito else entrada + digito
            nuevaEntrada = false
            actualizar()
        } }
        mapOf(R.id.rrit7zrc461 to "+", R.id.ri2xcypx9hq to "−",
            R.id.rd7d67u65sot to "×", R.id.rmzutha683p to "÷"
        ).forEach { (id, simbolo) -> conectar(id) { operar(simbolo) } }
        conectar(R.id.r7wqazfd8e1j) { operar("=") }
        conectar(R.id.rfbdslqrdxxq) { limpiar(); actualizar() }
        conectar(R.id.rus92bk7ov2g) {
            if (!error && !nuevaEntrada) {
                entrada = entrada.dropLast(1)
                if (entrada.isEmpty() || entrada == "-") entrada = "0"
            }
            actualizar()
        }
        conectar(R.id.rl4qn3vov7ok) {
            if (error) limpiar()
            if (nuevaEntrada) entrada = "0"
            if (!entrada.contains('.')) entrada += "."
            nuevaEntrada = false
            actualizar()
        }
        conectar(R.id.ry6luaijace) {
            if (!error && entrada.toBigDecimal().signum() != 0) {
                entrada = if (entrada.startsWith('-')) entrada.drop(1) else "-$entrada"
            }
            actualizar()
        }
        conectar(R.id.r35xmh5bog29) {
            if (!error) {
                entrada = formatear(entrada.toBigDecimal().divide(BigDecimal("100")))
                actualizar()
            }
        }
        conectar(R.id.rjc0rtmfrh4) { mostrar("salida") }
        conectar(R.id.rsg238bghkzn_rl) {
            AlertDialog.Builder(this).setTitle("Cómo calcular")
                .setMessage("Introduce un número, una operación y otro número. Pulsa = para calcular. Las operaciones se resuelven en el orden introducido. % divide el número visible entre 100; C borra todo.")
                .setPositiveButton("Aceptar", null).show()
        }
        conectar(R.id.rntz36gh9nsk_rl) {
            AlertDialog.Builder(this).setTitle("Opciones")
                .setItems(arrayOf("Borrar cálculo", "Salir")) { _, opcion ->
                    if (opcion == 0) { limpiar(); actualizar() } else mostrar("salida")
                }.show()
        }
        actualizar()
    }

    private fun operar(siguiente: String) {
        if (error) return
        val numero = entrada.toBigDecimal()
        val anterior = acumulado
        if (anterior != null && operador.isNotEmpty() && !nuevaEntrada) {
            if (operador == "÷" && numero.signum() == 0) {
                limpiar()
                error = true
                actualizar()
                return
            }
            val resultado = when (operador) {
                "+" -> anterior.add(numero)
                "−" -> anterior.subtract(numero)
                "×" -> anterior.multiply(numero)
                else -> anterior.divide(numero, MathContext.DECIMAL64)
            }
            entrada = formatear(resultado)
        }
        acumulado = if (siguiente == "=") null else entrada.toBigDecimal()
        operador = if (siguiente == "=") "" else siguiente
        nuevaEntrada = true
        actualizar()
    }

    private fun formatear(valor: BigDecimal) = valor.stripTrailingZeros().toPlainString()

    private fun actualizar() {
        visor?.text = if (error) "No se puede dividir entre 0" else entrada.replace('.', ',')
    }

    private fun limpiar() {
        entrada = "0"
        acumulado = null
        operador = ""
        nuevaEntrada = true
        error = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pantalla", pantalla)
        outState.putString("entrada", entrada)
        outState.putString("acumulado", acumulado?.toPlainString())
        outState.putString("operador", operador)
        outState.putBoolean("nuevaEntrada", nuevaEntrada)
        outState.putBoolean("error", error)
        super.onSaveInstanceState(outState)
    }
}
