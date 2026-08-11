package com.jaminsmoke.personalcomander.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MesaDao {
    @Query("SELECT * FROM mesas ORDER BY zona, numero")
    fun observeAll(): Flow<List<Mesa>>

    @Query("SELECT * FROM mesas WHERE id = :id")
    fun observeById(id: Long): Flow<Mesa?>

    @Query("SELECT COUNT(*) FROM mesas")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM mesas")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM mesas WHERE estado != 'LIBRE'")
    fun observeOcupadas(): Flow<Int>

    @Insert
    suspend fun insertAll(mesas: List<Mesa>)

    @Update
    suspend fun update(mesa: Mesa)

    @Query("UPDATE mesas SET estado = :estado, comandaActivaId = :comandaId, alias = :alias WHERE id = :id")
    suspend fun updateEstado(id: Long, estado: MesaEstado, comandaId: Long?, alias: String? = null)

    @Query("UPDATE mesas SET alias = :alias, capacidad = :capacidad WHERE id = :id")
    suspend fun updateConfig(id: Long, alias: String?, capacidad: Int)
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

    @Query("SELECT COUNT(*) FROM pedidos WHERE estado = 'ABIERTA'")
    fun observeAbiertos(): Flow<Int>

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

    @Delete
    suspend fun delete(linea: LineaPedido)
}
