package edu.umg.programacion2.gastos.datos

import edu.umg.programacion2.gastos.modelo.Gasto
import edu.umg.programacion2.gastos.modelo.Ingreso

class RepositorioEnMemoria : Repositorio {
    private val gastos = mutableListOf<Gasto>()
    private val ingresos = mutableListOf<Ingreso>()
    private var siguienteId = 1L

    override suspend fun guardarGasto(gasto: Gasto): Long {
        val id = siguienteId++
        gastos.add(gasto.copy(id = id))
        return id
    }

    override suspend fun modificarGasto(gasto: Gasto): Boolean {
        val i = gastos.indexOfFirst { it.id == gasto.id }
        if (i == -1) return false
        gastos[i] = gasto
        return true
    }

    override suspend fun eliminarGasto(id: Long) = gastos.removeIf { it.id == id }

    override suspend fun listarGastos(): List<Gasto> = gastos.toList()

    override suspend fun guardarIngreso(ingreso: Ingreso): Long {
        val id = siguienteId++
        ingresos.add(ingreso.copy(id = id))
        return id
    }

    override suspend fun modificarIngreso(ingreso: Ingreso): Boolean {
        val i = ingresos.indexOfFirst { it.id == ingreso.id }
        if (i == -1) return false
        ingresos[i] = ingreso
        return true
    }

    override suspend fun eliminarIngreso(id: Long) = ingresos.removeIf { it.id == id }

    override suspend fun listarIngresos(): List<Ingreso> = ingresos.toList()
}