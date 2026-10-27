package com.qrpro.scanner.data

import android.net.Uri
import android.util.Patterns
import com.qrpro.scanner.R

object ScanParser {

    private val digitsOnly = Regex("^[0-9]{8,14}$")

    fun parse(raw: String): Parsed {
        val t = raw.trim()
        val lower = t.lowercase()
        return when {
            lower.startsWith("http://") || lower.startsWith("https://") -> url(raw, t)
            lower.startsWith("www.") && !t.contains(' ') -> url(raw, "https://$t")
            lower.startsWith("wifi:") -> wifi(raw, t)
            lower.startsWith("begin:vcard") -> vcard(raw, t)
            lower.startsWith("mailto:") -> mailto(raw, t)
            lower.startsWith("tel:") -> phone(raw, t.substring(4).trim())
            lower.startsWith("smsto:") || lower.startsWith("sms:") -> sms(raw, t)
            lower.startsWith("geo:") -> geo(raw, t)
            Patterns.EMAIL_ADDRESS.matcher(t).matches() -> email(raw, t, "", "")
            digitsOnly.matches(t) -> Parsed(
                ScanType.PRODUCT, raw,
                listOf(Field(R.string.f_code, t)),
                mapOf("query" to t),
            )
            else -> Parsed(
                ScanType.TEXT, raw,
                listOf(Field(R.string.f_text, raw)),
                mapOf("query" to raw),
            )
        }
    }

    private fun url(raw: String, url: String) = Parsed(
        ScanType.URL, raw,
        listOf(Field(R.string.f_url, url)),
        mapOf("url" to url),
    )

    private fun phone(raw: String, number: String) = Parsed(
        ScanType.PHONE, raw,
        listOf(Field(R.string.f_phone, number)),
        mapOf("number" to number),
    )

    private fun email(raw: String, address: String, subject: String, body: String): Parsed {
        val fields = mutableListOf(Field(R.string.f_email, address))
        if (subject.isNotBlank()) fields.add(Field(R.string.f_subject, subject))
        if (body.isNotBlank()) fields.add(Field(R.string.f_message, body))
        return Parsed(
            ScanType.EMAIL, raw, fields,
            mapOf("email" to address, "subject" to subject, "body" to body),
        )
    }

    private fun mailto(raw: String, t: String): Parsed {
        val rest = t.substring(7)
        val address = Uri.decode(rest.substringBefore('?'))
        val q = query(rest.substringAfter('?', ""))
        return email(raw, address, q["subject"].orEmpty(), q["body"].orEmpty())
    }

    private fun sms(raw: String, t: String): Parsed {
        val number: String
        val body: String
        if (t.lowercase().startsWith("smsto:")) {
            val rest = t.substring(6)
            number = rest.substringBefore(':')
            body = rest.substringAfter(':', "")
        } else {
            val rest = t.substring(4)
            number = rest.substringBefore('?')
            body = query(rest.substringAfter('?', ""))["body"].orEmpty()
        }
        val fields = mutableListOf(Field(R.string.f_phone, number))
        if (body.isNotBlank()) fields.add(Field(R.string.f_message, body))
        return Parsed(ScanType.SMS, raw, fields, mapOf("number" to number, "body" to body))
    }

    private fun geo(raw: String, t: String): Parsed {
        val coords = t.substring(4).substringBefore('?').substringBefore(';')
        return Parsed(
            ScanType.LOCATION, raw,
            listOf(Field(R.string.f_coordinates, coords)),
            mapOf("coords" to coords),
        )
    }

    /** WIFI:T:WPA;S:MyNetwork;P:secret;H:false;; */
    private fun wifi(raw: String, t: String): Parsed {
        val body = t.substring(5)
        val map = splitUnescaped(body, ';')
            .mapNotNull { part ->
                val i = part.indexOf(':')
                if (i <= 0) null else part.substring(0, i).uppercase() to part.substring(i + 1)
            }
            .toMap()
        val ssid = map["S"].orEmpty()
        val password = map["P"].orEmpty()
        val security = map["T"].orEmpty()

        val fields = mutableListOf(Field(R.string.f_network, ssid))
        if (password.isNotBlank()) fields.add(Field(R.string.f_password, password))
        if (security.isNotBlank() && !security.equals("nopass", ignoreCase = true)) {
            fields.add(Field(R.string.f_security, security.uppercase()))
        }
        return Parsed(
            ScanType.WIFI, raw, fields,
            mapOf("ssid" to ssid, "password" to password),
        )
    }

    private fun vcard(raw: String, t: String): Parsed {
        val lines = t.lines().map { it.trim() }
        fun value(key: String): String = lines
            .firstOrNull { it.substringBefore(':').substringBefore(';').uppercase() == key }
            ?.substringAfter(':', "")
            ?.trim()
            .orEmpty()

        val parts = value("N").split(';')
        val nameFromN = listOf(parts.getOrNull(1), parts.getOrNull(0))
            .filterNotNull()
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val name = value("FN").ifBlank { nameFromN }
        val phone = value("TEL")
        val email = value("EMAIL")
        val org = value("ORG").replace(';', ' ').trim()

        val fields = mutableListOf<Field>()
        if (name.isNotBlank()) fields.add(Field(R.string.f_name, name))
        if (phone.isNotBlank()) fields.add(Field(R.string.f_phone, phone))
        if (email.isNotBlank()) fields.add(Field(R.string.f_email, email))
        if (org.isNotBlank()) fields.add(Field(R.string.f_org, org))
        if (fields.isEmpty()) fields.add(Field(R.string.f_text, raw))

        return Parsed(
            ScanType.CONTACT, raw, fields,
            mapOf("name" to name, "phone" to phone, "email" to email, "org" to org),
        )
    }

    private fun query(s: String): Map<String, String> = s.split('&')
        .mapNotNull { p ->
            val i = p.indexOf('=')
            if (i <= 0) null else p.substring(0, i).lowercase() to Uri.decode(p.substring(i + 1))
        }
        .toMap()

    private fun splitUnescaped(s: String, sep: Char): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                sb.append(s[i + 1])
                i += 2
                continue
            }
            if (c == sep) {
                out.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}
