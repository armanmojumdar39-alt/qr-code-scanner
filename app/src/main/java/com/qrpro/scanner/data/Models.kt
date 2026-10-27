package com.qrpro.scanner.data

import androidx.annotation.StringRes
import com.qrpro.scanner.R

enum class ScanType(@StringRes val label: Int) {
    URL(R.string.type_url),
    WIFI(R.string.type_wifi),
    CONTACT(R.string.type_contact),
    EMAIL(R.string.type_email),
    PHONE(R.string.type_phone),
    SMS(R.string.type_sms),
    LOCATION(R.string.type_location),
    PRODUCT(R.string.type_product),
    TEXT(R.string.type_text),
}

data class Field(@StringRes val label: Int, val value: String)

/** A decoded code, split into display fields and values the action buttons need. */
data class Parsed(
    val type: ScanType,
    val raw: String,
    val fields: List<Field>,
    val extras: Map<String, String> = emptyMap(),
)

data class Scan(val parsed: Parsed, val format: String, val time: Long)

data class HistoryItem(val raw: String, val format: String, val time: Long)
