package com.jaminsmoke.personalcomander.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.GrupoModificador
import com.jaminsmoke.personalcomander.data.OpcionModificador
import com.jaminsmoke.personalcomander.data.Producto
import com.jaminsmoke.personalcomander.data.subfamiliaOrNull
import com.jaminsmoke.personalcomander.ui.components.BrandHeaderDensity
import com.jaminsmoke.personalcomander.ui.components.PcBrandHeader
import com.jaminsmoke.personalcomander.ui.components.PcGoldFab

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MenuScreen(
    onBack: (() -> Unit)? = null,
    viewModel: MenuViewModel = viewModel()
) {
    val productos by viewModel.productos.collectAsState(initial = emptyList())
    val grupos by viewModel.grupos.collectAsState()
    val opciones by viewModel.opciones.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val cartaEditable by viewModel.cartaEditable.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var dialogVisible by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Producto?>(null) }
    var confirmarBorrado by remember { mutableStateOf<Producto?>(null) }
    var grupoDialog by remember { mutableStateOf<GrupoModificador?>(null) }
    var grupoNuevo by remember { mutableStateOf(false) }
    var confirmarBorradoGrupo by remember { mutableStateOf<GrupoModificador?>(null) }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (cartaEditable) {
                PcGoldFab(
                    onClick = {
                        editando = null
                        dialogVisible = true
                    },
                    contentDescription = stringResource(R.string.menu_new_product),
                )
            }
        },
        topBar = {
            PcBrandHeader(
                title = stringResource(R.string.menu_title),
                density = BrandHeaderDensity.Compact,
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                        }
                    }
                } else null,
            )
        }
    ) { padding ->
        if (!cargando && productos.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Restaurant,
                title = stringResource(R.string.empty_menu_title),
                subtitle = stringResource(R.string.empty_menu_subtitle),
                modifier = Modifier.padding(padding),
                actionLabel = if (cartaEditable) stringResource(R.string.empty_menu_action) else null,
                onAction = if (cartaEditable) ({ editando = null; dialogVisible = true }) else null,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!cartaEditable) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.sesion_banner_solo_lectura),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
                item {
                    GruposCartaCard(
                        grupos = grupos,
                        opciones = opciones,
                        editable = cartaEditable,
                        onNuevo = { grupoNuevo = true; grupoDialog = null },
                        onEditar = { grupoDialog = it; grupoNuevo = false },
                        onEliminar = { confirmarBorradoGrupo = it },
                    )
                }
                items(productos, key = { it.id }) { producto ->
                    MenuProductoRow(
                        producto = producto,
                        editable = cartaEditable,
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
    }

    if (dialogVisible) {
        ProductoDialog(
            producto = editando,
            categoriasExistentes = productos.map { it.categoria }.distinct(),
            subfamiliasExistentes = productos.mapNotNull { it.subfamiliaOrNull() }.distinct(),
            grupos = grupos,
            grupoIdsIniciales = editando?.let { viewModel.gruposDe(it.id) }.orEmpty(),
            onDismiss = { dialogVisible = false },
            onGuardar = { nombre, categoria, precio, subfamilia, permiteNota, grupoIds ->
                val productoEditado = editando
                if (productoEditado == null) {
                    viewModel.addProducto(nombre, categoria, precio, subfamilia, permiteNota, grupoIds)
                } else {
                    viewModel.updateProducto(productoEditado, nombre, categoria, precio, subfamilia, permiteNota, grupoIds)
                }
                dialogVisible = false
            }
        )
    }

    if (grupoNuevo || grupoDialog != null) {
        val g = grupoDialog
        GrupoDialog(
            grupo = g,
            opciones = g?.let { viewModel.opcionesDe(it.id) }.orEmpty().map {
                OpcionBorrador(it.id, it.nombre, it.deltaPrecio, it.alias)
            },
            onDismiss = { grupoNuevo = false; grupoDialog = null },
            onGuardar = { nombre, multiple, obligatorio, ops ->
                viewModel.guardarGrupo(g, nombre, multiple, obligatorio, ops)
                grupoNuevo = false
                grupoDialog = null
            },
        )
    }

    confirmarBorrado?.let { producto ->
        AlertDialog(
            onDismissRequest = { confirmarBorrado = null },
            title = { Text(stringResource(R.string.menu_delete_product)) },
            text = { Text(stringResource(R.string.menu_delete_confirm, producto.nombre)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProducto(producto)
                    confirmarBorrado = null
                }) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = null }) {
                    Text(stringResource(R.string.menu_cancel))
                }
            }
        )
    }

    confirmarBorradoGrupo?.let { grupo ->
        AlertDialog(
            onDismissRequest = { confirmarBorradoGrupo = null },
            title = { Text(stringResource(R.string.menu_delete_group)) },
            text = { Text(stringResource(R.string.menu_delete_group_confirm, grupo.nombre)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGrupo(grupo)
                    confirmarBorradoGrupo = null
                }) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorradoGrupo = null }) {
                    Text(stringResource(R.string.menu_cancel))
                }
            }
        )
    }
}

