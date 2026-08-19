package com.jaminsmoke.personalcomander.ui.sesion

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.sesion.CampoVisibilidad
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.data.sesion.VisibleOtrosEstablecimientos
import com.jaminsmoke.personalcomander.data.sesion.credencialRevocada
import com.jaminsmoke.personalcomander.data.sesion.etiquetaLocal
import com.jaminsmoke.personalcomander.data.sesion.fichaUrl
import com.jaminsmoke.personalcomander.data.sesion.perfil
import com.jaminsmoke.personalcomander.data.sesion.qrVisible
import com.jaminsmoke.personalcomander.ui.components.AvatarCamarero
import com.jaminsmoke.personalcomander.ui.components.BrandHeaderDensity
import com.jaminsmoke.personalcomander.ui.components.GlassCard
import com.jaminsmoke.personalcomander.ui.components.PcBrandHeader
import com.jaminsmoke.personalcomander.ui.components.PcPrimaryButton
import com.jaminsmoke.personalcomander.ui.components.PcSecondaryButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PerfilScreen(
    onBack: () -> Unit,
    viewModel: SesionViewModel = viewModel(),
) {
    val modo by viewModel.modo.collectAsState()
    val foto by viewModel.foto.collectAsState()
    val membresias by viewModel.membresias.collectAsState()
    val visibilidad by viewModel.visibilidad.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val perfil = modo.perfil
    val qrVisible = modo.qrVisible
    val fichaHttp = modo.fichaUrl?.trim()?.takeIf {
        it.startsWith("https://") || it.startsWith("http://")
    }
    val qrBmp = remember(qrVisible) { qrVisible?.let { qrImageBitmap(it) } }
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }
    var confirmarRenovar by remember { mutableStateOf(false) }
    var confirmarRevocar by remember { mutableStateOf(false) }
    var confirmarBorrar by remember { mutableStateOf(false) }
    var confirmarPassword by remember { mutableStateOf(false) }
    var passwordBorrar by remember { mutableStateOf("") }
    var passwordActual by remember { mutableStateOf("") }
    var passwordNueva by remember { mutableStateOf("") }
    var passwordNueva2 by remember { mutableStateOf("") }
    var nickEdit by remember(perfil?.id, perfil?.nick) { mutableStateOf(perfil?.mote.orEmpty()) }
    var direccionEdit by remember(perfil?.id, perfil?.direccion) {
        mutableStateOf(perfil?.direccion.orEmpty())
    }
    var ciudadEdit by remember(perfil?.id, perfil?.ciudad) { mutableStateOf(perfil?.ciudad.orEmpty()) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { viewModel.subirFoto(it) } }

    LaunchedEffect(Unit) { viewModel.refrescarPerfil() }
    LaunchedEffect(modo) {
        if (modo is ModoSesion.Local) onBack()
    }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbar.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            PcBrandHeader(
                title = stringResource(R.string.sesion_perfil_title),
                density = BrandHeaderDensity.Compact,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (perfil == null) {
                Text(stringResource(R.string.sesion_sin_cuenta), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                AvatarCamarero(
                    iniciales = perfil.iniciales,
                    fotoBytes = foto,
                    size = 96.dp,
                    contentDescription = stringResource(R.string.sesion_avatar_desc),
                )
                Text(perfil.nombreCompleto, style = MaterialTheme.typography.headlineSmall)
                Text(perfil.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                perfil.telefono?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = nickEdit,
                    onValueChange = { nickEdit = it },
                    label = { Text(stringResource(R.string.sesion_nick)) },
                    supportingText = { Text(stringResource(R.string.sesion_nick_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = direccionEdit,
                    onValueChange = { direccionEdit = it },
                    label = { Text(stringResource(R.string.sesion_direccion)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = ciudadEdit,
                    onValueChange = { ciudadEdit = it },
                    label = { Text(stringResource(R.string.sesion_ciudad)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PcSecondaryButton(
                    text = stringResource(R.string.sesion_ficha_guardar),
                    onClick = { viewModel.actualizarFicha(nickEdit, direccionEdit, ciudadEdit) },
                    enabled = !busy && nickEdit.trim().isNotEmpty() && (
                        nickEdit.trim() != perfil.nick.orEmpty() ||
                            direccionEdit.trim() != perfil.direccion.orEmpty() ||
                            ciudadEdit.trim() != perfil.ciudad.orEmpty()
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
                when (val actual = modo) {
                    is ModoSesion.Establecimiento -> Text(
                        text = stringResource(
                            when {
                                actual.sesionTrabajo -> R.string.sesion_modo_sala_admitido
                                actual.admitido -> R.string.sesion_modo_sala_sin_jornada
                                else -> R.string.sesion_modo_sala_pendiente
                            },
                            actual.etiquetaLocal().ifBlank {
                                stringResource(R.string.home_lan_local_sin_nombre)
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    is ModoSesion.Identidad -> Text(
                        text = stringResource(R.string.sesion_modo_identidad),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    else -> Unit
                }
                MembresiasIdentityBlock(membresias = membresias)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PcSecondaryButton(
                        text = stringResource(R.string.sesion_foto_elegir),
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    )
                    PcSecondaryButton(
                        text = stringResource(R.string.sesion_foto_quitar),
                        onClick = { viewModel.borrarFoto() },
                        enabled = !busy && foto != null,
                        modifier = Modifier.weight(1f),
                    )
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(stringResource(R.string.sesion_visibilidad_titulo), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.sesion_visibilidad_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilaVisibilidad(
                            titulo = stringResource(R.string.sesion_visibilidad_nombre),
                            hint = stringResource(R.string.sesion_visibilidad_nombre_hint),
                            checked = visibilidad.nombre,
                            enabled = !busy,
                            onCheckedChange = { viewModel.setVisibilidad(CampoVisibilidad.NOMBRE, it) },
                        )
                        FilaVisibilidad(
                            titulo = stringResource(R.string.sesion_visibilidad_apellidos),
                            checked = visibilidad.apellidos,
                            enabled = !busy,
                            onCheckedChange = { viewModel.setVisibilidad(CampoVisibilidad.APELLIDOS, it) },
                        )
                        FilaVisibilidad(
                            titulo = stringResource(R.string.sesion_visibilidad_nick),
                            checked = visibilidad.nick,
                            enabled = !busy,
                            onCheckedChange = { viewModel.setVisibilidad(CampoVisibilidad.NICK, it) },
                        )
                        FilaVisibilidad(
                            titulo = stringResource(R.string.sesion_visibilidad_email),
                            checked = visibilidad.email,
                            enabled = !busy,
                            onCheckedChange = { viewModel.setVisibilidad(CampoVisibilidad.EMAIL, it) },
                        )
                        FilaVisibilidad(
                            titulo = stringResource(R.string.sesion_visibilidad_telefono),
                            checked = visibilidad.telefono,
                            enabled = !busy,
                            onCheckedChange = { viewModel.setVisibilidad(CampoVisibilidad.TELEFONO, it) },
                        )
                        FilaVisibilidad(
                            titulo = stringResource(R.string.sesion_visibilidad_direccion),
                            checked = visibilidad.direccion,
                            enabled = !busy,
                            onCheckedChange = { viewModel.setVisibilidad(CampoVisibilidad.DIRECCION, it) },
                        )
                        FilaVisibilidad(
                            titulo = stringResource(R.string.sesion_visibilidad_ciudad),
                            checked = visibilidad.ciudad,
                            enabled = !busy,
                            onCheckedChange = { viewModel.setVisibilidad(CampoVisibilidad.CIUDAD, it) },
                        )
                        FilaVisibilidad(
                            titulo = stringResource(R.string.sesion_visibilidad_foto),
                            hint = stringResource(R.string.sesion_visibilidad_foto_hint),
                            checked = visibilidad.foto,
                            enabled = !busy,
                            onCheckedChange = { viewModel.setVisibilidad(CampoVisibilidad.FOTO, it) },
                        )
                    }
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.sesion_directorio_titulo), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.sesion_directorio_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val directorio = perfil.visibleDirectorio
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = directorio == VisibleOtrosEstablecimientos.Nunca,
                                onClick = {
                                    viewModel.setVisibleOtrosEstablecimientos(VisibleOtrosEstablecimientos.Nunca)
                                },
                                enabled = !busy,
                                label = { Text(stringResource(R.string.sesion_directorio_nunca)) },
                            )
                            FilterChip(
                                selected = directorio == VisibleOtrosEstablecimientos.SoloLibre,
                                onClick = {
                                    viewModel.setVisibleOtrosEstablecimientos(VisibleOtrosEstablecimientos.SoloLibre)
                                },
                                enabled = !busy,
                                label = { Text(stringResource(R.string.sesion_directorio_solo_libre)) },
                            )
                            FilterChip(
                                selected = directorio == VisibleOtrosEstablecimientos.Siempre,
                                onClick = {
                                    viewModel.setVisibleOtrosEstablecimientos(VisibleOtrosEstablecimientos.Siempre)
                                },
                                enabled = !busy,
                                label = { Text(stringResource(R.string.sesion_directorio_siempre)) },
                            )
                        }
                        if (directorio == VisibleOtrosEstablecimientos.SoloLibre && membresias.isNotEmpty()) {
                            Text(
                                stringResource(R.string.sesion_directorio_hint_ocupado),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.sesion_qr_titulo), style = MaterialTheme.typography.titleMedium)
                        if (modo.credencialRevocada) {
                            Text(
                                stringResource(R.string.sesion_qr_revocada),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            PcPrimaryButton(
                                text = stringResource(R.string.sesion_renovar),
                                onClick = { confirmarRenovar = true },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(
                                stringResource(
                                    if (fichaHttp != null) R.string.sesion_qr_desc
                                    else R.string.sesion_qr_desc_sin_url,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (qrBmp != null) {
                                Image(
                                    bitmap = qrBmp,
                                    contentDescription = stringResource(R.string.sesion_qr_desc_img),
                                    modifier = Modifier.size(240.dp),
                                )
                            }
                            if (fichaHttp != null) {
                                PcPrimaryButton(
                                    text = stringResource(R.string.sesion_qr_abrir),
                                    onClick = { uriHandler.openUri(fichaHttp) },
                                    enabled = !busy,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            PcSecondaryButton(
                                text = stringResource(R.string.sesion_renovar),
                                onClick = { confirmarRenovar = true },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            PcSecondaryButton(
                                text = stringResource(R.string.sesion_revocar),
                                onClick = { confirmarRevocar = true },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (busy) {
                    CircularProgressIndicator()
                }
                if (modo is ModoSesion.Establecimiento) {
                    PcSecondaryButton(
                        text = stringResource(R.string.sesion_desconectar_bar),
                        onClick = { viewModel.desconectarBar() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                PcSecondaryButton(
                    text = stringResource(R.string.sesion_password_cambiar),
                    onClick = { confirmarPassword = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                PcSecondaryButton(
                    text = stringResource(R.string.sesion_cerrar),
                    onClick = { viewModel.cerrarSesion() },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = { confirmarBorrar = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.sesion_borrar_cuenta),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (confirmarRenovar) {
        AlertDialog(
            onDismissRequest = { confirmarRenovar = false },
            title = { Text(stringResource(R.string.sesion_renovar_titulo)) },
            text = { Text(stringResource(R.string.sesion_renovar_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRenovar = false
                    viewModel.renovarQr()
                }) { Text(stringResource(R.string.sesion_renovar)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRenovar = false }) {
                    Text(stringResource(R.string.menu_cancel))
                }
            },
        )
    }
    if (confirmarRevocar) {
        AlertDialog(
            onDismissRequest = { confirmarRevocar = false },
            title = { Text(stringResource(R.string.sesion_revocar_titulo)) },
            text = { Text(stringResource(R.string.sesion_revocar_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRevocar = false
                    viewModel.revocarQr()
                }) {
                    Text(stringResource(R.string.sesion_revocar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRevocar = false }) {
                    Text(stringResource(R.string.menu_cancel))
                }
            },
        )
    }
    if (confirmarPassword) {
        val passwordLista = passwordActual.length >= 8 &&
            passwordNueva.length >= 8 &&
            passwordNueva == passwordNueva2
        AlertDialog(
            onDismissRequest = {
                confirmarPassword = false
                passwordActual = ""
                passwordNueva = ""
                passwordNueva2 = ""
            },
            title = { Text(stringResource(R.string.sesion_password_cambiar_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.sesion_password_cambiar_desc))
                    OutlinedTextField(
                        value = passwordActual,
                        onValueChange = { passwordActual = it },
                        label = { Text(stringResource(R.string.sesion_password_actual)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = passwordNueva,
                        onValueChange = { passwordNueva = it },
                        label = { Text(stringResource(R.string.sesion_password_nueva)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = passwordNueva2,
                        onValueChange = { passwordNueva2 = it },
                        label = { Text(stringResource(R.string.sesion_password_nueva_repetir)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val actual = passwordActual
                        val nueva = passwordNueva
                        confirmarPassword = false
                        passwordActual = ""
                        passwordNueva = ""
                        passwordNueva2 = ""
                        viewModel.cambiarPassword(actual, nueva)
                    },
                    enabled = passwordLista,
                ) { Text(stringResource(R.string.sesion_password_cambiar)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmarPassword = false
                    passwordActual = ""
                    passwordNueva = ""
                    passwordNueva2 = ""
                }) { Text(stringResource(R.string.menu_cancel)) }
            },
        )
    }
    if (confirmarBorrar) {
        AlertDialog(
            onDismissRequest = {
                confirmarBorrar = false
                passwordBorrar = ""
            },
            title = { Text(stringResource(R.string.sesion_borrar_cuenta_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.sesion_borrar_cuenta_confirm))
                    OutlinedTextField(
                        value = passwordBorrar,
                        onValueChange = { passwordBorrar = it },
                        label = { Text(stringResource(R.string.sesion_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pass = passwordBorrar
                        confirmarBorrar = false
                        passwordBorrar = ""
                        viewModel.borrarCuenta(pass)
                    },
                    enabled = passwordBorrar.length >= 8,
                ) {
                    Text(stringResource(R.string.sesion_borrar_cuenta), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmarBorrar = false
                    passwordBorrar = ""
                }) { Text(stringResource(R.string.menu_cancel)) }
            },
        )
    }
}

@Composable
private fun FilaVisibilidad(
    titulo: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    hint: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium)
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.semantics { this.contentDescription = titulo },
        )
    }
}
