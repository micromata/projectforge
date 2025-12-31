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

import mu.KotlinLogging
import org.projectforge.business.address.ImageType
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.min

private val log = KotlinLogging.logger {}

@Service
class ImageService {
    /**
     * Resizes image to fit within max dimensions while preserving aspect ratio.
     * Attempts to convert to JPEG. If that fails (e.g., transparency), uses PNG.
     * @param originalImage Original image bytes
     * @param maxWidth Maximum width in pixels
     * @param maxHeight Maximum height in pixels
     * @param format The format of the input image (used for determining output format)
     * @return Resized image bytes in appropriate format
     */
    fun resizeImagePreserveRatio(
        originalImage: ByteArray,
        maxWidth: Int,
        maxHeight: Int,
        format: ImageType
    ): ByteArray? {
        val image = createImageFromBytes(originalImage)
        if (image == null) {
            log.error("Could not decode image for resizing")
            return originalImage
        }

        val width = image.width
        val height = image.height

        // Calculate scale to fit within max dimensions while preserving aspect ratio
        val scaleWidth = maxWidth.toDouble() / width
        val scaleHeight = maxHeight.toDouble() / height
        val scale = min(scaleWidth, scaleHeight)

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        val resized = minimizeImage(image, newWidth, newHeight)

        // Use same format strategy as shrinkToMaxFileSize
        // Try JPEG first, fall back to PNG if it fails
        var result = createBytesFromImage(resized, ImageType.JPEG, 0.85f)
        if (result == null) {
            // JPEG failed, likely due to transparency - use PNG
            result = createBytesFromImage(resized, format)
        }
        return result
    }

