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

package org.projectforge.rest.businesscard

import de.micromata.merlin.utils.ReplaceUtils
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.ContactImageUrl
import org.projectforge.business.address.businesscard.BusinessCard
import org.projectforge.business.address.businesscard.BusinessCardService
import org.projectforge.common.i18n.UserException
import org.projectforge.rest.calendar.BarcodeServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The digital business cards of the logged-in user (next page `myBusinessCards`). Everything works on the own cards
 * only, see [BusinessCardService]; there is no user parameter.
 */
@RestController
@RequestMapping("${Rest.URL}/myBusinessCards")
class BusinessCardRest {
    @Autowired
    private lateinit var businessCardService: BusinessCardService

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var contactImageUrl: ContactImageUrl

    class CardInfo(
        val card: BusinessCard,
        val preview: Preview,
    )

    class Preview(
        /** The QR code with the vCard, as data url. */
        val qrCodeUrl: String,
        /** Length of the vCard: the QR code gets dense with a long one, the page warns. */
        val vCardLength: Int,
        /** The vCard has a photo url (on the gateway). */
        val withPhoto: Boolean,
        /** The source address of the card is the user's own one, so its photo may be published. */
        val photoAllowed: Boolean,
    )

    class InitData(
        val cards: List<CardInfo>,
        /** A new card from the address that is most likely the user's own one, if any. */
        val suggestion: BusinessCard?,
        val maxCards: Int,
        /** Without a gateway there is no photo url (photos can't be published). */
        val gatewayConfigured: Boolean,
    )

    @AccessChecked("Own user only (business cards of the logged-in user)")
    @GetMapping
    fun getInitialData(): InitData {
        return InitData(
            cards = businessCardService.list().map { CardInfo(it, preview(it)) },
            suggestion = businessCardService.suggestAddresses().firstOrNull()?.let { BusinessCardService.createFrom(it) },
            maxCards = BusinessCardService.MAX_CARDS_PER_USER,
            gatewayConfigured = contactImageUrl.gatewayPublicUrl.isNotBlank(),
        )
    }

    /**
     * A new card (not stored) from the given address, readable for the logged-in user.
     */
    @AccessChecked("Own user only; the address is read with the access check of the logged-in user")
    @GetMapping("fromAddress")
    fun fromAddress(@RequestParam("id") id: Long): ResponseEntity<BusinessCard> {
        val address = addressDao.find(id, checkAccess = true)?.takeIf { !it.deleted }
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(BusinessCardService.createFrom(address))
    }

    /**
     * The QR code of a card being edited (nothing is stored). POST because the card is the body.
     */
    @AccessChecked("Own user only (preview of a card of the logged-in user, nothing stored)")
    @PostMapping("preview")
    fun getPreview(@RequestBody card: BusinessCard): Preview {
        return preview(card)
    }

    /**
     * Inserts (no id) or updates the own card. Refusals (missing name, limit, photo not from the own address,
     * unknown id) are answered with 406 and the error, as the edit pages do.
     */
    @AccessChecked("Own user only (business cards of the logged-in user). CSRF token required")
    @PostMapping("save")
    fun save(@RequestBody postData: PostData<BusinessCard>): ResponseEntity<ResponseAction> {
        val stored = try {
            businessCardService.save(postData.data)
        } catch (ex: UserException) {
            val fieldId = if (ex.i18nKey == BusinessCardService.ERROR_NAME_REQUIRED) "cardName" else null
            return ResponseEntity(
                ResponseAction(validationErrors = listOf(ValidationError.create(ex.i18nKey, fieldId = fieldId))),
                HttpStatus.NOT_ACCEPTABLE,
            )
        }
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.NOTHING).addVariable("data", CardInfo(stored, preview(stored)))
        )
    }

    @AccessChecked("Own user only (business cards of the logged-in user). CSRF token required")
    @PostMapping("delete")
    fun delete(@RequestParam("id") id: String): ResponseEntity<ResponseAction> {
        if (!businessCardService.delete(id)) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
    }

    @AccessChecked("Own user only (business cards of the logged-in user)")
    @GetMapping("vcf")
    fun downloadVCard(@RequestParam("id") id: String): ResponseEntity<*> {
        val card = businessCardService.get(id) ?: return ResponseEntity<Any>(HttpStatus.NOT_FOUND)
        val name = listOfNotNull(card.firstName, card.lastName).joinToString(" ").ifBlank { card.cardName ?: "card" }
        val filename = ReplaceUtils.encodeFilename(name, true) + ".vcf"
        return RestUtils.downloadFile(filename, businessCardService.buildVCard(card))
    }

    private fun preview(card: BusinessCard): Preview {
        // Looked up once: the vCard and the check box both need it.
        val photoSource = businessCardService.photoSource(card)
        val vCard = businessCardService.buildVCard(card, photoSource)
        return Preview(
            qrCodeUrl = BarcodeServicesRest.getQrCodeDataUrl(vCard, QR_SIZE, QR_SIZE),
            vCardLength = vCard.length,
            withPhoto = vCard.contains("\nPHOTO"),
            photoAllowed = photoSource != null,
        )
    }

    companion object {
        private const val QR_SIZE = 400
    }
}
