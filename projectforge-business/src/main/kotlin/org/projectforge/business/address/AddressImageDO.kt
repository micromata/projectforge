/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.genome.jpa.DbRecord
import org.projectforge.business.fibu.EingangsrechnungDO
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@NamedQueries(
        NamedQuery(name = AddressImageDO.SELECT_IMAGE, query = "select image from AddressImageDO where address.id = :addressId"),
        NamedQuery(name = AddressImageDO.SELECT_IMAGE_PREVIEW, query = "select imagePreview from AddressImageDO where address.id = :addressId"))
@Entity
@Table(name = "T_ADDRESS_IMAGE")
open class AddressImageDO : DbRecord<Int> {
    @get:Id
    @get:GeneratedValue
    @get:Column(name = "pk")
    open var id: Int? = null

    @get:OneToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "address_fk", nullable = false)
    open var address: AddressDO? = null

    @get:Column
    @get:Basic(fetch = FetchType.LAZY)
    open var image: ByteArray? = null

    @get:Column(name = "image_preview")
    @get:Basic(fetch = FetchType.LAZY)
    open var imagePreview: ByteArray? = null

    @javax.persistence.Transient
    override fun getPk(): Int? {
        return id
    }

    override fun setPk(pk: Int?) {
        id = pk
    }

    companion object {
        internal const val SELECT_IMAGE = "AddressImageDO.selectImage"
        internal const val SELECT_IMAGE_PREVIEW = "AddressImageDO.selectImagePreview"
    }
}