    /**
     * Shrinks an image to fit within a maximum file size using binary search for optimal dimensions.
     * Attempts to convert to JPEG for optimal compression. If JPEG conversion fails (e.g., due to
     * transparency in PNG), falls back to PNG format.
     * @param originalImage The original image as byte array
     * @param maxSizeBytes Maximum file size in bytes
     * @return ImageResult containing the minimized image bytes and the format (JPEG or PNG)
     */
    fun shrinkToMaxFileSize(originalImage: ByteArray, maxSizeBytes: Long): ImageResult {
        val image = createImageFromBytes(originalImage)
        if (image == null) {
            log.error("Could not decode image for minimization")
            // Try to detect format from original bytes
            val detectedType = detectImageType(originalImage)
            return ImageResult(originalImage, detectedType)
        }

        // Try JPEG conversion first for optimal compression
        var format = ImageType.JPEG

        // Attempt JPEG conversion - will fail for images with transparency (e.g., PNG with alpha channel)
        var imageBytes = createBytesFromImage(image, format, DEFAULT_JPEG_QUALITY)

        if (imageBytes == null) {
            // JPEG conversion failed, fall back to PNG
            // This can happen for images with transparency (PNG with alpha, GIF)
            log.info("JPEG conversion failed (possibly due to transparency or unsupported format), using PNG format instead")
            format = detectImageType(originalImage)
            imageBytes = createBytesFromImage(image, format)

            if (imageBytes == null) {
                log.error("Could not convert image to any format")
                val detectedType = detectImageType(originalImage)
                return ImageResult(originalImage, detectedType)
            }
        }

        // If already small enough after conversion, return it
        if (imageBytes.size <= maxSizeBytes) {
            log.debug { "Image fits after $format conversion: ${originalImage.size} bytes -> ${imageBytes.size} bytes" }
            return ImageResult(imageBytes, format)
        }

        // Define optimal range (95-100% of maxSizeBytes)
        val targetMinSize = (maxSizeBytes * 0.95).toLong()

        // Constants
        val MIN_DIMENSION = 100
        val MAX_ITERATIONS = 10

        // Work with JPEG-converted image from here on
        var bestResult: ByteArray = imageBytes
        val originalWidth = image.width
        val originalHeight = image.height

        // Track best quality found - will be used in Strategy 2 if needed
        var bestQuality = if (format == ImageType.JPEG) DEFAULT_JPEG_QUALITY else 1.0f

        // Strategy 1: Try JPEG quality reduction first (only for JPEG format)
        if (format == ImageType.JPEG) {
            val qualityResultPair = tryReduceJpegQuality(image, maxSizeBytes, targetMinSize)
            if (qualityResultPair != null) {
                val qualityResult = qualityResultPair.first
                val usedQuality = qualityResultPair.second

                // If result is in optimal range, return it immediately
                if (qualityResult.size >= targetMinSize && qualityResult.size <= maxSizeBytes) {
                    log.info(
                        "Image shrinked by reducing JPEG quality to optimal range: {} bytes -> {} bytes (quality={})",
                        originalImage.size, qualityResult.size, usedQuality
                    )
                    return ImageResult(qualityResult, ImageType.JPEG)
                } else if (qualityResult.size <= maxSizeBytes) {
                    bestResult = qualityResult // Keep as fallback, but try to get closer to target
                    bestQuality = usedQuality // Use this quality for further processing
                }
            }
        }

        // Strategy 2: Binary search for optimal scale factor
        var minScale = 0.0
        var maxScale = 1.0
        var result: ByteArray? = null

        for (iteration in 0..<MAX_ITERATIONS) {
            val currentScale = (minScale + maxScale) / 2.0

            val newWidth = (originalWidth * currentScale).toInt()
            val newHeight = (originalHeight * currentScale).toInt()

            // Check minimum dimension constraint BEFORE creating the image
            if (newWidth < MIN_DIMENSION || newHeight < MIN_DIMENSION) {
                log.warn(
                    "Reached minimum dimension limit ({}x{}) during binary search at iteration {}, using best result from previous iterations",
                    newWidth, newHeight, iteration + 1
                )
                break
            }

            // Resize image and use best quality found from Strategy 1
            val resized = minimizeImage(image, newWidth, newHeight)
            result = createBytesFromImage(resized, format, bestQuality)

            if (result == null) {
                log.warn("Failed to create image bytes at scale {} during iteration {}", currentScale, iteration + 1)
                break
            }

            val resultSize = result.size.toLong()

            // Check if we're in the optimal range (95-100% of target)
            if (resultSize >= targetMinSize && resultSize <= maxSizeBytes) {
                log.info(
                    "Image minimized via binary search (iteration {}): {} bytes -> {} bytes ({}x{} -> {}x{}, scale={}%)",
                    iteration + 1, originalImage.size, resultSize,
                    originalWidth, originalHeight, newWidth, newHeight, (currentScale * 100).toInt()
                )
                return ImageResult(result, format)
            }

            // Update best result - always keep the smallest result we've created
            // Even if it's slightly over the limit, it's better than what we had before
            if (resultSize < bestResult.size) {
                bestResult = result
            }

            // Adjust search range for binary search
            if (resultSize > maxSizeBytes) {
                // Image too large, search smaller scales
                maxScale = currentScale
            } else {
                // Image too small, search larger scales to get closer to target
                minScale = currentScale
            }
        }

        // Return best result found
        if (bestResult.size <= maxSizeBytes) {
            log.info("Image minimized to best fit: {} bytes -> {} bytes", originalImage.size, bestResult.size)
            return ImageResult(bestResult, format)
        }

        // If still too large, return best effort
        log.warn("Could not minimize image to {} bytes, returning best effort: {} bytes", maxSizeBytes, bestResult.size)
        return ImageResult(bestResult, format)
    }

    /**
     * Tries to shrink JPEG quality to fit within size constraints.
     * Only reduces quality down to 0.7 to maintain acceptable image quality.
     * Tests qualities in descending order: 0.85, 0.8, 0.7
     * @param image The image to compress
     * @param maxSizeBytes Maximum file size in bytes
     * @param targetMinSize Minimum target size (95% of maxSizeBytes) for optimal result
     * @return Pair of (shrinked image bytes, quality used), or null if no acceptable result found
     */
    private fun tryReduceJpegQuality(
        image: BufferedImage,
        maxSizeBytes: Long,
        targetMinSize: Long
    ): Pair<ByteArray, Float>? {
        // Test quality levels from 0.85 down to 0.7 (initial conversion was 0.9)
        val qualities = floatArrayOf(0.85f, 0.8f, 0.7f)
        var bestResult: Pair<ByteArray, Float>? = null

        for (quality in qualities) {
            val result = createBytesFromImage(image, ImageType.JPEG, quality)
            if (result != null) {
                // If in optimal range (95-100% of target), return immediately
                if (result.size >= targetMinSize && result.size <= maxSizeBytes) {
                    return Pair(result, quality)
                }
                // Keep best result that fits under the limit
                if (result.size <= maxSizeBytes) {
                    bestResult = Pair(result, quality)
                }
            }
        }
        return bestResult
    }

