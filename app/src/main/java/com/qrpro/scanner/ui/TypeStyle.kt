package com.qrpro.scanner.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.qrpro.scanner.data.ScanType

fun ScanType.icon(): ImageVector = when (this) {
    ScanType.URL -> Icons.Outlined.Language
    ScanType.WIFI -> Icons.Outlined.Wifi
    ScanType.CONTACT -> Icons.Outlined.Person
    ScanType.EMAIL -> Icons.Outlined.Email
    ScanType.PHONE -> Icons.Outlined.Call
    ScanType.SMS -> Icons.Outlined.Sms
    ScanType.LOCATION -> Icons.Outlined.Place
    ScanType.PRODUCT -> Icons.Outlined.Inventory2
    ScanType.TEXT -> Icons.Outlined.TextFields
}
