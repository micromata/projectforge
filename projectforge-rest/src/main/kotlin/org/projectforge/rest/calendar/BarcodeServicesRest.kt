/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.calendar

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import org.apache.commons.io.output.ByteArrayOutputStream
import org.springframework.http.MediaType
import java.util.Base64

/**
 * QR codes as data urls. There is no endpoint serving them as images: a code may carry a secret (keys, tokens), which
 * must not be part of an url, and its size isn't left to the client.
 */
class BarcodeServicesRest {
    companion object {
        /**
         * The QR code of the given text as data url (`data:image/png;base64,...`) for an img tag. For secrets, which
         * must not be part of an url: the code is delivered in the (uncached) json response instead.
         */
        fun getQrCodeDataUrl(text: String, width: Int = 250, height: Int = 250): String {
            val png = createQrCodePng(text, width, height)
            return "data:${MediaType.IMAGE_PNG_VALUE};base64,${Base64.getEncoder().encodeToString(png)}"
        }

        private fun createQrCodePng(text: String, width: Int, height: Int): ByteArray {
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height)
            val pngOutputStream = ByteArrayOutputStream()
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream)
            return pngOutputStream.toByteArray()
        }
    }
}