    private fun minimizeImage(originalImage: BufferedImage?, width: Int, height: Int): BufferedImage? {
        if (originalImage != null) {
            val type = if (originalImage.type == 0) BufferedImage.TYPE_INT_ARGB else originalImage.type
            val resizedImage = BufferedImage(width, height, type)
            val g = resizedImage.createGraphics()
            g.drawImage(originalImage, 0, 0, width, height, null)
            g.dispose()
            return resizedImage
        }
        return null
    }

    private fun createBytesFromImage(
        image: BufferedImage?,
        format: ImageType = ImageType.JPEG,
        quality: Float = DEFAULT_JPEG_QUALITY,
    ): ByteArray? {
        if (image != null) {
            try {
                val baos = ByteArrayOutputStream()

                if (format == ImageType.JPEG) {
                    // Use JPEG compression with specified quality
                    val writer = ImageIO.getImageWritersByFormatName("jpg").next()
                    val param = writer.defaultWriteParam
                    if (param.canWriteCompressed()) {
                        param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                        param.compressionQuality = quality // 0.0 - 1.0
                    }
                    val ios = ImageIO.createImageOutputStream(baos)
                    writer.output = ios
                    writer.write(null, IIOImage(image, null, null), param)
                    writer.dispose()
                    ios.close()
                } else {
                    // Use PNG or other format
                    ImageIO.write(image, format.extension, baos)
                }

                baos.flush()
                val imageInByte = baos.toByteArray()
                baos.close()
                return imageInByte
            } catch (e: IOException) {
                log.error("Error while writing image : " + e.message)
                return null
            }
        }
        return null
    }

    private fun createImageFromBytes(imageData: ByteArray): BufferedImage? {
        val bais = ByteArrayInputStream(imageData)
        try {
            return ImageIO.read(bais)
        } catch (e: IOException) {
            log.error("Error while read ByteArrayInputStream : " + e.message)
            return null
        }
    }

    /**
     * Detects image type from byte array by checking magic bytes.
     * @param imageData The image data as byte array
     * @return The detected ImageType (JPEG, PNG, or GIF), defaults to PNG if unknown
     */
    private fun detectImageType(imageData: ByteArray): ImageType {
        if (imageData.size >= 3) {
            // JPEG magic bytes: FF D8 FF
            if (imageData[0] == 0xFF.toByte() && imageData[1] == 0xD8.toByte() && imageData[2] == 0xFF.toByte()) {
                return ImageType.JPEG
            }
            // GIF magic bytes: 47 49 46 ("GIF")
            if (imageData[0] == 0x47.toByte() && imageData[1] == 0x49.toByte() && imageData[2] == 0x46.toByte()) {
                return ImageType.GIF
            }
        }
        if (imageData.size >= 4) {
            // PNG magic bytes: 89 50 4E 47
            if (imageData[0] == 0x89.toByte() && imageData[1] == 0x50.toByte() &&
                imageData[2] == 0x4E.toByte() && imageData[3] == 0x47.toByte()
            ) {
                return ImageType.PNG
            }
        }
        // Default to PNG if unknown
        return ImageType.PNG
    }

    companion object {
        const val DEFAULT_JPEG_QUALITY = 0.9f  // Use high quality for dimensional scaling
    }
}

/**
 * Result of image processing operations containing the processed image bytes and its type.
 * @param bytes The processed image as byte array
 * @param imageType The format of the image (JPEG or PNG)
 */
data class ImageResult(
    val bytes: ByteArray,
    val imageType: ImageType
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageResult

        if (!bytes.contentEquals(other.bytes)) return false
        if (imageType != other.imageType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + imageType.hashCode()
        return result
    }
}
