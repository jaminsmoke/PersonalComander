package com.jaminsmoke.personalcomander.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
) {
    val scheme = MaterialTheme.colorScheme
    val hero = density == BrandHeaderDensity.Hero
    val shieldSize = if (hero) 56.dp else 40.dp
    val barHeight = if (hero) 88.dp else 64.dp
    val titleStyle = if (hero) {
        MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surfaceContainerLowest,
        shadowElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .fillMaxWidth()
                    .height(barHeight)
                    .padding(
                        start = if (navigationIcon == null) 16.dp else 4.dp,
                        end = if (actions == null) 16.dp else 4.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                Text(
                    text = title,
                    color = scheme.secondary,
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
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
