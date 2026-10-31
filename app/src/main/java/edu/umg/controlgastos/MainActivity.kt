package edu.umg.controlgastos

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import edu.umg.controlgastos.databinding.ActivityMainBinding
import edu.umg.programacion2.gastos.modelo.Categoria
import edu.umg.programacion2.gastos.modelo.Gasto
import edu.umg.programacion2.gastos.modelo.Ingreso
import edu.umg.programacion2.gastos.servicio.ControlGastos
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repo by lazy { RepositorioSQLite(applicationContext) }
    private val control by lazy { ControlGastos(repo) }

    private val formatoMes = SimpleDateFormat("yyyy-MM", Locale.US)
    private var mes = formatoMes.format(Date())
    private val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val nombresMes = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    private fun etiquetaMes(): String {
        val n = mes.substring(5, 7).toInt()
        return "${nombresMes[n - 1]} ${mes.substring(0, 4)}"
    }

    private fun cambiarMes(delta: Int) {
        val cal = Calendar.getInstance()
        cal.time = formatoMes.parse(mes)!!
        cal.add(Calendar.MONTH, delta)
        mes = formatoMes.format(cal.time)
        lifecycleScope.launch { actualizarPantalla() }
    }

    // Si estás viendo el mes actual guarda con la fecha de hoy;
    // si estás en otro mes, guarda con el día 1 de ese mes
    private fun fechaParaGuardar(): String =
        if (mes == hoy.substring(0, 7)) hoy else "$mes-01"

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("perfil", MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.getBoolean("oscuro", false)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)

        val usuario = repo.obtenerUsuario()
        if (usuario == null) {
            startActivity(Intent(this, RegistroActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val genero = prefs.getString("genero", "")
        val bienvenida = when (genero) { "mujer" -> "Bienvenida"; "hombre" -> "Bienvenido"; else -> "Te damos la bienvenida" }
        binding.tvSaludo.text = "$bienvenida, ${repo.nombreParaMostrar()} 🌱"

        binding.btnMesAnterior.setOnClickListener { cambiarMes(-1) }
        binding.btnMesSiguiente.setOnClickListener { cambiarMes(1) }

        binding.spTipo.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, listOf("Gasto", "Ingreso")
        )
        binding.spCategoria.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, Categoria.entries
        )
        binding.spTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                binding.spCategoria.visibility = if (pos == 0) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        binding.btnGuardar.setOnClickListener { guardar(usuario.id) }
        binding.btnNotas.setOnClickListener {
            startActivity(Intent(this, NotasActivity::class.java))
        }
        binding.btnHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }
        binding.btnAjustes.setOnClickListener {
            startActivity(Intent(this, AjustesActivity::class.java))
        }
    }

    // Se actualiza al abrir y al volver de Ajustes (por si cambió la moneda)
    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) lifecycleScope.launch { actualizarPantalla() }
    }

    private fun guardar(usuarioId: Long) {
        val montoEscrito = binding.etMonto.text.toString().toDoubleOrNull()
        if (montoEscrito == null) {
            binding.etMonto.error = "Ingresa un monto válido"
            return
        }
        val monto = Moneda.aQuetzales(this, montoEscrito)
        val descripcion = binding.etDescripcion.text.toString().trim()
        val esGasto = binding.spTipo.selectedItemPosition == 0
        val fecha = fechaParaGuardar()

        lifecycleScope.launch {
            val error = if (esGasto) {
                control.registrarGasto(
                    Gasto(usuarioId = usuarioId, descripcion = descripcion, monto = monto,
                        categoria = binding.spCategoria.selectedItem as Categoria, fecha = fecha)
                )
            } else {
                control.registrarIngreso(
                    Ingreso(usuarioId = usuarioId, descripcion = descripcion, monto = monto, fecha = fecha)
                )
            }
            if (error != null) {
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                return@launch
            }
            binding.etDescripcion.text.clear()
            binding.etMonto.text.clear()
            actualizarPantalla()
        }
    }

    private suspend fun actualizarPantalla() {
        binding.tvMes.text = etiquetaMes()

        val ingresos = control.totalIngresos(mes)
        val gastos = control.totalGastos(mes)
        binding.tvResumen.text = "Ingresos: ${Moneda.formato(this, ingresos)}\n" +
                "Gastos: ${Moneda.formato(this, gastos)}\n" +
                "Saldo: ${Moneda.formato(this, ingresos - gastos)}"
        binding.tvAlerta.text = control.alerta(mes) ?: ""

        binding.etMonto.hint = if (Moneda.esDolar(this)) "Monto (US$)" else "Monto (Q)"

        binding.llLista.removeAllViews()
        control.ingresosDelMes(mes).forEach { ing ->
            agregarFila("➕ ${ing.descripcion}: ${Moneda.formato(this, ing.monto)}", getColor(R.color.titulo)) {
                opcionesIngreso(ing)
            }
        }
        control.gastosDelMes(mes).forEach { g ->
            agregarFila("➖ ${g.descripcion} [${g.categoria}]: ${Moneda.formato(this, g.monto)}", Color.parseColor("#E53935")) {
                opcionesGasto(g)
            }
        }
    }

    private fun agregarFila(texto: String, color: Int, alTocar: () -> Unit) {
        val tv = TextView(this).apply {
            text = texto
            textSize = 16f
            setTextColor(color)
            setPadding(0, 16, 0, 16)
            setOnClickListener { alTocar() }
        }
        binding.llLista.addView(tv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    // ---------- Gastos ----------
    private fun opcionesGasto(g: Gasto) {
        AlertDialog.Builder(this)
            .setTitle(g.descripcion)
            .setItems(arrayOf("Editar", "Eliminar")) { _, which ->
                if (which == 0) editarGasto(g) else confirmarEliminar {
                    lifecycleScope.launch { control.eliminarGasto(g.id); actualizarPantalla() }
                }
            }.show()
    }

    private fun editarGasto(g: Gasto) {
        val (caja, etDesc, etMonto) = cajaEdicion(g.descripcion, g.monto)
        AlertDialog.Builder(this)
            .setTitle("Editar gasto")
            .setView(caja)
            .setPositiveButton("Guardar") { _, _ ->
                val monto = Moneda.aQuetzales(this@MainActivity, etMonto.text.toString().toDoubleOrNull() ?: 0.0)
                lifecycleScope.launch {
                    val error = control.modificarGasto(
                        g.copy(descripcion = etDesc.text.toString().trim(), monto = monto))
                    if (error != null) Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                    actualizarPantalla()
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    // ---------- Ingresos ----------
    private fun opcionesIngreso(i: Ingreso) {
        AlertDialog.Builder(this)
            .setTitle(i.descripcion)
            .setItems(arrayOf("Editar", "Eliminar")) { _, which ->
                if (which == 0) editarIngreso(i) else confirmarEliminar {
                    lifecycleScope.launch { control.eliminarIngreso(i.id); actualizarPantalla() }
                }
            }.show()
    }

    private fun editarIngreso(i: Ingreso) {
        val (caja, etDesc, etMonto) = cajaEdicion(i.descripcion, i.monto)
        AlertDialog.Builder(this)
            .setTitle("Editar ingreso")
            .setView(caja)
            .setPositiveButton("Guardar") { _, _ ->
                val monto = Moneda.aQuetzales(this@MainActivity, etMonto.text.toString().toDoubleOrNull() ?: 0.0)
                lifecycleScope.launch {
                    val error = control.modificarIngreso(
                        i.copy(descripcion = etDesc.text.toString().trim(), monto = monto))
                    if (error != null) Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                    actualizarPantalla()
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    // ---------- Ayudas ----------
    private fun cajaEdicion(descripcion: String, monto: Double): Triple<LinearLayout, EditText, EditText> {
        val caja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val etDesc = EditText(this).apply { hint = "Descripción"; setText(descripcion) }
        val etMonto = EditText(this).apply {
            hint = if (Moneda.esDolar(this@MainActivity)) "Monto (US$)" else "Monto (Q)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText("%.2f".format(Locale.US, Moneda.desdeQuetzales(this@MainActivity, monto)))
        }
        caja.addView(etDesc)
        caja.addView(etMonto)
        return Triple(caja, etDesc, etMonto)
    }

    private fun confirmarEliminar(accion: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar?")
            .setMessage("Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> accion() }
            .setNegativeButton("Cancelar", null).show()
    }
}