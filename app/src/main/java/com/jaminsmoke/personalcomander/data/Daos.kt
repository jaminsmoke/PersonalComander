package com.jaminsmoke.personalcomander.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaDao {
    @Query("SELECT * FROM salas ORDER BY orden, id")
    fun observeAll(): Flow<List<Sala>>

    @Query("SELECT * FROM salas ORDER BY orden, id")
    suspend fun getAll(): List<Sala>

    @Query("SELECT * FROM salas WHERE id = :id")
    suspend fun getById(id: Long): Sala?

    @Query("SELECT COUNT(*) FROM salas")
    suspend fun count(): Int

    @Insert
    suspend fun insert(sala: Sala): Long

    @Insert
    suspend fun insertAll(salas: List<Sala>)

    @Update
    suspend fun update(sala: Sala)

    @Update
    suspend fun updateAll(salas: List<Sala>)

    @Query("DELETE FROM salas WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COALESCE(MAX(orden), -1) FROM salas")
    suspend fun getMaxOrden(): Int
}

@Dao
interface MesaDao {
    @Query("SELECT * FROM mesas ORDER BY salaId, numero")
    fun observeAll(): Flow<List<Mesa>>

    @Query("SELECT * FROM mesas WHERE id = :id")
    fun observeById(id: Long): Flow<Mesa?>

    @Query("SELECT * FROM mesas ORDER BY salaId, numero")
    suspend fun getAll(): List<Mesa>

    @Query("SELECT COUNT(*) FROM mesas")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM mesas")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM mesas WHERE estado != 'LIBRE' OR bloqueada = 1 OR reservaActivaId IS NOT NULL")
    fun observeOcupadas(): Flow<Int>

    @Insert
    suspend fun insertAll(mesas: List<Mesa>)

    @Update
    suspend fun update(mesa: Mesa)

    @Update
    suspend fun updateAll(mesas: List<Mesa>)

    @Query("SELECT * FROM mesas WHERE id = :id")
    suspend fun getById(id: Long): Mesa?

    @Query("UPDATE mesas SET estado = :estado, comandaActivaId = :comandaId WHERE id = :id")
    suspend fun updateEstado(id: Long, estado: MesaEstado, comandaId: Long?)

    @Query("UPDATE mesas SET bloqueada = :bloqueada WHERE id = :id")
    suspend fun setBloqueada(id: Long, bloqueada: Boolean)

    @Query("UPDATE mesas SET reservaActivaId = :reservaId WHERE id = :id")
    suspend fun setReservaActiva(id: Long, reservaId: Long?)

    @Query("UPDATE mesas SET alias = :alias, capacidad = :capacidad, forma = :forma WHERE id = :id")
    suspend fun updateConfig(id: Long, alias: String?, capacidad: Int, forma: MesaForma)

    @Query("DELETE FROM mesas WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE mesas SET numero = numero - 1 WHERE numero > :numero")
    suspend fun renumberAfter(numero: Int)

    @Query("SELECT COALESCE(MAX(numero), 0) FROM mesas")
    suspend fun getMaxNumero(): Int

    @Query("SELECT COALESCE(MAX(indiceZona), 0) FROM mesas WHERE salaId = :salaId")
    suspend fun getMaxIndiceSala(salaId: Long): Int

    @Query("SELECT * FROM mesas WHERE salaId = :salaId ORDER BY indiceZona")
    suspend fun getPorSala(salaId: Long): List<Mesa>

    @Query("SELECT COUNT(*) FROM mesas WHERE salaId = :salaId")
    suspend fun countPorSala(salaId: Long): Int

    @Query("UPDATE mesas SET indiceZona = indiceZona - 1 WHERE salaId = :salaId AND indiceZona > :indice")
    suspend fun decrementarIndicesSala(salaId: Long, indice: Int)

    @Insert
    suspend fun insertMesa(mesa: Mesa): Long

    @Query("UPDATE mesas SET posX = :posX, posY = :posY WHERE id = :id")
    suspend fun updatePosicion(id: Long, posX: Float, posY: Float)

    @Query("UPDATE mesas SET girada = :girada WHERE id = :id")
    suspend fun updateGiro(id: Long, girada: Boolean)
}

