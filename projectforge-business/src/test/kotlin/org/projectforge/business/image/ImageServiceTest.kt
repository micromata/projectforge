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

package org.projectforge.business.image

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.projectforge.business.address.ImageType
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ImageServiceTest {
    private val imageService = ImageService()

    @Test
    fun `image with alpha channel is kept as PNG`() {
        val png = createImage(BufferedImage.TYPE_INT_ARGB, "png")
        val result = imageService.shrinkToMaxFileSize(png, 1_000_000)
        assertEquals(ImageType.PNG, result.imageType)
        assertNotNull(ImageIO.read(ByteArrayInputStream(result.bytes)))
        val preview = imageService.resizeImagePreserveRatio(png, 50, 50, ImageType.PNG)
        assertNotNull(preview)
        assertEquals(50, ImageIO.read(ByteArrayInputStream(preview)).width)
    }

    @Test
    fun `image without alpha channel is converted to JPEG`() {
        val png = createImage(BufferedImage.TYPE_INT_RGB, "png")
        assertEquals(ImageType.JPEG, imageService.shrinkToMaxFileSize(png, 1_000_000).imageType)
    }

    private fun createImage(type: Int, format: String): ByteArray {
        val image = BufferedImage(200, 100, type)
        val g = image.createGraphics()
        g.color = Color(255, 0, 0, 128)
        g.fillRect(0, 0, 100, 100)
        g.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(image, format, it) }.toByteArray()
    }
}
