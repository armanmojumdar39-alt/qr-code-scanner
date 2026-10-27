package com.qrpro.scanner.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qrpro.scanner.R
import com.qrpro.scanner.data.Parsed
import com.qrpro.scanner.data.Scan
import com.qrpro.scanner.data.ScanType
import com.qrpro.scanner.util.Actions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class Act(
    @StringRes val label: Int,
    val icon: ImageVector,
    val run: () -> Unit,
)

private fun mainAction(context: Context, p: Parsed): Act {
    val e = p.extras
    return when (p.type) {
        ScanType.URL -> Act(R.string.act_open_link, Icons.AutoMirrored.Outlined.OpenInNew) {
            Actions.openUrl(context, e["url"].orEmpty())
        }

        ScanType.WIFI -> {
            val password = e["password"].orEmpty()
            if (password.isNotBlank()) {
                Act(R.string.act_copy_password, Icons.Outlined.Lock) {
                    Actions.copy(context, password)
                }
            } else {
                Act(R.string.act_copy_network, Icons.Outlined.Wifi) {
                    Actions.copy(context, e["ssid"].orEmpty())
                }
            }
        }

        ScanType.CONTACT -> Act(R.string.act_add_contact, Icons.Outlined.PersonAdd) {
            Actions.addContact(
                context,
                e["name"].orEmpty(),
                e["phone"].orEmpty(),
                e["email"].orEmpty(),
                e["org"].orEmpty(),
            )
        }

        ScanType.EMAIL -> Act(R.string.act_send_email, Icons.Outlined.Email) {
            Actions.email(context, e["email"].orEmpty(), e["subject"].orEmpty(), e["body"].orEmpty())
        }

        ScanType.PHONE -> Act(R.string.act_call, Icons.Outlined.Call) {
            Actions.dial(context, e["number"].orEmpty())
        }

        ScanType.SMS -> Act(R.string.act_send_sms, Icons.Outlined.Sms) {
            Actions.sms(context, e["number"].orEmpty(), e["body"].orEmpty())
        }

        ScanType.LOCATION -> Act(R.string.act_open_map, Icons.Outlined.Map) {
            Actions.map(context, e["coords"].orEmpty())
        }

        ScanType.PRODUCT, ScanType.TEXT -> Act(R.string.act_search, Icons.Outlined.Search) {
            Actions.search(context, e["query"] ?: p.raw)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultSheet(scan: Scan, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val p = scan.parsed
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val main = remember(scan) { mainAction(context, p) }
    val isHttp = p.type == ScanType.URL &&
        (p.extras["url"] ?: "").startsWith("http://", ignoreCase = true)
    val date = remember(scan.time) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(scan.time))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = p.type.icon(),
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(p.type.label),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${scan.format} · $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            if (isHttp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.errorContainer.copy(alpha = 0.55f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = scheme.onErrorContainer,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.warn_http),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onErrorContainer,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Details card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = scheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    p.fields.forEachIndexed { index, field ->
                        if (index > 0) {
                            HorizontalDivider(color = scheme.outlineVariant)
                        }
                        Column(Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = stringResource(field.label),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            SelectionContainer {
                                Text(
                                    text = field.value,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Primary action
            Button(
                onClick = main.run,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(main.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(main.label),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(10.dp))

            // Secondary actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { Actions.copy(context, p.raw) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.act_copy))
                }
                OutlinedButton(
                    onClick = { Actions.share(context, p.raw) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.act_share))
                }
            }
        }
    }
}