@Composable
private fun MenuProductoRow(
    producto: Producto,
    editable: Boolean,
    onEditar: () -> Unit,
    onToggleDisponible: () -> Unit,
    onEliminar: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accent = if (producto.disponible) scheme.tertiary else scheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(accent, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (producto.disponible) scheme.onSurface else scheme.onSurfaceVariant,
                )
                val detalle = buildString {
                    append(producto.categoria)
                    producto.subfamiliaOrNull()?.let { append(" · "); append(it) }
                }
                Text(
                    text = if (producto.disponible) detalle else stringResource(R.string.menu_hidden_label, detalle),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Text(
                text = producto.precio.formatoEuro(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (producto.disponible) scheme.secondary else scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            if (editable) {
                Checkbox(
                    checked = producto.disponible,
                    onCheckedChange = { onToggleDisponible() }
                )
                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.btn_edit))
                }
                IconButton(onClick = onEliminar) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.btn_delete),
                        tint = scheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductoDialog(
    producto: Producto?,
    categoriasExistentes: List<String>,
    subfamiliasExistentes: List<String>,
    grupos: List<GrupoModificador>,
    grupoIdsIniciales: List<Long>,
    onDismiss: () -> Unit,
    onGuardar: (nombre: String, categoria: String, precio: Double, subfamilia: String?, permiteNota: Boolean, grupoIds: List<Long>) -> Unit
) {
    var nombre by remember(producto) { mutableStateOf(producto?.nombre ?: "") }
    var categoria by remember(producto) { mutableStateOf(producto?.categoria ?: "") }
    var subfamilia by remember(producto) { mutableStateOf(producto?.subfamilia.orEmpty()) }
    var permiteNota by remember(producto) { mutableStateOf(producto?.permiteNota == true) }
    var grupoIds by remember(producto) { mutableStateOf(grupoIdsIniciales.toSet()) }
    var precio by remember(producto) {
        mutableStateOf(if (producto == null) "" else producto.precio.toString())
    }
    var error by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    @SuppressLint("LocalContextGetResourceValueCall")
    fun guardar() {
        val p = parsePrecio(precio)
        error = when {
            nombre.isBlank() -> context.getString(R.string.menu_validation_name_required)
            categoria.isBlank() -> context.getString(R.string.menu_validation_category_required)
            p == null -> context.getString(R.string.menu_validation_invalid_price)
            p < 0 -> context.getString(R.string.menu_validation_negative_price)
            else -> null
        }
        if (error != null) return
        val precioFinal = p ?: return
        onGuardar(nombre, categoria, precioFinal, subfamilia, permiteNota, grupoIds.toList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (producto == null) stringResource(R.string.menu_new_product) else stringResource(R.string.menu_edit_product)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.menu_field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text(stringResource(R.string.menu_field_category)) },
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
                    value = subfamilia,
                    onValueChange = { subfamilia = it },
                    label = { Text(stringResource(R.string.menu_field_subfamily)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (subfamiliasExistentes.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subfamiliasExistentes, key = { it }) { sub ->
                            FilterChip(
                                selected = subfamilia == sub,
                                onClick = { subfamilia = sub },
                                label = { Text(sub) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text(stringResource(R.string.menu_field_price)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = permiteNota, onCheckedChange = { permiteNota = it })
                    Text(stringResource(R.string.menu_field_note), style = MaterialTheme.typography.bodyMedium)
                }
                if (grupos.isNotEmpty()) {
                    Text(stringResource(R.string.menu_field_groups), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        grupos.forEach { g ->
                            FilterChip(
                                selected = g.id in grupoIds,
                                onClick = {
                                    grupoIds = if (g.id in grupoIds) grupoIds - g.id else grupoIds + g.id
                                },
                                label = { Text(g.nombre) },
                            )
                        }
                    }
                }
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
            TextButton(onClick = { guardar() }) { Text(stringResource(R.string.menu_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.menu_cancel)) }
        }
    )
}

@Composable
private fun GruposCartaCard(
    grupos: List<GrupoModificador>,
    opciones: List<OpcionModificador>,
    editable: Boolean,
    onNuevo: () -> Unit,
    onEditar: (GrupoModificador) -> Unit,
    onEliminar: (GrupoModificador) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.menu_groups_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (editable) {
                    TextButton(onClick = onNuevo) { Text(stringResource(R.string.menu_groups_new)) }
                }
            }
            if (grupos.isEmpty()) {
                Text(
                    stringResource(R.string.menu_groups_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                grupos.forEach { g ->
                    val ops = opciones.filter { it.grupoId == g.id }.joinToString(" · ") { it.nombre }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(g.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            if (ops.isNotEmpty()) {
                                Text(ops, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (editable) {
                            IconButton(onClick = { onEditar(g) }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.btn_edit))
                            }
                            IconButton(onClick = { onEliminar(g) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrupoDialog(
    grupo: GrupoModificador?,
    opciones: List<OpcionBorrador>,
    onDismiss: () -> Unit,
    onGuardar: (nombre: String, multiple: Boolean, obligatorio: Boolean, opciones: List<OpcionBorrador>) -> Unit,
) {
    var nombre by remember(grupo) { mutableStateOf(grupo?.nombre ?: "") }
    var multiple by remember(grupo) { mutableStateOf(grupo?.multiple == true) }
    var obligatorio by remember(grupo) { mutableStateOf(grupo?.obligatorio == true) }
    var filas by remember(grupo) {
        mutableStateOf(opciones.ifEmpty { listOf(OpcionBorrador()) })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (grupo == null) stringResource(R.string.menu_groups_new) else stringResource(R.string.menu_groups_edit)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.menu_field_group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = multiple, onCheckedChange = { multiple = it })
                    Text(stringResource(R.string.menu_field_multiple), style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = obligatorio, onCheckedChange = { obligatorio = it })
                    Text(stringResource(R.string.menu_field_required), style = MaterialTheme.typography.bodyMedium)
                }
                filas.forEachIndexed { idx, fila ->
                    OutlinedTextField(
                        value = fila.nombre,
                        onValueChange = { v ->
                            filas = filas.toMutableList().also { it[idx] = fila.copy(nombre = v) }
                        },
                        label = { Text(stringResource(R.string.menu_field_option_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = if (fila.deltaPrecio == 0.0) "" else fila.deltaPrecio.toString(),
                        onValueChange = { v ->
                            filas = filas.toMutableList().also {
                                it[idx] = fila.copy(deltaPrecio = parsePrecio(v) ?: 0.0)
                            }
                        },
                        label = { Text(stringResource(R.string.menu_field_option_delta)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = fila.alias,
                        onValueChange = { v ->
                            filas = filas.toMutableList().also { it[idx] = fila.copy(alias = v) }
                        },
                        label = { Text(stringResource(R.string.menu_field_option_alias)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextButton(onClick = { filas = filas + OpcionBorrador() }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.menu_groups_add_option), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.menu_groups_add_option))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(nombre, multiple, obligatorio, filas) }) {
                Text(stringResource(R.string.menu_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.menu_cancel)) }
        },
    )
}

private fun parsePrecio(texto: String): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()
