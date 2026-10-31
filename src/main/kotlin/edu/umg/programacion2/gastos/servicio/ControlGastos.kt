package edu.umg.programacion2.gastos.servicio

import edu.umg.programacion2.gastos.datos.Repositorio
import edu.umg.programacion2.gastos.modelo.Gasto
import edu.umg.programacion2.gastos.modelo.Ingreso

class ControlGastos(private val repo: Repositorio) {

    // Cada función devuelve null si todo salió bien, o un mensaje de error.

    suspend fun registrarGasto(gasto: Gasto): String? {
        validar(gasto.descripcion, gasto.monto, gasto.fecha)?.let { return it }
        repo.guardarGasto(gasto)
        return null
    }

    suspend fun registrarIngreso(ingreso: Ingreso): String? {
        validar(ingreso.descripcion, ingreso.monto, ingreso.fecha)?.let { return it }
        repo.guardarIngreso(ingreso)
        return null
    }

    suspend fun modificarGasto(gasto: Gasto): String? {
        validar(gasto.descripcion, gasto.monto, gasto.fecha)?.let { return it }
        return if (repo.modificarGasto(gasto)) null else "El gasto no existe"
    }

    suspend fun modificarIngreso(ingreso: Ingreso): String? {
        validar(ingreso.descripcion, ingreso.monto, ingreso.fecha)?.let { return it }
        return if (repo.modificarIngreso(ingreso)) null else "El ingreso no existe"
    }

    suspend fun eliminarGasto(id: Long) = repo.eliminarGasto(id)
    suspend fun eliminarIngreso(id: Long) = repo.eliminarIngreso(id)

    // mes en formato "yyyy-MM", por ejemplo "2026-10"
    suspend fun gastosDelMes(mes: String) = repo.listarGastos().filter { it.fecha.startsWith(mes) }
    suspend fun ingresosDelMes(mes: String) = repo.listarIngresos().filter { it.fecha.startsWith(mes) }

    suspend fun totalGastos(mes: String) = gastosDelMes(mes).sumOf { it.monto }
    suspend fun totalIngresos(mes: String) = ingresosDelMes(mes).sumOf { it.monto }
    suspend fun saldo(mes: String) = totalIngresos(mes) - totalGastos(mes)

    // Alerta mensual: null si todo está bien
    suspend fun alerta(mes: String): String? {
        val ingresos = totalIngresos(mes)
        val gastos = totalGastos(mes)
        return when {
            gastos > ingresos -> "⚠️ Tus gastos superan tus ingresos este mes"
            ingresos > 0 && gastos >= ingresos * 0.8 -> "⚠️ Ya gastaste más del 80% de tus ingresos"
            else -> null
        }
    }

    suspend fun resumen(mes: String): String =
        "Ingresos: Q %.2f · Gastos: Q %.2f · Saldo: Q %.2f".format(
            totalIngresos(mes), totalGastos(mes), saldo(mes)
        )

    private fun validar(descripcion: String, monto: Double, fecha: String): String? = when {
        descripcion.isBlank() -> "La descripción no puede estar vacía"
        monto <= 0 -> "El monto debe ser mayor que cero"
        !Regex("""\d{4}-\d{2}-\d{2}""").matches(fecha) -> "Fecha inválida (use yyyy-MM-dd)"
        else -> null
    }
}