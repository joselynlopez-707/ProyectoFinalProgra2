package edu.umg.programacion2.gastos.datos

import edu.umg.programacion2.gastos.modelo.Gasto
import edu.umg.programacion2.gastos.modelo.Ingreso

interface Repositorio {
    // Gastos
    suspend fun guardarGasto(gasto: Gasto): Long
    suspend fun modificarGasto(gasto: Gasto): Boolean
    suspend fun eliminarGasto(id: Long): Boolean
    suspend fun listarGastos(): List<Gasto>

    // Ingresos
    suspend fun guardarIngreso(ingreso: Ingreso): Long
    suspend fun modificarIngreso(ingreso: Ingreso): Boolean
    suspend fun eliminarIngreso(id: Long): Boolean
    suspend fun listarIngresos(): List<Ingreso>
}