package edu.umg.controlgastos

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import edu.umg.controlgastos.databinding.ActivityHistorialBinding
import edu.umg.programacion2.gastos.servicio.ControlGastos
import kotlinx.coroutines.launch

class HistorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialBinding
    private val repo by lazy { RepositorioSQLite(applicationContext) }
    private val control by lazy { ControlGastos(repo) }

    private val nombresMes = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        val oscuro = getSharedPreferences("perfil", MODE_PRIVATE).getBoolean("oscuro", false)
        AppCompatDelegate.setDefaultNightMode(
            if (oscuro) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch { mostrar() }
    }

    // "2026-10" -> "Octubre 2026"
    private fun nombreMes(mes: String): String {
        val n = mes.substring(5, 7).toIntOrNull() ?: 1
        return "${nombresMes[n - 1]} ${mes.substring(0, 4)}"
    }

    private suspend fun mostrar() {
        binding.llMeses.removeAllViews()

        // Más reciente arriba, enero (el más antiguo) hasta abajo
        val meses = (repo.listarGastos().map { it.fecha.take(7) } +
                repo.listarIngresos().map { it.fecha.take(7) })
            .distinct().sortedDescending()

        if (meses.isEmpty()) {
            binding.llMeses.addView(TextView(this).apply {
                text = "Aún no hay movimientos registrados."
                setPadding(0, 24, 0, 0)
            })
            return
        }

        for (mes in meses) {
            val ingresos = control.totalIngresos(mes)
            val gastos = control.totalGastos(mes)
            val saldo = ingresos - gastos

            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(getColor(R.color.tarjeta))
                setPadding(32, 24, 32, 24)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 24) }
                setOnClickListener { lifecycleScope.launch { detalle(mes) } }
            }
            tarjeta.addView(TextView(this).apply {
                text = nombreMes(mes)
                textSize = 18f
                setTextColor(getColor(R.color.acento))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            tarjeta.addView(TextView(this).apply {
                text = "Ingresos: ${Moneda.formato(this@HistorialActivity, ingresos)}\n" +
                        "Gastos: ${Moneda.formato(this@HistorialActivity, gastos)}\n" +
                        "Saldo: ${Moneda.formato(this@HistorialActivity, saldo)}"
                textSize = 15f
                setPadding(0, 8, 0, 0)
            })
            binding.llMeses.addView(tarjeta)
        }
    }

    private suspend fun detalle(mes: String) {
        val gastos = control.gastosDelMes(mes)
        val ingresos = control.ingresosDelMes(mes)

        val porCategoria = gastos.groupBy { it.categoria }
            .map { (cat, lista) -> "• $cat: ${Moneda.formato(this, lista.sumOf { it.monto })}" }
            .joinToString("\n")
            .ifBlank { "Sin gastos" }

        val listaIngresos = ingresos
            .joinToString("\n") { "• ${it.descripcion}: ${Moneda.formato(this, it.monto)}" }
            .ifBlank { "Sin ingresos" }

        AlertDialog.Builder(this)
            .setTitle(nombreMes(mes))
            .setMessage("GASTOS POR CATEGORÍA\n$porCategoria\n\nINGRESOS\n$listaIngresos")
            .setPositiveButton("Cerrar", null)
            .setNegativeButton("Eliminar mes") { _, _ -> confirmarEliminar(mes) }
            .show()
    }

    private fun confirmarEliminar(mes: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar ${nombreMes(mes)}?")
            .setMessage("Se borrarán todos los ingresos y gastos de este mes. No se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    control.gastosDelMes(mes).forEach { control.eliminarGasto(it.id) }
                    control.ingresosDelMes(mes).forEach { control.eliminarIngreso(it.id) }
                    mostrar()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}