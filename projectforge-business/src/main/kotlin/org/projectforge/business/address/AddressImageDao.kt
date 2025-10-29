/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.address

import jakarta.persistence.Tuple
import mu.KotlinLogging
import org.projectforge.Constants
import org.projectforge.business.image.ImageService
import org.projectforge.common.extensions.formatBytesForUser
import org.projectforge.framework.persistence.database.TupleUtils
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class AddressImageDao {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressImageCache: AddressImageCache

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var imageService: ImageService

    fun findImage(
        addressId: Long,
        fetchImage: Boolean = false,
        fetchPreviewImage: Boolean = false,
        checkAccess: Boolean = true
    ): AddressImageDO? {
        var address: AddressDO? = null
        if (checkAccess) {
            address = addressDao.find(addressId, checkAccess = true) ?: return null // For access checking!
        }
        if (fetchImage && fetchPreviewImage) {
            // Fetch all:
            val res = persistenceService.find(AddressImageDO::class.java, addressId)
            if (res != null) {
                // Fix: lastUpdate is not set in the database for older entries.
                res.lastUpdate = res.lastUpdate ?: Date(0L)
            }
            return res
        }
        val result = AddressImageDO()
        val namedQuery = if (fetchImage) {
            AddressImageDO.SELECT_IMAGE_ONLY
        } else if (fetchPreviewImage) {
            AddressImageDO.SELECT_IMAGE_PREVIEW_ONLY
        } else {
            AddressImageDO.SELECT_WITHOUT_IMAGES
        }
        persistenceService.selectNamedSingleResult(namedQuery, Tuple::class.java, Pair("addressId", addressId))?.let {
            result.id = it[0] as? Long
            result.lastUpdate = TupleUtils.getDate(it, "lastUpdate")
            result.imageType = it.get("imageType", ImageType::class.java)
                ?: ImageType.PNG // Default image type for older images (<=8.0).
            if (fetchImage) {
                result.image = it.get("image", ByteArray::class.java)
            } else if (fetchPreviewImage) {
                result.imagePreview = it.get("imagePreview", ByteArray::class.java)
            }
        }
        // Fix: lastUpdate is not set in the database for older entries.
        result.lastUpdate = result.lastUpdate ?: Date(0L)
        return result
    }

    /**
     * Does the access checking. The user may only get images, if he has the select access to the given address.
     */
    @JvmOverloads
    open fun getImage(addressId: Long, checkAccess: Boolean = true): ByteArray? {
        return findImage(addressId, fetchImage = true, checkAccess = checkAccess)?.image
    }

    /**
     * Does the access checking. The user may only get images, if he has the select access to the given address.
     */
    open fun getPreviewImage(addressId: Long, checkAccess: Boolean = true): ByteArray? {
        return findImage(addressId, fetchPreviewImage = true, checkAccess = checkAccess)?.imagePreview
    }

    /**
     * Extract the image type from the filename. Supported image types are PNG and JPEG.
     * @param filename The filename.
     * @return The image type, if the image type is supported. Otherwise, null.
     * @see ImageType
     */
    fun getSupportedImageType(filename: String?): ImageType? {
        val imageType = ImageType.fromExtension(filename)
        return if (isSupportedImageType(imageType)) {
            imageType
        } else {
            null
        }
    }

    fun isSupportedImageType(imageType: ImageType?): Boolean {
        return imageType != null
    }

    /**
     * Does the access checking. The user may only modify images, if he has the access to modify the given address.
     */
    open fun saveOrUpdate(addressId: Long, image: ByteArray, imageType: ImageType): Boolean {
        if (!isSupportedImageType(imageType)) {
            log.error("Can't save or update immage of address. Unsupported image type: $imageType.")
            return false
        }
        val address = addressDao.find(addressId)
        if (address == null) {
            log.error("Can't save or update immage of address. Address #$addressId not found.")
            return false
        }
        address.imageLastUpdate = Date()
        addressDao.update(address) // Throws an exception if the logged-in user has now access.

        // Shrink image to maximum file size
        // Note: Also defined in AddressImageServicesRest.MAX_STORED_IMAGE_FILE_SIZE_KB
        val shrinkedImage =
            imageService.shrinkToMaxFileSize(image, MAX_SIZE_BYTES_OF_SHRINKED_IMAGES, imageType.extension)

        persistenceService.runInTransaction { context ->
            val addressImage = context.selectSingleResult(
                "from ${AddressImageDO::class.java.name} t where t.address = :address",
                AddressImageDO::class.java,
                Pair("address", address),
                attached = true,
            ) ?: AddressImageDO()
            addressImage.address = address
            addressImage.image = shrinkedImage
            addressImage.imagePreview = imageService.resizeImage(shrinkedImage)
            addressImage.imageType = imageType
            addressImage.lastUpdate = Date()
            if (addressImage.id != null) {
                // Update
                context.update(addressImage)
            } else {
                // Insert
                context.insert(addressImage)
            }
        }
        log.info("New image for address ${address.id} (${address.fullName}) saved.")
        addressImageCache.setExpired()
        return true
    }

    /**
     * Does the access checking. The user may only delete images, if he has the access to modify the given address.
     */
    open fun delete(addressId: Long): Boolean {
        val address = addressDao.find(addressId)
        if (address == null) {
            log.error("Can't save or update immage of address. Address #$addressId not found.")
            return false
        }
        address.imageLastUpdate = null
        addressDao.update(address) // Throws an exception if the logged-in user has now access.
        var success = false
        persistenceService.runInTransaction { context ->
            // Should be only one image. But for safety reasons we delete all images.
            context.executeQuery(
                "from ${AddressImageDO::class.java.name} t where t.address.id = :addressId",
                AddressImageDO::class.java,
                Pair("addressId", address.id),
                attached = true,
            ).forEach { image ->
                log.info("Image for address ${address.id} (${address.fullName}) deleted.")
                context.delete(image)
                success = true
            }
        }
        addressImageCache.setExpired()
        return success
    }

    /**
     * Shrinks all existing images in the database to the maximum file size.
     * This is useful after changing the MAX_STORED_IMAGE_FILE_SIZE_KB setting.
     * @return Statistics about the shrinking operation
     */
    open fun shrinkAllImages(): String {
        log.info("Starting shrinking of all address images...")
        val startTime = System.currentTimeMillis()

        val result = StringBuilder()
        result.append("Address Image Shrinking Report\n")
        result.append("=================================\n\n")
        result.append("Maximum file size: ${MAX_SIZE_BYTES_OF_SHRINKED_IMAGES.formatBytesForUser()}\n\n")

        var totalImages = 0
        var shrinkedImages = 0
        var skippedImages = 0
        var failedImages = 0
        var totalSizeBefore = 0L
        var totalSizeAfter = 0L

        // Load all address images with full image data
        val allImages = persistenceService.executeQuery(
            "from ${AddressImageDO::class.java.name} t",
            AddressImageDO::class.java,
        )

        result.append("Found ${allImages.size} images in database.\n\n")

        allImages.forEach { addressImage ->
            totalImages++
            val imageBytes = addressImage.image

            if (imageBytes == null || imageBytes.isEmpty()) {
                skippedImages++
                result.append("Skipped image for address ${addressImage.address?.id} (no image data)\n")
                return@forEach
            }

            val sizeBefore = imageBytes.size.toLong()
            totalSizeBefore += sizeBefore

            // Skip if already small enough
            if (sizeBefore <= MAX_SIZE_BYTES_OF_SHRINKED_IMAGES) {
                skippedImages++
                totalSizeAfter += sizeBefore
                result.append("Skipped image for address ${addressImage.address?.id} (already ${sizeBefore / 1024} KB)\n")
                return@forEach
            }

            try {
                // Shrink image
                val shrinkedBytes = imageService.shrinkToMaxFileSize(
                    imageBytes,
                    MAX_SIZE_BYTES_OF_SHRINKED_IMAGES,
                    addressImage.imageType?.extension ?: "png"
                )

                if (shrinkedBytes == null || shrinkedBytes.isEmpty()) {
                    failedImages++
                    totalSizeAfter += sizeBefore // Count original size
                    result.append("Failed to shrink image for address ${addressImage.address?.id}\n")
                    return@forEach
                }

                val sizeAfter = shrinkedBytes.size.toLong()
                totalSizeAfter += sizeAfter

                // Only update if shrinking was successful
                if (sizeAfter < sizeBefore) {
                    persistenceService.runInTransaction { context ->
                        val attached = context.selectSingleResult(
                            "from ${AddressImageDO::class.java.name} t where t.id = :id",
                            AddressImageDO::class.java,
                            Pair("id", addressImage.id),
                            attached = true,
                        )
                        if (attached != null) {
                            attached.image = shrinkedBytes
                            attached.imagePreview = imageService.resizeImage(shrinkedBytes)
                            attached.lastUpdate = java.util.Date()
                            context.update(attached)
                        }
                    }
                    shrinkedImages++
                    val savedKB = (sizeBefore - sizeAfter) / 1024
                    result.append("Shrinked image for address ${addressImage.address?.id}: ${sizeBefore / 1024} KB -> ${sizeAfter / 1024} KB (saved $savedKB KB)\n")
                } else {
                    skippedImages++
                    result.append("Skipped image for address ${addressImage.address?.id} (shrinking would increase size)\n")
                }
            } catch (e: Exception) {
                failedImages++
                totalSizeAfter += sizeBefore // Count original size
                result.append("Error shrinking image for address ${addressImage.address?.id}: ${e.message}\n")
                log.error("Error shrinking image for address ${addressImage.address?.id}", e)
            }
        }

        val duration = (System.currentTimeMillis() - startTime) / 1000
        val savedBytes = totalSizeBefore - totalSizeAfter
        val savedMB = savedBytes / (1024 * 1024)

        result.append("\n=================================\n")
        result.append("Summary:\n")
        result.append("  Total images: $totalImages\n")
        result.append("  Shrinked: $shrinkedImages\n")
        result.append("  Skipped: $skippedImages\n")
        result.append("  Failed: $failedImages\n")
        result.append("  Total size before: ${totalSizeBefore / (1024 * 1024)} MB\n")
        result.append("  Total size after: ${totalSizeAfter / (1024 * 1024)} MB\n")
        result.append("  Saved: $savedMB MB (${(savedBytes * 100) / maxOf(totalSizeBefore, 1)}%)\n")
        result.append("  Duration: $duration seconds\n")

        log.info("Image shrinking completed: $shrinkedImages shrinked, $skippedImages skipped, $failedImages failed, saved $savedMB MB")
        addressImageCache.setExpired()

        return result.toString()
    }

    companion object {
        private const val MAX_SIZE_BYTES_OF_SHRINKED_IMAGES = 100L * Constants.KB
    }
}
