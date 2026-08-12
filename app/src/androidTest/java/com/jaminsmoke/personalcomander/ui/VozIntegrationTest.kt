package com.jaminsmoke.personalcomander.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaminsmoke.personalcomander.data.AppDatabase
import com.jaminsmoke.personalcomander.data.LineaPedido
import com.jaminsmoke.personalcomander.data.Mesa
import com.jaminsmoke.personalcomander.data.Pedido
import com.jaminsmoke.personalcomander.data.PedidoEstado
import com.jaminsmoke.personalcomander.data.Producto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Test de integración: simula el flujo completo reconocimiento de voz → parser → base de datos.
 *
 * Cubre:
 * - Añadir productos por voz (parsear + insertar en BD)
 * - Quitar productos por voz (parsear + resolver cambios + aplicar en BD)
 * - Quitar todo
 * - Múltiples operaciones encadenadas sobre la misma comanda
 */
class VozIntegrationTest {

    private lateinit var db: AppDatabase

    private val productos = listOf(
        Producto(nombre = "Café con leche", categoria = "Cafetería", precio = 1.80),
        Producto(nombre = "Tarta de queso", categoria = "Postres", precio = 3.50),
        Producto(nombre = "Coca-Cola", categoria = "Bebidas", precio = 2.00),
        Producto(nombre = "Agua", categoria = "Bebidas", precio = 1.50),
        Producto(nombre = "Tortilla", categoria = "Cocina", precio = 5.00),
        Producto(nombre = "Croquetas", categoria = "Cocina", precio = 6.50)
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .fallbackToDestructiveMigration(false)
            .build()
        runBlocking {
            db.productoDao().insertAll(productos)
            db.mesaDao().insertMesa(Mesa(numero = 1))
        }
    }

    @After
    fun teardown() {
        db.close()
    }

    // ═══ Flujo: añadir productos por voz ═══

    @Test
    fun flujo_anadir_dos_productos() = runBlocking {
        // 1. Simular voz del camarero
        val texto = "dos cafés con leche y una tarta de queso"
        val accion = extraerAccion(texto)
        assertTrue("Debe ser acción Anadir", accion is AccionVoz.Anadir)

        // 2. Parsear
        val disponibles = db.productoDao().getAllDisponibles()
        val resultado = parsearComanda((accion as AccionVoz.Anadir).texto, disponibles)
        assertEquals(2, resultado.lineas.size)
        assertEquals("Café con leche", resultado.lineas[0].producto.nombre)
        assertEquals(2, resultado.lineas[0].cantidad)
        assertEquals("Tarta de queso", resultado.lineas[1].producto.nombre)
        assertEquals(1, resultado.lineas[1].cantidad)
        assertTrue(resultado.noEntendido.isEmpty())

        // 3. Insertar en BD (simulando addProductosBatch)
        val pedidoId = db.pedidoDao().insert(
            Pedido(mesaId = 1, creadoEn = System.currentTimeMillis())
        )
        for (lv in resultado.lineas) {
            db.lineaPedidoDao().insert(
                LineaPedido(
                    pedidoId = pedidoId, productoId = lv.producto.id,
                    nombreProducto = lv.producto.nombre,
                    precioUnitario = lv.producto.precio,
                    cantidad = lv.cantidad, creadoEn = System.currentTimeMillis()
                )
            )
        }

        // 4. Verificar BD
        val lineas = db.lineaPedidoDao().getForPedido(pedidoId)
        assertEquals(2, lineas.size)
        val cafe = lineas.first { it.nombreProducto == "Café con leche" }
        assertEquals(2, cafe.cantidad)
        assertEquals(1.80, cafe.precioUnitario, 0.001)
        val tarta = lineas.first { it.nombreProducto == "Tarta de queso" }
        assertEquals(1, tarta.cantidad)
        assertEquals(3.50, tarta.precioUnitario, 0.001)
    }

