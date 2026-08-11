package com.jaminsmoke.personalcomander.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.data.ServidorDescubierto
import com.jaminsmoke.personalcomander.data.TpvPrograma

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    onBack: () -> Unit,
    viewModel: AjustesViewModel = viewModel()
) {
    val sync by viewModel.sync.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    val importPreview by viewModel.importPreview.collectAsState()
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Import preview dialog
    importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelarImportacion() },
            title = { Text("Vista previa de importación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Se van a importar los siguientes cambios:")
                    Text("🆕 ${preview.nuevos} productos nuevos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("🔄 ${preview.actualizados} productos actualizados", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    Text("⏭️ ${preview.ignorados} productos ignorados (nombres vacíos)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri?.let { viewModel.confirmarImportacion(it) }
                }) { Text("Importar", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarImportacion() }) { Text("Cancelar") }
            }
        )
    }

    val crearArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportar(uri) }
    val abrirArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) { pendingImportUri = uri; viewModel.importar(uri) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Sincronizar con TPV",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Importa tus productos desde el programa de gestión del restaurante a través de la red local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SincronizarCard(
                    state = sync,
                    onPrograma = viewModel::setPrograma,
                    onHost = viewModel::setHost,
                    onPuerto = viewModel::setPuerto,
                    onRuta = viewModel::setRuta,
                    onElegirServidor = viewModel::elegirServidor,
                    onBuscar = viewModel::buscarServidores,
                    onSincronizar = viewModel::sincronizar
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Copia de seguridad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Exporta o importa todos los productos en un archivo JSON.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                BackupCard(
                    onExportar = {
                        crearArchivo.launch("personalcomander_productos.json")
                    },
                    onImportar = {
                        abrirArchivo.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    }
                )
            }
        }
    }
}

@Composable
private fun SincronizarCard(
    state: AjustesSyncState,
    onPrograma: (TpvPrograma) -> Unit,
    onHost: (String) -> Unit,
    onPuerto: (String) -> Unit,
    onRuta: (String) -> Unit,
    onElegirServidor: (ServidorDescubierto) -> Unit,
    onBuscar: () -> Unit,
    onSincronizar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Programa de gestión",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TpvPrograma.entries) { programa ->
                    FilterChip(
                        selected = state.programa == programa,
                        onClick = { onPrograma(programa) },
                        label = { Text(programa.nombre) }
                    )
                }
            }
            OutlinedTextField(
                value = state.host,
                onValueChange = onHost,
                label = { Text("IP del servidor TPV") },
                placeholder = { Text("192.168.1.50") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.puerto,
                    onValueChange = onPuerto,
                    label = { Text("Puerto") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.ruta,
                    onValueChange = onRuta,
                    label = { Text("Ruta") },
                    singleLine = true,
                    modifier = Modifier.weight(1.6f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onBuscar,
                    enabled = !state.escaneando,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.escaneando) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                    Text("Buscar en red")
                }
                Button(
                    onClick = onSincronizar,
                    enabled = !state.sincronizando,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.sincronizando) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                    }
                    Text("Sincronizar")
                }
            }
            if (state.servidores.isNotEmpty()) {
                Text(
                    text = "Servidores encontrados",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.servidores.forEach { servidor ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onElegirServidor(servidor) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${servidor.ip}:${servidor.puerto}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = servidor.etiqueta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onElegirServidor(servidor) }) {
                            Text("Usar")
                        }
                    }
                }
            }
            state.mensaje?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (it.startsWith("Sincronizados") || it.startsWith("Se encontraron")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
private fun BackupCard(onExportar: () -> Unit, onImportar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onExportar, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Text("Exportar")
            }
            OutlinedButton(onClick = onImportar, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Text("Importar")
            }
        }
    }
}
