package edu.umg.programacion2.gastos.modelo

data class Ingreso(
    val id: Long = 0,
    val usuarioId: Long = 0,
    val descripcion: String,
    val monto: Double,
    val fecha: String,                 // formato yyyy-MM-dd
    val sincronizado: Boolean? = null  // null = solo existe en el teléfono
)