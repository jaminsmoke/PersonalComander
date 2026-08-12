package com.jaminsmoke.personalcomander.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jaminsmoke.personalcomander.R

enum class VoiceFeedbackType {
    SUCCESS,    // Todo reconocido
    PARTIAL,    // Reconocido parcialmente
    ERROR       // Error o no entendido
}

@Composable
fun VoiceFeedbackCard(
    message: String,
    type: VoiceFeedbackType,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        val (containerColor, contentColor, icon) = when (type) {
            VoiceFeedbackType.SUCCESS -> Triple(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                Icons.Default.CheckCircle
            )
            VoiceFeedbackType.PARTIAL -> Triple(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
                Icons.Default.Warning
            )
            VoiceFeedbackType.ERROR -> Triple(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
                Icons.Default.Error
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        stringResource(R.string.voice_feedback_dismiss),
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

/**
 * Parsea el mensaje de feedback para determinar el tipo.
 * Mensajes que contienen "No reconocí" o "No entendí" → ERROR
 * Mensajes que contienen "No reconocido" → PARTIAL
 * El resto → SUCCESS
 */
fun parseFeedbackType(message: String): VoiceFeedbackType = when {
    message.contains("No reconocí", ignoreCase = true) ||
    message.contains("No entendí", ignoreCase = true) ||
    message.contains("Error", ignoreCase = true) -> VoiceFeedbackType.ERROR
    message.contains("No reconocido", ignoreCase = true) -> VoiceFeedbackType.PARTIAL
    else -> VoiceFeedbackType.SUCCESS
}
