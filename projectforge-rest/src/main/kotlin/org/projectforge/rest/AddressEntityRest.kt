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
import org.projectforge.NextMigration
import org.projectforge.business.address.*
import org.projectforge.business.sipgate.SipgateConfiguration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AbstractEntityRest.CloneSupport
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.Address
import org.projectforge.rest.dto.PostData
import org.projectforge.sms.SmsSenderConfig
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * The layout-free REST endpoint of the addresses, serving the hand-built list, view and form of projectforge-next
 * (see components/features/address). Replaces the React UILayout `AddressPagesRest`; the React form is still served
 * for the address campaigns by [AddressLegacyPagesRest].
 *
 * The services around an address (image, vCard, text parser, CardDAV) are served by [AddressServicesRest] and
 * [AddressImageServicesRest] under the same path, the VCF import by
 * [org.projectforge.rest.address.importer.AddressImportRest].
 */
@RestController
@RequestMapping("${Rest.URL}/address")
class AddressEntityRest : AbstractDTOEntityRest<AddressDO, Address, AddressDao>(
    AddressDao::class.java,
    i18nKeyPrefix = "address.title",
    cloneSupport = CloneSupport.CLONE,
) {
    companion object {
        /** Set by the CardDAV module, if its server is enabled: the list offers its info dialog then. */
        var carddavServerEnabled = false
    }

    /**
     * What the view page of an address needs besides the address: whether a phone number may be called or sent a
     * text message from here.
     */
    class AddressView(
        val address: Address,
        val phoneCallEnabled: Boolean,
        val smsEnabled: Boolean,
    )

    /** An existing address with the same name, for the warning of the form while adding one. */
    class DuplicateAddress(
        val id: Long,
        val fullName: String?,
        val organization: String?,
        val email: String?,
    )

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Autowired
    private lateinit var addressExport: AddressExport

    @Autowired
    private lateinit var addressSaveSupport: AddressSaveSupport

    @Autowired
    private lateinit var personalAddressCache: PersonalAddressCache

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var sipgateConfiguration: SipgateConfiguration

    @Autowired
    private lateinit var smsSenderConfig: SmsSenderConfig

    override fun transformForDB(dto: Address): AddressDO {
        val addressDO = AddressDO()
        dto.copyTo(addressDO)
        return addressDO
    }

    override fun transformFromDB(obj: AddressDO, editMode: Boolean): Address {
        val address = Address()
        address.copyFrom(obj)
        // The image itself is fetched by its own url, the DTO only says whether there is one.
        address.imageData = null
        address.imageDataPreview = null
        address.isFavoriteCard = personalAddressCache.isPersonalAddress(obj.id)
        return address
    }

    /**
     * A new address is up to date, active and in the global address book.
     */
    override fun newBaseDO(request: HttpServletRequest?): AddressDO {
        val address = super.newBaseDO(request)
        address.addressStatus = AddressStatus.UPTODATE
        address.contactStatus = ContactStatus.ACTIVE
        address.add(addressbookDao.globalAddressbook)
        return address
    }

    /**
     * A row and a search hit lead to the view of an address (`next/address/:id`), not to its form.
     */
    override fun getStandardEditPage(): String {
        return "${NextMigration.listUrl(category)}/${NextMigration.ID_PLACEHOLDER}"
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterElement(
                "myFavorites",
                UIFilterElement.FilterType.BOOLEAN,
                translate("address.filter.myFavorites"),
                defaultFilter = true,
            )
        )
        elements.add(UIFilterElement("doublets", UIFilterElement.FilterType.BOOLEAN, translate("address.filter.doublets")))
        elements.add(UIFilterElement("images", UIFilterElement.FilterType.BOOLEAN, translate("address.filter.images")))
        elements.add(
            UIFilterElement(
                "myEntries",
                UIFilterElement.FilterType.BOOLEAN,
                translate("address.filter.myEntries"),
                tooltip = translate("address.filter.myEntries.tooltip"),
                defaultFilter = false,
            )
        )
    }

    /**
     * The four filters of the list are no properties of the address, so they are synthetic entries the query
     * ignores, applied to the result instead.
     *
     * Unlike the former React list, the result is not cut to the page size: the list of projectforge-next pages
     * on the client and needs the whole result (capped by maxRows).
     */
    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<AddressDO>>? {
        fun synthetic(field: String): Boolean {
            val entry = source.entries.find { it.field == field } ?: return false
            entry.synthetic = true
            return entry.isTrueValue
        }
        val doublets = synthetic("doublets")
        val myFavorites = synthetic("myFavorites")
        val images = synthetic("images")
        val myEntries = synthetic("myEntries")
        val filters = mutableListOf<CustomResultFilter<AddressDO>>()
        if (doublets) {
            filters.add(DoubletsResultFilter())
        }
        if (myFavorites) {
            filters.add(FavoritesResultFilter(personalAddressDao))
        }
        if (images) {
            filters.add(ImagesResultFilter())
        }
        if (myEntries) {
            filters.add(AddressMyEntriesResultFilter(persistenceService, ThreadLocalUserContext.requiredLoggedInUserId))
        }
        return filters
    }

    /**
     * Sets also uid to null.
     */
    override fun prepareClone(dto: Address): Address {
        val clone = super.prepareClone(dto)
        clone.uid = null
        return clone
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: Address) {
        addressSaveSupport.validate(validationErrors, dto)
    }

    override fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: AddressDO, postData: PostData<Address>) {
        addressSaveSupport.onAfterSaveOrUpdate(request, obj, postData.data)
        addressSaveSupport.reconcileImport(request, postData.data.importIndex)
    }

    override fun onAfterUndelete(request: HttpServletRequest, obj: AddressDO, postData: PostData<Address>): ResponseAction {
        addressSaveSupport.reconcileImport(request, postData.data.importIndex)
        return super.onAfterUndelete(request, obj, postData)
    }

    override fun onAfterMarkAsDeleted(request: HttpServletRequest, obj: AddressDO, postData: PostData<Address>): ResponseAction {
        addressSaveSupport.reconcileImport(request, postData.data.importIndex)
        return super.onAfterMarkAsDeleted(request, obj, postData)
    }

    override fun onAfterDelete(request: HttpServletRequest, obj: AddressDO, postData: PostData<Address>): ResponseAction {
        addressSaveSupport.reconcileImport(request, postData.data.importIndex)
        return super.onAfterDelete(request, obj, postData)
    }

    override val autoCompleteSearchFields = arrayOf("name", "firstName", "organization", "city", "privateCity")

    /** Name hits first, see [autoCompletePrimarySearchFields]. */
    override val autoCompletePrimarySearchFields = arrayOf("name", "firstName")

    override fun addVariablesForListPage(): Map<String, Any> {
        return mapOf(
            "phoneCallEnabled" to sipgateConfiguration.isConfigured(),
            "smsEnabled" to smsSenderConfig.isSmsConfigured(),
            "carddavEnabled" to carddavServerEnabled,
        )
    }

    /**
     * The address as its view page shows it. Remembers the address as the one to highlight in the list, as the
     * former view of the React app did.
     */
    @AccessChecked("DAO: select access (AddressDao.find)")
    @GetMapping("view/{id:\\d+}")
    fun getView(@PathVariable("id") id: Long): ResponseEntity<AddressView> {
        val address = getById(id, editMode = true) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        userPrefService.putEntry(category, USER_PREF_PARAM_HIGHLIGHT_ROW, id, false)
        return ResponseEntity.ok(
            AddressView(
                address,
                phoneCallEnabled = sipgateConfiguration.isConfigured(),
                smsEnabled = smsSenderConfig.isSmsConfigured(),
            )
        )
    }

    /**
     * Marks the address as a favorite of the logged-in user (or removes the mark), without saving the address.
     */
    @AccessChecked("DAO: select access (AddressDao.find); the personal address belongs to the logged-in user")
    @PostMapping("favorite/{id:\\d+}")
    fun setFavorite(@PathVariable("id") id: Long, @RequestParam("favorite") favorite: Boolean): ResponseEntity<Map<String, Boolean>> {
        val address = baseDao.find(id) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        addressSaveSupport.setFavorite(address, favorite)
        return ResponseEntity.ok(mapOf("isFavoriteCard" to favorite))
    }

    /**
     * The addresses with the given name and first name the logged-in user may see, for the warning of the form
     * while adding an address.
     */
    @AccessChecked("DAO: select access (AddressDao.findByNameAndFirstName with checkAccess)")
    @GetMapping("duplicates")
    fun getDuplicates(
        @RequestParam("name", required = false) name: String?,
        @RequestParam("firstName", required = false) firstName: String?,
    ): List<DuplicateAddress> {
        return baseDao.findByNameAndFirstName(name, firstName, checkAccess = true).map {
            DuplicateAddress(it.id!!, it.fullName, it.organization, it.email)
        }
    }

    /**
     * The filtered list as Excel file, with the favorite flags of the logged-in user. An empty result answers 404
     * rather than a file.
     */
    @AccessChecked("DAO: select access (list result filtered by baseDao)")
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info { "Exporting addresses as Excel file." }
        val list = getResultList(filter)
        val xls = addressExport.export(list, personalAddressDao.personalAddressByAddressId)
        if (list.isEmpty() || xls == null || xls.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        val filename = "ProjectForge-AddressExport_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx"
        return RestUtils.downloadFile(filename, xls)
    }
}
