package edu.umg.programacion2.gastos

import edu.umg.programacion2.gastos.datos.RepositorioEnMemoria
import edu.umg.programacion2.gastos.modelo.Categoria
import edu.umg.programacion2.gastos.modelo.Gasto
import edu.umg.programacion2.gastos.modelo.Ingreso
import edu.umg.programacion2.gastos.servicio.ControlGastos
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun main() = runBlocking {
    val control = ControlGastos(RepositorioEnMemoria())
    val mes = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    while (true) {
        println("\n===== CONTROL DE GASTOS ($mes) =====")
        println("1. Registrar ingreso")
        println("2. Registrar gasto")
        println("3. Ver movimientos")
        println("4. Modificar un movimiento")
        println("5. Eliminar un movimiento")
        println("6. Ver resumen y alerta")
        println("0. Salir")
        print("Opción: ")

        when (readLine()?.trim()) {
            "1" -> {
                print("Descripción: "); val d = readLine().orEmpty()
                print("Monto: "); val m = readLine()?.toDoubleOrNull() ?: 0.0
                println(control.registrarIngreso(Ingreso(descripcion = d, monto = m, fecha = hoy)) ?: "✅ Ingreso guardado")
            }
            "2" -> {
                print("Descripción: "); val d = readLine().orEmpty()
                print("Monto: "); val m = readLine()?.toDoubleOrNull() ?: 0.0
                Categoria.entries.forEachIndexed { i, c -> println("  ${i + 1}. $c") }
                print("Categoría (número): ")
                val cat = Categoria.entries.getOrNull((readLine()?.toIntOrNull() ?: 0) - 1) ?: Categoria.OTROS
                println(control.registrarGasto(Gasto(descripcion = d, monto = m, categoria = cat, fecha = hoy)) ?: "✅ Gasto guardado")
            }
            "3" -> {
                println("--- Ingresos ---")
                control.ingresosDelMes(mes).forEach { println("#${it.id} ${it.descripcion}  Q ${it.monto}  (${it.fecha})") }
                println("--- Gastos ---")
                control.gastosDelMes(mes).forEach { println("#${it.id} ${it.descripcion}  Q ${it.monto}  [${it.categoria}]  (${it.fecha})") }
            }
            "4" -> {
                print("¿Ingreso (i) o gasto (g)? "); val tipo = readLine()?.trim()
                print("ID a modificar: "); val id = readLine()?.toLongOrNull() ?: 0L
                print("Nueva descripción: "); val d = readLine().orEmpty()
                print("Nuevo monto: "); val m = readLine()?.toDoubleOrNull() ?: 0.0
                if (tipo == "i") {
                    println(control.modificarIngreso(Ingreso(id = id, descripcion = d, monto = m, fecha = hoy)) ?: "✅ Ingreso modificado")
                } else {
                    println(control.modificarGasto(Gasto(id = id, descripcion = d, monto = m, categoria = Categoria.OTROS, fecha = hoy)) ?: "✅ Gasto modificado")
                }
            }
            "5" -> {
                print("¿Ingreso (i) o gasto (g)? "); val tipo = readLine()?.trim()
                print("ID a eliminar: "); val id = readLine()?.toLongOrNull() ?: 0L
                val ok = if (tipo == "i") control.eliminarIngreso(id) else control.eliminarGasto(id)
                println(if (ok) "✅ Eliminado" else "No se encontró ese ID")
            }
            "6" -> {
                println(control.resumen(mes))
                println(control.alerta(mes) ?: "👍 Todo en orden este mes")
            }
            "0" -> { println("¡Hasta luego!"); break }
            else -> println("Opción no válida")
        }
    }
}
