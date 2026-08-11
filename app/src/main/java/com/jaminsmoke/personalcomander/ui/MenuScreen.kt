package com.jaminsmoke.personalcomander.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.data.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onBack: () -> Unit,
    viewModel: MenuViewModel = viewModel(
        factory = MenuViewModel.factory(LocalContext.current.applicationContext)
    )
) {
    val productos by viewModel.productos.collectAsState(initial = emptyList())
    var dialogVisible by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Producto?>(null) }
    var confirmarBorrado by remember { mutableStateOf<Producto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión del menú") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editando = null
                        dialogVisible = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo producto")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(productos, key = { it.id }) { producto ->
                MenuProductoRow(
                    producto = producto,
                    onEditar = {
                        editando = producto
                        dialogVisible = true
                    },
                    onToggleDisponible = { viewModel.toggleDisponible(producto) },
                    onEliminar = { confirmarBorrado = producto }
                )
            }
        }
    }

    if (dialogVisible) {
        ProductoDialog(
            producto = editando,
            categoriasExistentes = productos.map { it.categoria }.distinct(),
            onDismiss = { dialogVisible = false },
            onGuardar = { nombre, categoria, precio ->
                if (editando == null) {
                    viewModel.addProducto(nombre, categoria, precio)
                } else {
                    viewModel.updateProducto(editando!!, nombre, categoria, precio)
                }
                dialogVisible = false
            }
        )
    }

    confirmarBorrado?.let { producto ->
        AlertDialog(
            onDismissRequest = { confirmarBorrado = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Eliminar \"${producto.nombre}\" definitivamente?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProducto(producto)
                    confirmarBorrado = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun MenuProductoRow(
    producto: Producto,
    onEditar: () -> Unit,
    onToggleDisponible: () -> Unit,
    onEliminar: () -> Unit
) {
    val textoColor = if (producto.disponible) Color.Unspecified
    else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (producto.disponible) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textoColor
                )
                Text(
                    text = if (producto.disponible) producto.categoria else "Oculto · ${producto.categoria}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = producto.precio.formatoEuro(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = textoColor,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Checkbox(
                checked = producto.disponible,
                onCheckedChange = { onToggleDisponible() }
            )
            IconButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ProductoDialog(
    producto: Producto?,
    categoriasExistentes: List<String>,
    onDismiss: () -> Unit,
    onGuardar: (nombre: String, categoria: String, precio: Double) -> Unit
) {
    var nombre by remember(producto) { mutableStateOf(producto?.nombre ?: "") }
    var categoria by remember(producto) { mutableStateOf(producto?.categoria ?: "") }
    var precio by remember(producto) {
        mutableStateOf(if (producto == null) "" else producto.precio.toString())
    }
    var error by remember { mutableStateOf<String?>(null) }

    fun guardar() {
        val p = parsePrecio(precio)
        error = when {
            nombre.isBlank() -> "El nombre es obligatorio"
            categoria.isBlank() -> "La categoría es obligatoria"
            p == null -> "Precio no válido"
            p < 0 -> "El precio no puede ser negativo"
            else -> null
        }
        if (error != null) return
        onGuardar(nombre, categoria, p!!)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (producto == null) "Nuevo producto" else "Editar producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoría") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (categoriasExistentes.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categoriasExistentes, key = { it }) { cat ->
                            FilterChip(
                                selected = categoria == cat,
                                onClick = { categoria = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { guardar() }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun parsePrecio(texto: String): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()
