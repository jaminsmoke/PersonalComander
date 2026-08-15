package com.jaminsmoke.personalcomander.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jaminsmoke.personalcomander.R

enum class BrandHeaderDensity {
    Hero,
    Compact,
}

@Composable
fun PcBrandHeader(
    title: String,
    density: BrandHeaderDensity,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val hero = density == BrandHeaderDensity.Hero
    val shieldSize = if (hero) 56.dp else 36.dp
    val titleStyle = if (hero) {
        MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    }
    // El Scaffold de la app ya consume statusBars en NavHost: no volver a
    // aplicar el inset aquí (duplicaba un hueco vacío encima del logo).
    val rowHeight = when {
        hero -> Modifier.height(88.dp)
        supportingContent != null -> Modifier.padding(vertical = 4.dp)
        else -> Modifier.height(52.dp)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surfaceContainerLowest,
        shadowElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(rowHeight)
                    .padding(
                        start = if (navigationIcon == null) 16.dp else 4.dp,
                        end = if (actions == null) 16.dp else 4.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (navigationIcon != null) {
                    navigationIcon()
                }
                Image(
                    painter = painterResource(R.drawable.ic_brand_shield),
                    contentDescription = stringResource(R.string.app_logo_desc),
                    modifier = Modifier.size(shieldSize),
                    contentScale = ContentScale.Fit,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = scheme.secondary,
                        style = titleStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (supportingContent != null) {
                        supportingContent()
                    }
                }
                if (actions != null) {
                    Row(content = actions)
                }
            }
            if (hero) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(scheme.secondary.copy(alpha = 0.28f)),
                )
            }
        }
    }
}