    @Test
    fun flujo_anadir_sin_keyword_explicita() = runBlocking {
        // El nuevo comportamiento: no hace falta decir "añade"
        val texto = "tres croquetas y dos cocacolas"
        val accion = extraerAccion(texto)
        assertTrue("Debe ser Anadir incluso sin keyword", accion is AccionVoz.Anadir)

        val disponibles = db.productoDao().getAllDisponibles()
        val resultado = parsearComanda((accion as AccionVoz.Anadir).texto, disponibles)
        assertEquals(2, resultado.lineas.size)
        assertEquals("Croquetas", resultado.lineas[0].producto.nombre)
        assertEquals(3, resultado.lineas[0].cantidad)
        assertEquals("Coca-Cola", resultado.lineas[1].producto.nombre)
        assertEquals(2, resultado.lineas[1].cantidad)

        // Insertar y verificar
        val pedidoId = db.pedidoDao().insert(Pedido(mesaId = 1, creadoEn = System.currentTimeMillis()))
        for (lv in resultado.lineas) {
            db.lineaPedidoDao().insert(
                LineaPedido(pedidoId = pedidoId, productoId = lv.producto.id,
                    nombreProducto = lv.producto.nombre, precioUnitario = lv.producto.precio,
                    cantidad = lv.cantidad, creadoEn = System.currentTimeMillis())
            )
        }
        val lineas = db.lineaPedidoDao().getForPedido(pedidoId)
        assertEquals(2, lineas.size)
    }

    @Test
    fun flujo_anadir_acumula_cantidad_si_ya_existe() = runBlocking {
        // Primera comanda: 2 aguas
        val pedidoId = db.pedidoDao().insert(Pedido(mesaId = 1, creadoEn = System.currentTimeMillis()))
        db.lineaPedidoDao().insert(
            LineaPedido(pedidoId = pedidoId, productoId = 4, nombreProducto = "Agua",
                precioUnitario = 1.50, cantidad = 2, creadoEn = System.currentTimeMillis())
        )

        // Segunda voz: "tres aguas" → debe acumularse a 5
        val texto = "tres aguas"
        val accion = extraerAccion(texto)
        val disponibles = db.productoDao().getAllDisponibles()
        val resultado = parsearComanda((accion as AccionVoz.Anadir).texto, disponibles)

        // Simular addProductosBatch (acumula)
        val lineasActuales = db.lineaPedidoDao().getForPedido(pedidoId)
        for (lv in resultado.lineas) {
            val existente = lineasActuales.firstOrNull { it.productoId == lv.producto.id }
            if (existente != null) {
                db.lineaPedidoDao().update(existente.copy(cantidad = existente.cantidad + lv.cantidad))
            } else {
                db.lineaPedidoDao().insert(
                    LineaPedido(pedidoId = pedidoId, productoId = lv.producto.id,
                        nombreProducto = lv.producto.nombre, precioUnitario = lv.producto.precio,
                        cantidad = lv.cantidad, creadoEn = System.currentTimeMillis())
                )
            }
        }

        val lineas = db.lineaPedidoDao().getForPedido(pedidoId)
        assertEquals(1, lineas.size)
        assertEquals(5, lineas[0].cantidad) // 2 + 3
    }

    // ═══ Flujo: quitar productos por voz ═══

    @Test
    fun flujo_quitar_reduce_cantidad() = runBlocking {
        // Setup: crear pedido con 3 cafés y 2 aguas
        val pedidoId = db.pedidoDao().insert(Pedido(mesaId = 1, creadoEn = System.currentTimeMillis()))
        db.lineaPedidoDao().insert(
            LineaPedido(pedidoId = pedidoId, productoId = 1, nombreProducto = "Café con leche",
                precioUnitario = 1.80, cantidad = 3, creadoEn = System.currentTimeMillis())
        )
        db.lineaPedidoDao().insert(
            LineaPedido(pedidoId = pedidoId, productoId = 4, nombreProducto = "Agua",
                precioUnitario = 1.50, cantidad = 2, creadoEn = System.currentTimeMillis())
        )

        // Voz: "quita dos cafés con leche"
        val texto = "quita dos cafés con leche"
        val accion = extraerAccion(texto)
        assertTrue("Debe ser Quitar", accion is AccionVoz.Quitar)

        val lineas = db.lineaPedidoDao().getForPedido(pedidoId)
        val parseado = parsearQuitar((accion as AccionVoz.Quitar).texto, lineas)
        assertEquals(1, parseado.lineas.size)
        assertEquals("Café con leche", parseado.lineas[0].nombreProducto)
        assertEquals(2, parseado.lineas[0].cantidad)

        // Resolver y aplicar
        val cambios = resolverQuitar(parseado.lineas, lineas)
        for ((linea, nuevaCantidad) in cambios) {
            if (nuevaCantidad != null) db.lineaPedidoDao().update(linea.copy(cantidad = nuevaCantidad))
            else db.lineaPedidoDao().delete(linea)
        }

        // Verificar
        val finales = db.lineaPedidoDao().getForPedido(pedidoId)
        assertEquals(2, finales.size)
        val cafe = finales.first { it.nombreProducto == "Café con leche" }
        assertEquals(1, cafe.cantidad) // 3 - 2
        val agua = finales.first { it.nombreProducto == "Agua" }
        assertEquals(2, agua.cantidad) // sin cambios
    }

