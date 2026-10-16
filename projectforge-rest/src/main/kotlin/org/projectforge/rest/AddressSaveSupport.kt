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

package org.projectforge.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressImageDao
import org.projectforge.business.address.PersonalAddressDO
import org.projectforge.business.address.PersonalAddressDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.AddressImageServicesRest.Companion.SESSION_IMAGE_ATTR
import org.projectforge.rest.address.importer.AddressImportSupport
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.dto.Address
import org.projectforge.rest.importer.ImportEntry
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * What saving an address involves besides the entity itself, shared by the form of projectforge-next
 * ([AddressEntityRest]) and the React form the address campaigns still open ([AddressLegacyPagesRest]): the
 * favorite flag of the logged-in user, the image uploaded to (or deleted in) the session, and the entry of a running
 * VCF import the form was opened for.
 */
@Service
class AddressSaveSupport {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressImageDao: AddressImageDao

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    fun validate(validationErrors: MutableList<ValidationError>, dto: Address) {
        if (StringUtils.isAllBlank(dto.name, dto.firstName, dto.organization)) {
            validationErrors.add(ValidationError(translate("address.form.error.toFewFields"), fieldId = "name"))
        }
        if (dto.addressbookList.isNullOrEmpty()) {
            validationErrors.add(
                ValidationError(
                    translateMsg("validation.error.fieldRequired", translate("address.addressbooks")),
                    fieldId = "addressbookList",
                )
            )
        }
    }

    /**
     * Saves the favorite flag of the logged-in user and the image of the session (or deletes the image, if the user
     * removed it in the form).
     */
    fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: AddressDO, dto: Address) {
        val id = obj.id!!
        val personalAddress = PersonalAddressDO()
        personalAddress.address = addressDao.findOrLoad(id)
        personalAddress.isFavoriteCard = dto.isFavoriteCard
        personalAddressDao.setOwner(personalAddress, ThreadLocalUserContext.requiredLoggedInUserId)
        personalAddressDao.saveOrUpdate(personalAddress)

        val session = request.getSession(false)
        if (dto.imageDeleted) {
            // An image uploaded and removed again before saving must not be saved either.
            ExpiringSessionAttributes.removeAttribute(session, SESSION_IMAGE_ATTR)
            addressImageDao.delete(id)
            log.debug { "Deleted image of address $id on save (imageDeleted flag)." }
        } else {
            val image = ExpiringSessionAttributes.getAttribute(session, SESSION_IMAGE_ATTR)
            if (image is AddressImageServicesRest.SessionImage) {
                addressImageDao.saveOrUpdate(id, image.bytes, image.imageType)
                ExpiringSessionAttributes.removeAttribute(session, SESSION_IMAGE_ATTR)
            }
        }
    }

    /**
     * Sets the favorite flag of the logged-in user for the given address, without saving the address itself.
     */
    fun setFavorite(address: AddressDO, favorite: Boolean) {
        val owner = ThreadLocalUserContext.requiredLoggedInUser
        val personalAddress = personalAddressDao.getByAddressId(address.id, owner) ?: PersonalAddressDO()
        if (personalAddress.id == null) {
            if (!favorite) {
                return // Nothing to do: no personal entry, and none is needed.
            }
            personalAddress.address = address
            personalAddressDao.setOwner(personalAddress, owner.id!!)
        } else if (personalAddress.isFavoriteCard == favorite) {
            return
        }
        personalAddress.isFavoriteCard = favorite
        personalAddressDao.saveOrUpdate(personalAddress)
    }

    /**
     * After an address was created, updated, undeleted, marked as deleted or deleted from the form of an import
     * entry: matches the import against the database again and marks this entry as imported, so the import list
     * shows the current state.
     *
     * @param importIndex The index of the import entry the form was opened for, null if it wasn't.
     */
    fun reconcileImport(request: HttpServletRequest, importIndex: Int?) {
        importIndex ?: return
        val storage = AddressImportSupport.getStorage(request)
        if (storage == null) {
            log.warn { "Import storage not found in session (expired?), importIndex=$importIndex." }
            return
        }
        storage.reconcileImportStorage()
        val entry = storage.getImportEntryByIndex(importIndex)
        if (entry != null) {
            entry.status = ImportEntry.Status.IMPORTED
        } else {
            log.warn { "Import entry $importIndex not found, can't mark it as imported." }
        }
        AddressImportSupport.storeStorage(request, storage)
    }
}
