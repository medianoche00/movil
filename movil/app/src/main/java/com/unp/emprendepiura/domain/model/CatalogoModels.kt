package com.unp.emprendepiura.domain.model

/*
 * Archivo: CatalogoModels.kt
 * Proposito: Modelos de dominio para Categorias, Emprendimientos locales y Productos.
 */
enum class EstadoEmprendimiento(val etiqueta: String) {
    PENDIENTE("Pendiente de aprobación"),
    APROBADO("Aprobado"),
    SUSPENDIDO("Suspendido");

    companion object {
        fun fromRaw(raw: String): EstadoEmprendimiento =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: PENDIENTE
    }
}

enum class EstadoProducto(val etiqueta: String) {
    ACTIVO("Activo"),
    PAUSADO("Pausado"),
    OCULTO_ADMIN("Oculto por Admin");

    companion object {
        fun fromRaw(raw: String): EstadoProducto =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: ACTIVO
    }
}

data class Categoria(
    val id: String,
    val nombre: String,
    val icono: String = "category",
    val orden: Int = 0,
    val activa: Boolean = true
)

data class Emprendimiento(
    val id: String = "",
    val usuarioId: String,
    val nombreTienda: String,
    val descripcion: String = "",
    val categoriaPrincipal: String = "Alimentos y Postres",
    val zonaPiura: String = "Campus UNP",
    val puntoEncuentroHabitual: String = "Puerta Principal UNP",
    val telefonoContacto: String,
    val numeroYapePlin: String = "",
    val qrPagoUrl: String = "",
    val logoUrl: String = "",
    val estado: EstadoEmprendimiento = EstadoEmprendimiento.PENDIENTE,
    val motivoRevision: String = ""
)

data class Producto(
    val id: String = "",
    val emprendimientoId: String,
    val categoriaId: String,
    val titulo: String,
    val descripcion: String = "",
    val precio: Double,
    val stock: Int = 1,
    val zonaPiura: String = "Campus UNP",
    val imagenUrl: String = "",
    val estado: EstadoProducto = EstadoProducto.ACTIVO,
    val destacado: Boolean = false,
    val nombreTienda: String = "",
    val nombreCategoria: String = ""
)
