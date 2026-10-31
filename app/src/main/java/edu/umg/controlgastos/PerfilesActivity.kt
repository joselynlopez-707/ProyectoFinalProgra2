package edu.umg.controlgastos

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import edu.umg.controlgastos.databinding.ActivityPerfilesBinding

class PerfilesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilesBinding
    private val repo by lazy { RepositorioSQLite(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnNuevoPerfil.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        mostrar()
    }

    private fun irAInicio() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun boton(texto: String, color: Int, accion: () -> Unit) =
        Button(this).apply {
            text = texto
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(getColor(color))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(4, 4, 4, 4) }
            setOnClickListener { accion() }
        }

    private fun mostrar() {
        binding.llPerfiles.removeAllViews()
        val activo = repo.perfilActivo()?.id

        repo.listarPerfiles().forEach { p ->
            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.tarjeta_form)
                setPadding(32, 24, 32, 24)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 24) }
            }

            val usaUser = repo.usaUsername(p.id)
            val estado = if (p.id == activo) "  ✓ En uso" else ""
            tarjeta.addView(TextView(this).apply {
                text = "@${p.username}$estado"
                textSize = 18f
                setTextColor(getColor(R.color.acento))
                setTypeface(typeface, Typeface.BOLD)
            })
            tarjeta.addView(TextView(this).apply {
                text = "${p.nombres}\n${p.gmail}\nEl saludo muestra: ${if (usaUser) "@username" else "nombre"}"
                textSize = 14f
                setPadding(0, 8, 0, 8)
            })

            val fila1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            fila1.addView(boton("Usar", R.color.morado) { repo.activar(p); irAInicio() })
            fila1.addView(boton(if (usaUser) "Ver nombre" else "Ver @user", R.color.verde) {
                repo.fijarUsaUsername(p.id, !usaUser); mostrar()
            })

            val fila2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            fila2.addView(boton("Editar username", R.color.morado) { editarUsername(p) })
            fila2.addView(boton("Eliminar", R.color.peligro) { confirmarEliminar(p) })

            tarjeta.addView(fila1)
            tarjeta.addView(fila2)
            binding.llPerfiles.addView(tarjeta)
        }
    }

    private fun editarUsername(p: Perfil) {
        val et = EditText(this).apply { setText(p.username); setPadding(48, 32, 48, 16) }
        AlertDialog.Builder(this)
            .setTitle("Editar username")
            .setView(et)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevo = et.text.toString().trim().removePrefix("@")
                when {
                    nuevo.length < 3 || nuevo.length > 30 ->
                        Toast.makeText(this, "Entre 3 y 30 caracteres", Toast.LENGTH_SHORT).show()
                    repo.existeUsername(nuevo, p.id) ->
                        Toast.makeText(this, "Ese username ya existe", Toast.LENGTH_SHORT).show()
                    else -> { repo.cambiarUsername(p.id, nuevo); mostrar() }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun confirmarEliminar(p: Perfil) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar @${p.username}?")
            .setMessage("Se borrarán también todos sus ingresos, gastos y compras. No se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                repo.eliminarPerfil(p.id)
                irAInicio()
            }
            .setNegativeButton("Cancelar", null).show()
    }
}