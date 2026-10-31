package edu.umg.controlgastos

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import edu.umg.programacion2.gastos.datos.Repositorio
import edu.umg.programacion2.gastos.modelo.Categoria
import edu.umg.programacion2.gastos.modelo.Gasto
import edu.umg.programacion2.gastos.modelo.Ingreso
import edu.umg.programacion2.gastos.modelo.Usuario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Nota(
    val id: Long,
    val texto: String,
    val comprado: Boolean,
    val fecha: String?,
    val fechaCompra: String?,
    val monto: Double = 0.0
)

data class Perfil(
    val id: Long = 0,
    val nombres: String,
    val gmail: String,
    val username: String,
    val genero: String,
    val passHash: String? = null
)

class RepositorioSQLite(private val ctx: Context) :
    SQLiteOpenHelper(ctx, "gastos.db", null, 7), Repositorio {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE gastos (id INTEGER PRIMARY KEY AUTOINCREMENT, usuarioId INTEGER, descripcion TEXT, monto REAL, categoria TEXT, fecha TEXT, sincronizado INTEGER)")
        db.execSQL("CREATE TABLE ingresos (id INTEGER PRIMARY KEY AUTOINCREMENT, usuarioId INTEGER, descripcion TEXT, monto REAL, fecha TEXT, sincronizado INTEGER)")
        crearUsuarios(db)
        crearNotas(db)
    }

    private fun tieneColumna(db: SQLiteDatabase, tabla: String, columna: String): Boolean =
        db.rawQuery("PRAGMA table_info($tabla)", null).use { c ->
            var existe = false
            while (c.moveToNext()) if (c.getString(1) == columna) existe = true
            existe
        }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        if (o < 2) crearUsuarios(db)
        if (o < 3) {
            crearNotas(db)
        } else if (o < 4) {
            db.execSQL("ALTER TABLE notas ADD COLUMN fecha TEXT")
            db.execSQL("ALTER TABLE notas ADD COLUMN fechaCompra TEXT")
        }
        if (o < 5) {
            if (o >= 2) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN username TEXT")
                db.execSQL("ALTER TABLE usuarios ADD COLUMN genero TEXT")
                db.execSQL("ALTER TABLE usuarios ADD COLUMN passHash TEXT")
            }
            val g = ctx.getSharedPreferences("perfil", Context.MODE_PRIVATE).getString("genero", "") ?: ""
            db.execSQL("UPDATE usuarios SET genero=? WHERE genero IS NULL", arrayOf<Any>(g))
            db.execSQL("UPDATE usuarios SET username=lower(replace(nombres,' ','')) WHERE username IS NULL")
            db.execSQL("UPDATE gastos SET usuarioId=(SELECT MIN(id) FROM usuarios)")
            db.execSQL("UPDATE ingresos SET usuarioId=(SELECT MIN(id) FROM usuarios)")
        }
        if (o < 6) {
            if (!tieneColumna(db, "notas", "usuarioId")) db.execSQL("ALTER TABLE notas ADD COLUMN usuarioId INTEGER")
            db.execSQL("UPDATE notas SET usuarioId=(SELECT MIN(id) FROM usuarios) WHERE usuarioId IS NULL")
        }
        if (o < 7) {
            if (!tieneColumna(db, "notas", "monto")) db.execSQL("ALTER TABLE notas ADD COLUMN monto REAL DEFAULT 0")
        }
    }

    private fun crearUsuarios(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nombres TEXT, gmail TEXT, username TEXT, genero TEXT, passHash TEXT)")
    }

    private fun crearNotas(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS notas (id INTEGER PRIMARY KEY AUTOINCREMENT, texto TEXT, comprado INTEGER DEFAULT 0, fecha TEXT, fechaCompra TEXT, usuarioId INTEGER, monto REAL DEFAULT 0)")
    }

    private fun hoy() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun prefs() = ctx.getSharedPreferences("perfil", Context.MODE_PRIVATE)

    // ---------- Perfiles ----------
    private fun aPerfil(c: Cursor) = Perfil(
        c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "",
        c.getString(3) ?: "", c.getString(4) ?: "",
        if (c.isNull(5)) null else c.getString(5)
    )

    fun crearPerfil(p: Perfil): Long {
        val v = ContentValues().apply {
            put("nombres", p.nombres); put("gmail", p.gmail)
            put("username", p.username); put("genero", p.genero)
            if (p.passHash == null) putNull("passHash") else put("passHash", p.passHash)
        }
        return writableDatabase.insert("usuarios", null, v)
    }

    fun listarPerfiles(): List<Perfil> =
        readableDatabase.rawQuery("SELECT id,nombres,gmail,username,genero,passHash FROM usuarios ORDER BY id", null).use { c ->
            buildList { while (c.moveToNext()) add(aPerfil(c)) }
        }

    fun perfilActivo(): Perfil? {
        val id = prefs().getLong("usuario_activo", -1L)
        val lista = listarPerfiles()
        val p = lista.firstOrNull { it.id == id } ?: lista.firstOrNull() ?: return null
        if (p.id != id) activar(p)
        return p
    }

    fun activar(p: Perfil) {
        prefs().edit()
            .putLong("usuario_activo", p.id)
            .putString("genero", p.genero)
            .apply()
    }

    private fun idActivo(): Long = perfilActivo()?.id ?: -1L

    fun obtenerUsuario(): Usuario? = perfilActivo()?.let { Usuario(it.id, it.nombres, it.gmail) }

    // ---------- Opciones de perfil ----------
    fun usaUsername(id: Long) = prefs().getBoolean("usar_username_$id", false)

    fun fijarUsaUsername(id: Long, valor: Boolean) {
        prefs().edit().putBoolean("usar_username_$id", valor).apply()
    }

    fun nombreParaMostrar(): String {
        val p = perfilActivo() ?: return ""
        return if (usaUsername(p.id)) "@${p.username}" else p.nombres.split(" ").first()
    }

    fun existeUsername(username: String, exceptoId: Long = -1L) =
        listarPerfiles().any { it.id != exceptoId && it.username.equals(username, ignoreCase = true) }

    fun cambiarUsername(id: Long, nuevo: String) {
        val v = ContentValues().apply { put("username", nuevo) }
        writableDatabase.update("usuarios", v, "id=?", arrayOf(id.toString()))
    }

    fun eliminarPerfil(id: Long) {
        val db = writableDatabase
        db.delete("gastos", "usuarioId=?", arrayOf(id.toString()))
        db.delete("ingresos", "usuarioId=?", arrayOf(id.toString()))
        db.delete("notas", "usuarioId=?", arrayOf(id.toString()))
        db.delete("usuarios", "id=?", arrayOf(id.toString()))
        prefs().edit().remove("usar_username_$id").apply()
        if (prefs().getLong("usuario_activo", -1L) == id) {
            prefs().edit().remove("usuario_activo").apply()
        }
    }

    // ---------- Notas de compras (por perfil, con precio) ----------
    fun guardarNota(texto: String, monto: Double): Long {
        val v = ContentValues().apply {
            put("texto", texto); put("comprado", 0); put("fecha", hoy())
            put("usuarioId", idActivo()); put("monto", monto)
        }
        return writableDatabase.insert("notas", null, v)
    }

    fun editarNota(id: Long, texto: String, monto: Double) {
        val v = ContentValues().apply { put("texto", texto); put("monto", monto) }
        writableDatabase.update("notas", v, "id=?", arrayOf(id.toString()))
    }

    fun listarNotas(): List<Nota> =
        readableDatabase.rawQuery(
            "SELECT id,texto,comprado,fecha,fechaCompra,monto FROM notas WHERE usuarioId=? ORDER BY id DESC",
            arrayOf(idActivo().toString())
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(
                    Nota(c.getLong(0), c.getString(1) ?: "", c.getInt(2) == 1,
                        if (c.isNull(3)) null else c.getString(3),
                        if (c.isNull(4)) null else c.getString(4),
                        if (c.isNull(5)) 0.0 else c.getDouble(5))
                )
            }
        }

    fun marcarNota(id: Long, comprado: Boolean) {
        val v = ContentValues().apply {
            put("comprado", if (comprado) 1 else 0)
            if (comprado) put("fechaCompra", hoy()) else putNull("fechaCompra")
        }
        writableDatabase.update("notas", v, "id=?", arrayOf(id.toString()))
    }

    fun eliminarNota(id: Long) {
        writableDatabase.delete("notas", "id=?", arrayOf(id.toString()))
    }

    // ---------- Gastos e ingresos ----------
    private fun valores(g: Gasto) = ContentValues().apply {
        put("usuarioId", g.usuarioId); put("descripcion", g.descripcion)
        put("monto", g.monto); put("categoria", g.categoria.name); put("fecha", g.fecha)
        if (g.sincronizado == null) putNull("sincronizado") else put("sincronizado", if (g.sincronizado!!) 1 else 0)
    }

    private fun valores(i: Ingreso) = ContentValues().apply {
        put("usuarioId", i.usuarioId); put("descripcion", i.descripcion)
        put("monto", i.monto); put("fecha", i.fecha)
        if (i.sincronizado == null) putNull("sincronizado") else put("sincronizado", if (i.sincronizado!!) 1 else 0)
    }

    override suspend fun guardarGasto(gasto: Gasto) = writableDatabase.insert("gastos", null, valores(gasto))
    override suspend fun modificarGasto(gasto: Gasto) =
        writableDatabase.update("gastos", valores(gasto), "id=?", arrayOf(gasto.id.toString())) > 0
    override suspend fun eliminarGasto(id: Long) =
        writableDatabase.delete("gastos", "id=?", arrayOf(id.toString())) > 0

    override suspend fun listarGastos(): List<Gasto> =
        readableDatabase.rawQuery("SELECT id,usuarioId,descripcion,monto,categoria,fecha,sincronizado FROM gastos WHERE usuarioId=? ORDER BY fecha DESC, id DESC", arrayOf(idActivo().toString())).use { c ->
            buildList {
                while (c.moveToNext()) add(
                    Gasto(c.getLong(0), c.getLong(1), c.getString(2), c.getDouble(3),
                        Categoria.valueOf(c.getString(4)), c.getString(5),
                        if (c.isNull(6)) null else c.getInt(6) == 1)
                )
            }
        }

    override suspend fun guardarIngreso(ingreso: Ingreso) = writableDatabase.insert("ingresos", null, valores(ingreso))
    override suspend fun modificarIngreso(ingreso: Ingreso) =
        writableDatabase.update("ingresos", valores(ingreso), "id=?", arrayOf(ingreso.id.toString())) > 0
    override suspend fun eliminarIngreso(id: Long) =
        writableDatabase.delete("ingresos", "id=?", arrayOf(id.toString())) > 0

    override suspend fun listarIngresos(): List<Ingreso> =
        readableDatabase.rawQuery("SELECT id,usuarioId,descripcion,monto,fecha,sincronizado FROM ingresos WHERE usuarioId=? ORDER BY fecha DESC, id DESC", arrayOf(idActivo().toString())).use { c ->
            buildList {
                while (c.moveToNext()) add(
                    Ingreso(c.getLong(0), c.getLong(1), c.getString(2), c.getDouble(3), c.getString(4),
                        if (c.isNull(5)) null else c.getInt(5) == 1)
                )
            }
        }
}