@Dao
interface ReservaDao {
    @Query("SELECT * FROM reservas WHERE id = :id")
    suspend fun getById(id: Long): Reserva?

    @Insert
    suspend fun insert(reserva: Reserva): Long

    @Query("UPDATE reservas SET canceladaEn = :ts WHERE id = :id")
    suspend fun marcarCancelada(id: Long, ts: Long)

    @Query("UPDATE reservas SET convertidaEn = :ts WHERE id = :id")
    suspend fun marcarConvertida(id: Long, ts: Long)
}

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos WHERE disponible = 1 ORDER BY categoria, nombre")
    fun observeAll(): Flow<List<Producto>>

    @Query("SELECT * FROM productos ORDER BY categoria, nombre")
    fun observeAllIncluyendoOcultos(): Flow<List<Producto>>

    @Query("SELECT * FROM productos ORDER BY categoria, nombre")
    suspend fun getAllIncluyendoOcultos(): List<Producto>

    @Query("SELECT * FROM productos WHERE disponible = 1")
    suspend fun getAllDisponibles(): List<Producto>

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun getById(id: Long): Producto?

    @Query("SELECT COUNT(*) FROM productos")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(productos: List<Producto>)

    @Insert
    suspend fun insert(producto: Producto): Long

    @Update
    suspend fun update(producto: Producto)

    @Update
    suspend fun updateAll(productos: List<Producto>)

    @Query("UPDATE productos SET disponible = :disponible WHERE id = :id")
    suspend fun updateDisponible(id: Long, disponible: Boolean)

    @Query("DELETE FROM productos WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PedidoDao {
    @Query("SELECT * FROM pedidos WHERE mesaId = :mesaId AND estado != 'CERRADA' ORDER BY id DESC LIMIT 1")
    fun observeActivo(mesaId: Long): Flow<Pedido?>

    @Query("SELECT * FROM pedidos WHERE mesaId = :mesaId AND estado != 'CERRADA' ORDER BY id DESC LIMIT 1")
    suspend fun getActivo(mesaId: Long): Pedido?

    @Query("SELECT * FROM pedidos WHERE mesaId = :mesaId AND estado = 'CERRADA' ORDER BY cerradoEn DESC LIMIT 1")
    suspend fun getLastCerrado(mesaId: Long): Pedido?

    @Query("SELECT COUNT(*) FROM pedidos WHERE estado != 'CERRADA'")
    fun observeActivos(): Flow<Int>

    @Query("SELECT COALESCE(SUM(l.precioUnitario * l.cantidad), 0) FROM lineas_pedido l INNER JOIN pedidos p ON l.pedidoId = p.id WHERE p.creadoEn >= :inicioDelDia")
    fun observeTotalHoy(inicioDelDia: Long): Flow<Double>

    @Insert
    suspend fun insert(pedido: Pedido): Long

    @Update
    suspend fun update(pedido: Pedido)
}

@Dao
interface LineaPedidoDao {
    @Query("SELECT * FROM lineas_pedido WHERE pedidoId = :pedidoId ORDER BY id")
    fun observeForPedido(pedidoId: Long): Flow<List<LineaPedido>>

    @Query("SELECT * FROM lineas_pedido WHERE pedidoId = :pedidoId ORDER BY id")
    suspend fun getForPedido(pedidoId: Long): List<LineaPedido>

    @Insert
    suspend fun insert(linea: LineaPedido): Long

    @Update
    suspend fun update(linea: LineaPedido)

    @Query("SELECT * FROM lineas_pedido WHERE ticketId = :ticketId")
    suspend fun getByTicketId(ticketId: String): List<LineaPedido>

    @Query(
        "UPDATE lineas_pedido SET estado = :nuevo WHERE ticketId = :ticketId AND estado = :desde"
    )
    suspend fun updateEstadoSi(ticketId: String, desde: LineaEstado, nuevo: LineaEstado): Int

    @Query("SELECT COUNT(*) FROM lineas_pedido lp INNER JOIN pedidos p ON lp.pedidoId = p.id WHERE lp.productoId = :productoId AND p.estado != 'CERRADA'")
    suspend fun countActiveLinesForProduct(productoId: Long): Int

    @Delete
    suspend fun delete(linea: LineaPedido)
}
