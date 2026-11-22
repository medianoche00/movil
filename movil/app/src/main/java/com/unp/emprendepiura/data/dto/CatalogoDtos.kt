package com.unp.emprendepiura.data.dto

import com.unp.emprendepiura.domain.model.Categoria
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.EstadoEmprendimiento
import com.unp.emprendepiura.domain.model.EstadoProducto
import com.unp.emprendepiura.domain.model.Producto
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoriaDto(
    val id: String = "",
    val nombre: String,
    val icono: String = "category",
    val orden: Int = 0,
    val activa: Boolean = true
)

@Serializable
data class CategoriaWriteDto(
    val nombre: String,
    val icono: String = "category",
    val orden: Int = 0,
    val activa: Boolean = true
)

@Serializable
data class EmprendimientoDto(
    val id: String = "",
    @SerialName("usuario_id")
    val usuarioId: String,
    @SerialName("nombre_tienda")
    val nombreTienda: String,
    val descripcion: String = "",
    @SerialName("categoria_principal")
    val categoriaPrincipal: String = "Alimentos y Postres",
    @SerialName("zona_piura")
    val zonaPiura: String = "Campus UNP",
    @SerialName("punto_encuentro_habitual")
    val puntoEncuentroHabitual: String = "Puerta Principal UNP",
    @SerialName("telefono_contacto")
    val telefonoContacto: String = "",
    @SerialName("numero_yape_plin")
    val numeroYapePlin: String? = null,
    @SerialName("qr_pago_url")
    val qrPagoUrl: String? = null,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    val estado: String = "PENDIENTE",
    @SerialName("motivo_revision")
    val motivoRevision: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class EmprendimientoWriteDto(
    @SerialName("usuario_id")
    val usuarioId: String,
    @SerialName("nombre_tienda")
    val nombreTienda: String,
    val descripcion: String,
    @SerialName("categoria_principal")
    val categoriaPrincipal: String,
    @SerialName("zona_piura")
    val zonaPiura: String,
    @SerialName("punto_encuentro_habitual")
    val puntoEncuentroHabitual: String,
    @SerialName("telefono_contacto")
    val telefonoContacto: String,
    @SerialName("numero_yape_plin")
    val numeroYapePlin: String,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val estado: String = "PENDIENTE",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("motivo_revision")
    val motivoRevision: String = ""
)

@Serializable
data class ProductoDto(
    val id: String = "",
    @SerialName("emprendimiento_id")
    val emprendimientoId: String,
    @SerialName("categoria_id")
    val categoriaId: String,
    val titulo: String,
    val descripcion: String = "",
    val precio: Double,
    val stock: Int = 1,
    @SerialName("zona_piura")
    val zonaPiura: String = "Campus UNP",
    @SerialName("imagen_url")
    val imagenUrl: String? = null,
    val estado: String = "ACTIVO",
    val destacado: Boolean = false
)

@Serializable
data class ProductoWriteDto(
    @SerialName("emprendimiento_id")
    val emprendimientoId: String,
    @SerialName("categoria_id")
    val categoriaId: String,
    val titulo: String,
    val descripcion: String,
    val precio: Double,
    val stock: Int,
    @SerialName("zona_piura")
    val zonaPiura: String,
    @SerialName("imagen_url")
    val imagenUrl: String? = null,
    val estado: String = "ACTIVO"
)

fun CategoriaDto.toDomain() = Categoria(id, nombre, icono, orden, activa)

fun EmprendimientoDto.toDomain() = Emprendimiento(
    id = id,
    usuarioId = usuarioId,
    nombreTienda = nombreTienda,
    descripcion = descripcion,
    categoriaPrincipal = categoriaPrincipal,
    zonaPiura = zonaPiura,
    puntoEncuentroHabitual = puntoEncuentroHabitual,
    telefonoContacto = telefonoContacto,
    numeroYapePlin = numeroYapePlin.orEmpty(),
    qrPagoUrl = qrPagoUrl.orEmpty(),
    logoUrl = logoUrl.orEmpty(),
    estado = EstadoEmprendimiento.fromRaw(estado),
    motivoRevision = motivoRevision.orEmpty()
)

fun ProductoDto.toDomain(
    nombreTienda: String = "",
    nombreCategoria: String = ""
) = Producto(
    id = id,
    emprendimientoId = emprendimientoId,
    categoriaId = categoriaId,
    titulo = titulo,
    descripcion = descripcion,
    precio = precio,
    stock = stock,
    zonaPiura = zonaPiura,
    imagenUrl = imagenUrl.orEmpty(),
    estado = EstadoProducto.fromRaw(estado),
    destacado = destacado,
    nombreTienda = nombreTienda,
    nombreCategoria = nombreCategoria
)
