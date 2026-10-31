package edu.umg.controlgastos

import android.content.Context

object Moneda {

    fun esDolar(c: Context) =
        c.getSharedPreferences("perfil", Context.MODE_PRIVATE).getBoolean("dolar", false)

    fun tipoCambio(c: Context) =
        c.getSharedPreferences("perfil", Context.MODE_PRIVATE).getFloat("cambio", 7.70f).toDouble()

    fun guardar(c: Context, dolar: Boolean, cambio: Double) {
        c.getSharedPreferences("perfil", Context.MODE_PRIVATE).edit()
            .putBoolean("dolar", dolar)
            .putFloat("cambio", cambio.toFloat())
            .apply()
    }

    // Recibe un monto en quetzales y lo devuelve como texto en la moneda elegida
    fun formato(c: Context, quetzales: Double): String =
        if (esDolar(c)) "US$ %.2f".format(quetzales / tipoCambio(c))
        else "Q %.2f".format(quetzales)
    fun aQuetzales(c: Context, valor: Double): Double =
        if (esDolar(c)) valor * tipoCambio(c) else valor

    fun desdeQuetzales(c: Context, q: Double): Double =
        if (esDolar(c)) q / tipoCambio(c) else q


}