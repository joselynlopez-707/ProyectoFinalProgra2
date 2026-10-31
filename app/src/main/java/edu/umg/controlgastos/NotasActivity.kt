package edu.umg.controlgastos

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import edu.umg.controlgastos.databinding.ActivityNotasBinding
import java.util.Locale

class NotasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotasBinding
    private val repo by lazy { RepositorioSQLite(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        val oscuro = getSharedPreferences("perfil", MODE_PRIVATE).getBoolean("oscuro", false)
        AppCompatDelegate.setDefaultNightMode(
            if (oscuro) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        binding = ActivityNotasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etPrecio.hint = if (Moneda.esDolar(this)) "Precio en US$ (opcional)" else "Precio en Q (opcional)"

        binding.btnAgregarNota.setOnClickListener {
            val texto = binding.etNota.text.toString().trim()
            if (texto.isBlank()) {
                binding.etNota.error = "Escribe algo"
                return@setOnClickListener
            }
            val escrito = binding.etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            repo.guardarNota(texto, Moneda.aQuetzales(this, escrito))
            binding.etNota.text.clear()
            binding.etPrecio.text.clear()
            mostrar()
        }
        mostrar()
    }

    // 2026-10-30 -> 30/10/2026
    private fun formato(f: String?): String {
        if (f == null || f.length < 10) return ""
        return "${f.substring(8, 10)}/${f.substring(5, 7)}/${f.substring(0, 4)}"
    }

    private fun titulo(texto: String) {
        binding.llNotas.addView(TextView(this).apply {
            text = texto
            textSize = 18f
            setTextColor(getColor(R.color.acento))
            setPadding(0, 24, 0, 8)
            setTypeface(typeface, Typeface.BOLD)
        })
    }

    private fun botonChico(texto: String, color: Int, accion: () -> Unit) =
        Button(this).apply {
            text = texto
            textSize = 11f
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(getColor(color))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(4, 0, 0, 0) }
            setOnClickListener { accion() }
        }

    private fun fila(n: Nota) {
        val fecha = if (n.comprado) formato(n.fechaCompra) else formato(n.fecha)
        val precio = if (n.monto > 0) "  ·  ${Moneda.formato(this, n.monto)}" else ""

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 4)
        }

        val cb = CheckBox(this).apply {
            text = "${n.texto}$precio\n$fecha"
            textSize = 15f
            isChecked = n.comprado
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnCheckedChangeListener { _, marcado ->
                repo.marcarNota(n.id, marcado)
                mostrar()
            }
        }

        contenedor.addView(cb)
        contenedor.addView(botonChico("Editar", R.color.morado) { editar(n) })
        contenedor.addView(botonChico("Borrar", R.color.peligro) { confirmarBorrar(n) })
        binding.llNotas.addView(contenedor)
    }

    private fun mostrar() {
        binding.llNotas.removeAllViews()
        val notas = repo.listarNotas()
        val pendientes = notas.filter { !it.comprado }
        val comprados = notas.filter { it.comprado }

        binding.tvTotal.text = "Pendiente por comprar: ${Moneda.formato(this, pendientes.sumOf { it.monto })}\n" +
                "Ya comprado: ${Moneda.formato(this, comprados.sumOf { it.monto })}"

        titulo("Pendientes")
        pendientes.forEach { fila(it) }

        titulo("Comprado")
        comprados.forEach { fila(it) }
    }

    private fun editar(n: Nota) {
        val caja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val etTexto = EditText(this).apply { hint = "Compra"; setText(n.texto) }
        val etPrecio = EditText(this).apply {
            hint = if (Moneda.esDolar(this@NotasActivity)) "Precio en US$" else "Precio en Q"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            if (n.monto > 0) setText("%.2f".format(Locale.US, Moneda.desdeQuetzales(this@NotasActivity, n.monto)))
        }
        caja.addView(etTexto)
        caja.addView(etPrecio)

        AlertDialog.Builder(this)
            .setTitle("Editar compra")
            .setView(caja)
            .setPositiveButton("Guardar") { _, _ ->
                val texto = etTexto.text.toString().trim()
                if (texto.isBlank()) {
                    Toast.makeText(this, "Escribe algo", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val escrito = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
                repo.editarNota(n.id, texto, Moneda.aQuetzales(this, escrito))
                mostrar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarBorrar(n: Nota) {
        AlertDialog.Builder(this)
            .setTitle("¿Borrar \"${n.texto}\"?")
            .setMessage("Esta acción no se puede deshacer.")
            .setPositiveButton("Borrar") { _, _ ->
                repo.eliminarNota(n.id)
                mostrar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}