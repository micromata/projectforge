package org.projectforge.rest.dto

import org.projectforge.business.address.*
import org.projectforge.framework.utils.LabelValueBean
import java.sql.Date
import java.util.*

class Kunde(var contactStatus: ContactStatus? = null,
            var addressStatus: AddressStatus? = null,
            var uid: String? = null,
            var name: String? = null,
            var firstName: String? = null,
            var form: FormOfAddress? = null,
            var title: String? = null,
            var positionText: String? = null,
            var organization: String? = null,
            var division: String? = null,
            var businessPhone: String? = null,
            var mobilePhone: String? = null,
            var fax: String? = null,
            var addressText: String? = null,
            var zipCode: String? = null,
            var city: String? = null,
            var country: String? = null,
            var state: String? = null,
            var postalAddressText: String? = null,
            var postalZipCode: String? = null,
            var postalCity: String? = null,
            var postalCountry: String? = null,
            var postalState: String? = null,
            var email: String? = null,
            var website: String? = null,
            var communicationLanguage: Locale? = null,
            var privatePhone: String? = null,
            var privateMobilePhone: String? = null,
            var privateAddressText: String? = null,
            var privateZipCode: String? = null,
            var privateCity: String? = null,
            var privateCountry: String? = null,
            var privateState: String? = null,
            var privateEmail: String? = null,
            var publicKey: String? = null,
            var fingerprint: String? = null,
            var comment: String? = null,
            var birthday: Date? = null,
            var imageUrl: String? = null,
            var previewImageUrl: String? = null,
            var instantMessaging: MutableList<LabelValueBean<InstantMessagingType, String>>? = null,
            var addressbookList: MutableSet<Addressbook>? = null
) : BaseObject<AddressDO>() {

    override fun copyFrom(src: AddressDO) {
        super.copyFrom(src)
        if (src.imageData != null) {
            imageUrl = "address/image/$id"
        }
        if (src.imageDataPreview != null) {
            previewImageUrl = "address/imagePreview/$id}"
        }
        if (!src.addressbookList.isNullOrEmpty()) {
            addressbookList = mutableSetOf()
            src.addressbookList?.forEach { srcAddressbook ->
                val addressbook = Addressbook()
                addressbook.copyFromMinimal(srcAddressbook)
                addressbookList!!.add(addressbook)
            }
        }
    }

    override fun copyTo(dest: AddressDO) {
        super.copyTo(dest)
        if (!addressbookList.isNullOrEmpty()) {
            dest.addressbookList = mutableSetOf()
            addressbookList?.forEach { srcAddressbook ->
                val addressbook = AddressbookDO()
                srcAddressbook.copyTo(addressbook)
                dest.addressbookList!!.add(addressbook)
            }
        }
    }
}
