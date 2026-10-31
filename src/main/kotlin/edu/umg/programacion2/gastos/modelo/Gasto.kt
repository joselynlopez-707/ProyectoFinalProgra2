package edu.umg.programacion2.gastos.modelo

data class Gasto(
    val id: Long = 0,
    val usuarioId: Long = 0,
    val descripcion: String,
    val monto: Double,
    val categoria: Categoria,
    val fecha: String,                 // formato yyyy-MM-dd
    val sincronizado: Boolean? = null
)