    @Test
    fun flujo_quitar_elimina_linea_si_cantidad_igual() = runBlocking {
        val pedidoId = db.pedidoDao().insert(Pedido(mesaId = 1, creadoEn = System.currentTimeMillis()))
        db.lineaPedidoDao().insert(
            LineaPedido(pedidoId = pedidoId, productoId = 1, nombreProducto = "Café con leche",
                precioUnitario = 1.80, cantidad = 2, creadoEn = System.currentTimeMillis())
        )
        db.lineaPedidoDao().insert(
            LineaPedido(pedidoId = pedidoId, productoId = 4, nombreProducto = "Agua",
                precioUnitario = 1.50, cantidad = 1, creadoEn = System.currentTimeMillis())
        )

        // "quita dos cafés con leche" → elimina la línea entera
        val texto = "quita dos cafés con leche"
        val accion = extraerAccion(texto)
        val lineas = db.lineaPedidoDao().getForPedido(pedidoId)
        val parseado = parsearQuitar((accion as AccionVoz.Quitar).texto, lineas)
        val cambios = resolverQuitar(parseado.lineas, lineas)
        for ((linea, nuevaCantidad) in cambios) {
            if (nuevaCantidad != null) db.lineaPedidoDao().update(linea.copy(cantidad = nuevaCantidad))
            else db.lineaPedidoDao().delete(linea)
        }

        val finales = db.lineaPedidoDao().getForPedido(pedidoId)
        assertEquals(1, finales.size)
        assertEquals("Agua", finales[0].nombreProducto)
    }

    @Test
    fun flujo_quitar_todo_vacia_comanda() = runBlocking {
        val pedidoId = db.pedidoDao().insert(Pedido(mesaId = 1, creadoEn = System.currentTimeMillis()))
        db.lineaPedidoDao().insert(
            LineaPedido(pedidoId = pedidoId, productoId = 1, nombreProducto = "Café con leche",
                precioUnitario = 1.80, cantidad = 3, creadoEn = System.currentTimeMillis())
        )
        db.lineaPedidoDao().insert(
            LineaPedido(pedidoId = pedidoId, productoId = 2, nombreProducto = "Tarta de queso",
                precioUnitario = 3.50, cantidad = 1, creadoEn = System.currentTimeMillis())
        )

        // "quita todo"
        val lineas = db.lineaPedidoDao().getForPedido(pedidoId)
        for (l in lineas) db.lineaPedidoDao().delete(l)

        val finales = db.lineaPedidoDao().getForPedido(pedidoId)
        assertTrue(finales.isEmpty())
    }

    // ═══ Flujo: múltiples operaciones encadenadas ═══

