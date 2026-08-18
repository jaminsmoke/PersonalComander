package com.jaminsmoke.personalcomander.ui.sesion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaminsmoke.personalcomander.R
import com.jaminsmoke.personalcomander.data.sesion.ModoSesion
import com.jaminsmoke.personalcomander.ui.components.BrandHeaderDensity
import com.jaminsmoke.personalcomander.ui.components.PcBrandHeader
import com.jaminsmoke.personalcomander.ui.components.PcPrimaryButton
import com.jaminsmoke.personalcomander.ui.components.PcSecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onAutenticado: () -> Unit,
    viewModel: SesionViewModel = viewModel(),
) {
    val modo by viewModel.modo.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val teclado = LocalSoftwareKeyboardController.current
    var registro by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var nick by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var errorAuth by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(modo) {
        if (modo !is ModoSesion.Local) onAutenticado()
    }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            errorAuth = it
            viewModel.limpiarMensaje()
        }
    }

    fun enviarLogin() {
        teclado?.hide()
        errorAuth = null
        viewModel.login(email, password)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PcBrandHeader(
                title = stringResource(if (registro) R.string.sesion_registro_title else R.string.sesion_login_title),
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.sesion_auth_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !registro,
                    onClick = { registro = false },
                    label = { Text(stringResource(R.string.sesion_tab_login)) },
                )
                FilterChip(
                    selected = registro,
                    onClick = { registro = true },
                    label = { Text(stringResource(R.string.sesion_tab_registro)) },
                )
            }
            if (registro) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.sesion_nombre)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it },
                    label = { Text(stringResource(R.string.sesion_apellidos)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = nick,
                    onValueChange = { nick = it },
                    label = { Text(stringResource(R.string.sesion_nick)) },
                    supportingText = { Text(stringResource(R.string.sesion_nick_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorAuth = null
                },
                label = { Text(stringResource(R.string.sesion_email)) },
                singleLine = true,
                isError = errorAuth != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (registro) {
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text(stringResource(R.string.sesion_telefono)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorAuth = null
                },
                label = { Text(stringResource(R.string.sesion_password)) },
                singleLine = true,
                isError = errorAuth != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (registro) ImeAction.Done else ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = { if (!busy) enviarLogin() },
                    onDone = {
                        if (!busy && registro) {
                            teclado?.hide()
                            errorAuth = null
                            viewModel.registrar(
                                nombre,
                                apellidos,
                                email,
                                password,
                                telefono.ifBlank { null },
                                nick.trim(),
                            )
                        }
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            errorAuth?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (registro) {
                PcPrimaryButton(
                    text = stringResource(R.string.sesion_crear_cuenta),
                    onClick = {
                        teclado?.hide()
                        errorAuth = null
                        viewModel.registrar(nombre, apellidos, email, password, telefono.ifBlank { null }, nick.trim())
                    },
                    enabled = nick.trim().isNotEmpty() &&
                        nombre.isNotBlank() &&
                        apellidos.isNotBlank() &&
                        email.isNotBlank() &&
                        password.length >= 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PcPrimaryButton(
                    text = stringResource(R.string.sesion_entrar),
                    onClick = { enviarLogin() },
                    enabled = email.trim().isNotEmpty() && password.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
                PcSecondaryButton(
                    text = stringResource(R.string.sesion_ir_registro),
                    onClick = { registro = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
