/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.address.vcard

import net.fortuna.ical4j.util.CompatibilityHints
import net.fortuna.ical4j.vcard.VCard
import org.projectforge.business.address.AddressDO
import java.io.InputStream

object VCardUtils {
    init {
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
    }

    @JvmStatic
    fun convert(inputStream: InputStream): List<AddressDO> {
        throw RuntimeException("Not implemented yet.")
        /*val vCardBuilder = VCardBuilder(inputStream)
        val addresses = mutableListOf<AddressDO>()
        while (true) {
            val card: VCard? = vCardBuilder.build()
            if (card == null) break
            addresses.add(convert(card))
        }
        return addresses*/
    }

    fun convert(content: String): List<AddressDO> {
        throw RuntimeException("Not implemented yet.")
        /*        val reader = StringReader(content)
                val vCardBuilder = VCardBuilder(reader)
                val addresses = mutableListOf<AddressDO>()
                var paranoiaCounter = 10000
                while (true) {
                    val card: VCard? = vCardBuilder.build()
                    if (card == null) break
                    addresses.add(convert(card))
                    if (--paranoiaCounter <= 0) {
                        throw RuntimeException("Paranoia counter reached.")
                    }
                }
                return addresses*/
    }

    fun convert(card: VCard): AddressDO {
        val address = AddressDO()
        val all = card.entities
        /*        all.first().getAddresses()
                all.forEach { entry ->
                    entry.propertyList.forEach { property ->
                        when (property.id) {
                            Property.Id.N -> address.setName(property)
                            Property.Id.ORG -> address.setOrganization(property)
                            Property.Id.BDAY -> address.setBirth(property)
                            Property.Id.NOTE -> address.setNote(property)
                            else -> address.setProperties(property)
                        }
                    }
                    entry.findProperty(Property.Id.N)?.let { address.setName(it) }
                }
                card.entityList.all.forEach { entry ->
                    when (entry.key) {
                        Property.Id.N -> address.setName(entry.value)
                        Property.Id.ORG -> address.setOrganization(entry.value)
                        Property.Id.BDAY -> address.setBirth(entry.value)
                        Property.Id.NOTE -> address.setNote(entry.value)
                        else -> address.setProperties(entry.key, entry.value)
                    }
                }
                card.egetProperty(Property.Id.N)?.let { address.setName(it) }*/
        /*        // //// SET BASE DATA
                if (card.getProperty(net.fortuna.ical4j.vcard.Property.Id.N) != null) {
                    setName(card.getProperty(net.fortuna.ical4j.vcard.Property.Id.N), newAddress);
                }
                if (card.getProperty(net.fortuna.ical4j.vcard.Property.Id.ORG) != null) {
                    setOrganization(card.getProperty(net.fortuna.ical4j.vcard.Property.Id.ORG), newAddress);
                }
                if (card.getProperty(net.fortuna.ical4j.vcard.Property.Id.BDAY) != null) {
                    setBirth(card.getProperty(net.fortuna.ical4j.vcard.Property.Id.BDAY), newAddress);
                }
                if (card.getProperty(net.fortuna.ical4j.vcard.Property.Id.NOTE) != null) {
                    setNote(card.getProperty(net.fortuna.ical4j.vcard.Property.Id.NOTE), newAddress);
                }

                // //// SET ADDITIONAL DATA
                final List < Property > li = card . getProperties ();
                setProperties(li, newAddress);

                // handle item entries
                final VCardItemElementHandler ih = new VCardItemElementHandler (new FileInputStream (file));
                if (!ih.getItemList().isEmpty())
                    setProperties(ih.getItemList(), newAddress);

                newAddress.setAddressStatus(AddressStatus.UPTODATE);
                newAddress.setDeleted(false);
                newAddress.setLastUpdate(DateTime.now().toDate());
                newAddress.setCreated(DateTime.now().toDate());
                newAddress.setContactStatus(ContactStatus.ACTIVE);
                newAddress.setForm(FormOfAddress.UNKNOWN);

                // //// CHECK IF THERE IS SOMETHING MORE TO ADD
                if (newAddress.getAddressText() == null || newAddress.getAddressText() == "") {
                    setOtherPropertiesToWork(li, newAddress);
                    if (!ih.getItemList()
                            .isEmpty() && newAddress.getAddressText() == null || newAddress.getAddressText() == ""
                    ) {
                        setOtherPropertiesToWork(ih.getItemList(), newAddress);
                    }
                } else if (newAddress.getPrivateAddressText() == null || newAddress.getPrivateAddressText() == "") {
                    setOtherPropertiesToPrivate(li, newAddress);
                    if (!ih.getItemList()
                            .isEmpty() && newAddress.getPostalAddressText() == null || newAddress.getPostalAddressText() == ""
                    )
                        setOtherPropertiesToPrivate(ih.getItemList(), newAddress);
                } else {
                    setPostalProperties(li, newAddress);
                    if (!ih.getItemList()
                            .isEmpty() && newAddress.getPostalAddressText() == null || newAddress.getPostalAddressText() == ""
                    )
                        setPostalProperties(ih.getItemList(), newAddress);
                }
                newAddresses.add(newAddress);*/
        return address;
    }
}
