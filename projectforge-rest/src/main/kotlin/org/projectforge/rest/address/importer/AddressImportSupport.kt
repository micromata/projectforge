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

package org.projectforge.rest.address.importer

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportStorageInfo

/**
 * The VCF import of addresses as the session holds it, shared by the import page ([AddressImportRest]) and the
 * address form, which reconciles one entry of it at a time (see `AddressSaveSupport`).
 *
 * The import is no bulk commit: after the upload every vCard is matched against the database at once, and the
 * user takes each entry over by opening it in the address form, comparing its fields and saving.
 */
object AddressImportSupport {
    /**
     * Session attribute of the [AddressImportStorage]. The name of the former React import page is kept, so an
     * import started before an update survives it.
     */
    const val SESSION_IMPORT_STORAGE_ATTR = "org.projectforge.rest.address.importer.AddressImportUploadPageRest.importStorage"

    /** How long an import stays in the session, in minutes. */
    const val SESSION_TTL_MINUTES = 30

    fun getStorage(request: HttpServletRequest): AddressImportStorage? {
        return ExpiringSessionAttributes.getAttribute(request, SESSION_IMPORT_STORAGE_ATTR, AddressImportStorage::class.java)
    }

    fun storeStorage(request: HttpServletRequest, storage: AddressImportStorage) {
        ExpiringSessionAttributes.setAttribute(request, SESSION_IMPORT_STORAGE_ATTR, storage, SESSION_TTL_MINUTES)
    }

    fun clearStorage(request: HttpServletRequest) {
        ExpiringSessionAttributes.removeAttribute(request, SESSION_IMPORT_STORAGE_ATTR)
    }

    /**
     * The entries of the import as the import page lists them.
     */
    fun entriesOf(storage: AddressImportStorage): List<AddressImportEntryData> {
        return storage.pairEntries.mapIndexed { index, pairEntry ->
            AddressImportEntryData(pairEntry, index, calculateAdditionalChanges(pairEntry))
        }
    }

    /**
     * Which fields have changed (are in oldDiffValues) but are not shown as columns of their own, and whether the
     * stored address is deleted (and will be restored by the import).
     * @return A translated, comma-separated list of the additional changes, or null if there are none.
     */
    fun calculateAdditionalChanges(pairEntry: ImportPairEntry<AddressImportDTO>): String? {
        val additionalChanges = mutableListOf<String>()
        if (pairEntry.stored?.deleted == true) {
            additionalChanges.add(translate("undelete"))
        }
        val oldDiffValues = pairEntry.oldDiffValues
        if (!oldDiffValues.isNullOrEmpty()) {
            additionalChanges.addAll(
                oldDiffValues.keys
                    .filter { !DISPLAYED_FIELDS.contains(it) }
                    .map { translate("address.${it.removePrefix("read.")}") }
                    .sorted()
            )
        }
        return if (additionalChanges.isEmpty()) null else additionalChanges.joinToString(", ")
    }

    /**
     * The fields of the vCard next to the ones of the database, for the comparison in the text parser of the
     * address form: field name -> map with the values "vcf" and "db". A field is left out if both are blank.
     */
    fun buildVcfComparisonData(
        vcfData: AddressImportDTO?,
        dbData: AddressImportDTO?,
    ): Map<String, Map<String, String?>> {
        val result = mutableMapOf<String, Map<String, String?>>()
        fun addField(fieldName: String, vcfValue: String?, dbValue: String?) {
            if (fieldName == "form") {
                // Only if the vCard has one: vCards usually carry no form of address, which must not read as
                // "delete the form of address".
                if (!vcfValue.isNullOrBlank()) {
                    result[fieldName] = mapOf("vcf" to vcfValue, "db" to dbValue)
                }
            } else if (!vcfValue.isNullOrBlank() || !dbValue.isNullOrBlank()) {
                result[fieldName] = mapOf("vcf" to vcfValue, "db" to dbValue)
            }
        }
        // Personal info
        addField("title", vcfData?.title, dbData?.title)
        addField("firstName", vcfData?.firstName, dbData?.firstName)
        addField("name", vcfData?.name, dbData?.name)
        addField("birthName", vcfData?.birthName, dbData?.birthName)
        addField("birthday", vcfData?.birthday?.toString(), dbData?.birthday?.toString())
        // Organization
        addField("organization", vcfData?.organization, dbData?.organization)
        addField("division", vcfData?.division, dbData?.division)
        addField("positionText", vcfData?.positionText, dbData?.positionText)
        // Business contact
        addField("email", vcfData?.email, dbData?.email)
        addField("businessPhone", vcfData?.businessPhone, dbData?.businessPhone)
        addField("mobilePhone", vcfData?.mobilePhone, dbData?.mobilePhone)
        addField("fax", vcfData?.fax, dbData?.fax)
        // Private contact
        addField("privateEmail", vcfData?.privateEmail, dbData?.privateEmail)
        addField("privatePhone", vcfData?.privatePhone, dbData?.privatePhone)
        addField("privateMobilePhone", vcfData?.privateMobilePhone, dbData?.privateMobilePhone)
        // Business address
        addField("addressText", vcfData?.addressText, dbData?.addressText)
        addField("addressText2", vcfData?.addressText2, dbData?.addressText2)
        addField("zipCode", vcfData?.zipCode, dbData?.zipCode)
        addField("city", vcfData?.city, dbData?.city)
        addField("state", vcfData?.state, dbData?.state)
        addField("country", vcfData?.country, dbData?.country)
        // Private address
        addField("privateAddressText", vcfData?.privateAddressText, dbData?.privateAddressText)
        addField("privateAddressText2", vcfData?.privateAddressText2, dbData?.privateAddressText2)
        addField("privateZipCode", vcfData?.privateZipCode, dbData?.privateZipCode)
        addField("privateCity", vcfData?.privateCity, dbData?.privateCity)
        addField("privateState", vcfData?.privateState, dbData?.privateState)
        addField("privateCountry", vcfData?.privateCountry, dbData?.privateCountry)
        // Postal address
        addField("postalAddressText", vcfData?.postalAddressText, dbData?.postalAddressText)
        addField("postalAddressText2", vcfData?.postalAddressText2, dbData?.postalAddressText2)
        addField("postalZipCode", vcfData?.postalZipCode, dbData?.postalZipCode)
        addField("postalCity", vcfData?.postalCity, dbData?.postalCity)
        addField("postalState", vcfData?.postalState, dbData?.postalState)
        addField("postalCountry", vcfData?.postalCountry, dbData?.postalCountry)
        // Other
        addField("website", vcfData?.website, dbData?.website)
        addField("comment", vcfData?.comment, dbData?.comment)
        addField("fingerprint", vcfData?.fingerprint, dbData?.fingerprint)
        addField("publicKey", vcfData?.publicKey, dbData?.publicKey)
        // Form of address and communication language
        addField("form", vcfData?.form?.toString(), dbData?.form?.toString())
        addField(
            "communicationLanguage",
            vcfData?.communicationLanguage?.displayName,
            dbData?.communicationLanguage?.displayName,
        )
        return result
    }

