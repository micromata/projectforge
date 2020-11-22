/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.rest.config.Rest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.net.URLEncoder
import java.nio.file.FileSystems
import java.nio.file.Path


/**
 * Rest services for getting events.
 */
@RestController
@RequestMapping(BarcodeServicesRest.PATH)
class BarcodeServicesRest {
    class BarcodeRequest(var text: String, var width: Int = 250, var height: Int = 250)

    @PostMapping(value = ["qrcodeFromPost.png"], produces = [MediaType.IMAGE_PNG_VALUE])
    @ResponseBody
    fun getQrCode(@RequestBody barcodeRequest: BarcodeRequest): ByteArray {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(barcodeRequest.text, BarcodeFormat.QR_CODE, barcodeRequest.width, barcodeRequest.height)
        val pngOutputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream)
        return pngOutputStream.toByteArray()
    }

    @GetMapping(value = ["qrcode.png"], produces = [MediaType.IMAGE_PNG_VALUE])
    @ResponseBody
    fun getQrCode(@RequestParam("width") width: Int?,
                  @RequestParam("height") height: Int?,
                  @RequestParam("text") text: String?)
            : ByteArray {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(text ?: "", BarcodeFormat.QR_CODE, width ?: 250, height ?: 250)
        val pngOutputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream)
        return pngOutputStream.toByteArray()
    }

    companion object {
        fun getBarcodeGetUrl(text: String): String {
            return "$GET_URL?text=${URLEncoder.encode("$text", "UTF-8")}"
        }

        internal const val PATH = "${Rest.URL}/barcode"

        const val POST_URL = "$PATH/qrcodeFromPost.png"

        const val GET_URL = "$PATH/qrcode.png"
    }
}

fun main() {
    val text = "https://www.projectforge.org"
    val barcodeRequest = BarcodeServicesRest.BarcodeRequest(text = text)
    val qrCodeWriter = QRCodeWriter()
    val bitMatrix = qrCodeWriter.encode(barcodeRequest.text, BarcodeFormat.QR_CODE, barcodeRequest.width, barcodeRequest.height)
    val path: Path = FileSystems.getDefault().getPath("test.png")
    MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path)
}