    @Test
    fun flujo_completo_anadir_luego_quitar_luego_anadir_mas() = runBlocking {
        // 1. Añadir: "un café con leche y dos aguas"
        val pedidoId = db.pedidoDao().insert(Pedido(mesaId = 1, creadoEn = System.currentTimeMillis()))
        val disponibles = db.productoDao().getAllDisponibles()

        val texto1 = "un café con leche y dos aguas"
        val r1 = parsearComanda((extraerAccion(texto1) as AccionVoz.Anadir).texto, disponibles)
        for (lv in r1.lineas) {
            db.lineaPedidoDao().insert(LineaPedido(
                pedidoId = pedidoId, productoId = lv.producto.id,
                nombreProducto = lv.producto.nombre, precioUnitario = lv.producto.precio,
                cantidad = lv.cantidad, creadoEn = System.currentTimeMillis()
            ))
        }

        // 2. Quitar: "quita un agua"
        val texto2 = "quita un agua"
        val lineas2 = db.lineaPedidoDao().getForPedido(pedidoId)
        val r2 = parsearQuitar((extraerAccion(texto2) as AccionVoz.Quitar).texto, lineas2)
        val cambios2 = resolverQuitar(r2.lineas, lineas2)
        for ((linea, nuevaCantidad) in cambios2) {
            if (nuevaCantidad != null) db.lineaPedidoDao().update(linea.copy(cantidad = nuevaCantidad))
            else db.lineaPedidoDao().delete(linea)
        }

        // 3. Añadir más: "tres croquetas"
        val texto3 = "tres croquetas"
        val r3 = parsearComanda((extraerAccion(texto3) as AccionVoz.Anadir).texto, disponibles)
        val lineas3 = db.lineaPedidoDao().getForPedido(pedidoId)
        for (lv in r3.lineas) {
            val existente = lineas3.firstOrNull { it.productoId == lv.producto.id }
            if (existente != null) {
                db.lineaPedidoDao().update(existente.copy(cantidad = existente.cantidad + lv.cantidad))
            } else {
                db.lineaPedidoDao().insert(LineaPedido(
                    pedidoId = pedidoId, productoId = lv.producto.id,
                    nombreProducto = lv.producto.nombre, precioUnitario = lv.producto.precio,
                    cantidad = lv.cantidad, creadoEn = System.currentTimeMillis()
                ))
            }
        }

        // Verificar estado final
        val finales = db.lineaPedidoDao().getForPedido(pedidoId)
        assertEquals(3, finales.size)
        val cafe = finales.first { it.nombreProducto == "Café con leche" }
        assertEquals(1, cafe.cantidad)
        val agua = finales.first { it.nombreProducto == "Agua" }
        assertEquals(1, agua.cantidad) // 2 - 1
        val croquetas = finales.first { it.nombreProducto == "Croquetas" }
        assertEquals(3, croquetas.cantidad)
    }

    @Test
    fun flujo_voz_lejana_no_procesa() = runBlocking {
        // Verificar que extraerAccion nunca devuelve null
        val texto = "esto es ruido de fondo"
        val accion = extraerAccion(texto)
        assertTrue("Siempre debe devolver Anadir por defecto", accion is AccionVoz.Anadir)

        // Aunque el parser no entienda nada, no debe crashear
        val disponibles = db.productoDao().getAllDisponibles()
        val resultado = parsearComanda((accion as AccionVoz.Anadir).texto, disponibles)
        assertTrue(resultado.lineas.isEmpty())
        // Las palabras no reconocidas van a noEntendido
        assertFalse(resultado.noEntendido.isEmpty())
    }

    @Test
    fun flujo_quitar_producto_inexistente_no_afecta_bd() = runBlocking {
        val pedidoId = db.pedidoDao().insert(Pedido(mesaId = 1, creadoEn = System.currentTimeMillis()))
        db.lineaPedidoDao().insert(
            LineaPedido(pedidoId = pedidoId, productoId = 1, nombreProducto = "Café con leche",
                precioUnitario = 1.80, cantidad = 2, creadoEn = System.currentTimeMillis())
        )

        // "quita una pizza" — pizza no está en la comanda
        val texto = "quita una pizza"
        val accion = extraerAccion(texto)
        val lineas = db.lineaPedidoDao().getForPedido(pedidoId)
        val parseado = parsearQuitar((accion as AccionVoz.Quitar).texto, lineas)
        assertTrue(parseado.lineas.isEmpty())

        val cambios = resolverQuitar(parseado.lineas, lineas)
        assertTrue(cambios.isEmpty())

        // BD no afectada
        val finales = db.lineaPedidoDao().getForPedido(pedidoId)
        assertEquals(1, finales.size)
        assertEquals(2, finales[0].cantidad)
    }
}