    /**
     * The fields shown as columns of their own (the diff cells use the prefix "read."), or deliberately left out
     * of the additional changes (the form of address, which vCards usually don't carry). The single address, email
     * and phone fields are part of the formatted columns.
     */
    private val DISPLAYED_FIELDS = setOf(
        "read.name", "read.firstName", "read.organization",
        "read.formattedBusinessAddress", "read.addressText", "read.addressText2", "read.zipCode", "read.city",
        "read.country", "read.state",
        "read.formattedPrivateAddress", "read.privateAddressText", "read.privateAddressText2", "read.privateZipCode",
        "read.privateCity", "read.privateCountry", "read.privateState",
        "read.formattedPostalAddress", "read.postalAddressText", "read.postalAddressText2", "read.postalZipCode",
        "read.postalCity", "read.postalCountry", "read.postalState",
        "read.formattedEmails", "read.email", "read.privateEmail",
        "read.formattedPhones", "read.businessPhone", "read.mobilePhone", "read.privatePhone",
        "read.privateMobilePhone", "read.fax",
        "read.comment",
        "read.form",
    )
}

/**
 * The import as its page shows it, in the shape of [org.projectforge.rest.importer.ImportView] (which the shared
 * import components of projectforge-next read), with the address specific entries.
 */
class AddressImportView(
    /** The name of the uploaded file, null if no import is in progress. */
    val filename: String? = null,
    val hasBeenReconciled: Boolean = false,
    val info: ImportStorageInfo? = null,
    val entries: List<AddressImportEntryData> = emptyList(),
) {
    constructor(storage: AddressImportStorage) : this(
        filename = storage.filename,
        hasBeenReconciled = true,
        info = storage.info,
        entries = AddressImportSupport.entriesOf(storage),
    )
}

/**
 * One entry of the import list, in the shape of [org.projectforge.rest.importer.ImportEntry].
 */
class AddressImportEntryData(
    /** The index of the entry in the import, which identifies it (`addressImport/entry/{index}`). */
    val id: Int,
    val status: String,
    val statusAsString: String,
    val error: String?,
    val hasError: Boolean,
    val read: AddressImportDTO?,
    val stored: AddressImportDTO?,
    /** The id of the matching address in the database, null for a new one. */
    val addressId: Long?,
    /** The old values of the modified fields, for the diff cells. */
    val oldDiffValues: Map<String, Any>?,
    /** Translated list of the changed fields not shown as columns of their own. */
    val additionalChanges: String?,
    /** Whether the vCard of this entry carries an image (see `addressImport/imagePreview/{index}`). */
    val hasImage: Boolean,
) {
    constructor(pairEntry: ImportPairEntry<AddressImportDTO>, index: Int, additionalChanges: String?) : this(
        id = index,
        status = pairEntry.status.name,
        statusAsString = pairEntry.statusAsString,
        error = pairEntry.error,
        hasError = !pairEntry.error.isNullOrBlank(),
        read = pairEntry.read,
        stored = pairEntry.stored,
        addressId = pairEntry.stored?.id,
        oldDiffValues = pairEntry.oldDiffValues,
        additionalChanges = additionalChanges,
        hasImage = pairEntry.read?.addressImage != null,
    )
}
