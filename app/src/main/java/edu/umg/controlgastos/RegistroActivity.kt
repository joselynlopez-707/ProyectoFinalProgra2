package edu.umg.controlgastos

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import edu.umg.controlgastos.databinding.ActivityRegistroBinding

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repo = RepositorioSQLite(applicationContext)

        binding.btnRegistrar.setOnClickListener {
            val nombres = binding.etNombres.text.toString().trim()
            val username = binding.etUsername.text.toString().trim().removePrefix("@")
            val gmail = binding.etGmail.text.toString().trim()

            if (nombres.isBlank()) {
                binding.etNombres.error = "Escribe tu nombre"
                return@setOnClickListener
            }
            if (username.length < 3 || username.length > 30) {
                binding.etUsername.error = "Entre 3 y 30 caracteres"
                return@setOnClickListener
            }
            if (repo.existeUsername(username)) {
                binding.etUsername.error = "Ese username ya existe"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(gmail).matches()) {
                binding.etGmail.error = "Escribe un correo válido"
                return@setOnClickListener
            }
            if (binding.rgGenero.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Elige si eres mujer u hombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val genero = if (binding.rbMujer.isChecked) "mujer" else "hombre"
            val nuevo = Perfil(nombres = nombres, gmail = gmail, username = username, genero = genero)
            val id = repo.crearPerfil(nuevo)
            repo.activar(nuevo.copy(id = id))

            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
}