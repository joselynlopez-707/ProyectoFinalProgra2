package edu.umg.controlgastos

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import edu.umg.controlgastos.databinding.ActivityAjustesBinding

class AjustesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAjustesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("perfil", MODE_PRIVATE)
        val oscuro = prefs.getBoolean("oscuro", false)
        AppCompatDelegate.setDefaultNightMode(
            if (oscuro) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        binding = ActivityAjustesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnPerfiles.setOnClickListener {
            startActivity(android.content.Intent(this, PerfilesActivity::class.java))
        }

        binding.swOscuro.isChecked = oscuro
        binding.swOscuro.setOnCheckedChangeListener { _, marcado ->
            prefs.edit().putBoolean("oscuro", marcado).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (marcado) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        if (Moneda.esDolar(this)) binding.rbDolar.isChecked = true else binding.rbQuetzal.isChecked = true
        binding.etCambio.setText(Moneda.tipoCambio(this).toString())

        binding.btnGuardarAjustes.setOnClickListener {
            val cambio = binding.etCambio.text.toString().toDoubleOrNull()
            if (cambio == null || cambio <= 0) {
                binding.etCambio.error = "Tipo de cambio inválido"
                return@setOnClickListener
            }
            Moneda.guardar(this, binding.rbDolar.isChecked, cambio)
            Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}