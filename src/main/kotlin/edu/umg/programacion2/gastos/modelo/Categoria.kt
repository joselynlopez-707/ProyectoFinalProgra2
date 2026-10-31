package edu.umg.programacion2.gastos.modelo

enum class Categoria(private val nombre: String) {
    ALIMENTACION("Alimentación"),
    TRANSPORTE("Transporte"),
    SERVICIOS("Servicios"),
    SALUD("Salud"),
    EDUCACION("Educación"),
    ENTRETENIMIENTO("Entretenimiento"),
    OTROS("Otros");

    override fun toString() = nombre
}