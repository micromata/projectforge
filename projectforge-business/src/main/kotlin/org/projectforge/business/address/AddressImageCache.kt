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

import jakarta.annotation.PostConstruct
import jakarta.persistence.Tuple
import mu.KotlinLogging
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.database.TupleUtils
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
open class AddressImageCache : AbstractCache() {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * Key is the address.id. Mustn't be synchronized, because it is only read.
     */
    private var imageMap = mapOf<Long, AddressImageDO>()

    @PostConstruct
    private fun postConstruct() {
        instance = this
    }

    /**
     * Returns the image for the given addressId.
     * The byte arrays of preview and image isn't loaded here.
     * @param addressId The address.id.
     * @return The image or null if not found.
     */
    fun getImage(addressId: Long?): AddressImageDO? {
        addressId ?: return null
        checkRefresh()
        return imageMap[addressId]
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing AddressImageCache ...")
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            // This method must not be synchronized because it works with a new copy of maps.
            val newMap = mutableMapOf<Long, AddressImageDO>()
            val em = context.em
            em.createQuery(SELECT_ADDRESS_IMAGE_INFO, Tuple::class.java).resultList
                .forEach { tuple ->
                    val addressId = TupleUtils.getLong(tuple, "addressId")!!
                    newMap[addressId] = AddressImageDO().also { image ->
                        image.lastUpdate = TupleUtils.getDate(tuple, "lastUpdate")
                        image.imageType = tuple.get("imageType", ImageType::class.java) ?: ImageType.PNG
                    }
                }
            imageMap = newMap
            log.info {
                "Initializing of AddressImageCache done. ${
                    context.formatStats(
                        true
                    )
                }"
            }
        }
    }

    companion object {
        lateinit var instance: AddressImageCache
            private set

        private val SELECT_ADDRESS_IMAGE_INFO =
            "SELECT address.id as addressId,lastUpdate as lastUpdate,imageType as imageType FROM ${AddressImageDO::class.simpleName}"
    }
}
