package com.fraudlens.sdk.ui.risk

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Optional Material3 dialog for blocked / high-risk payment flows (matches demo UX intent).
 */
@Composable
fun FraudLensHighRiskPaymentDialog(
    visible: Boolean,
    title: String,
    bodyLines: List<String>,
    countdownSeconds: Int?,
    confirmLabel: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.error) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                bodyLines.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                }
                countdownSeconds?.let { sec ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Time remaining: ${sec}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
        modifier = Modifier.padding(8.dp),
    )
}
