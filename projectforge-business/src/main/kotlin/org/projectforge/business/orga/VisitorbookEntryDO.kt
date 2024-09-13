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

package org.projectforge.business.orga

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import jakarta.persistence.*
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import java.io.Serializable
import java.time.LocalDate

/**
 * Represents timeable attributes of an employee (annual leave days and status).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Table(
    name = "t_orga_visitorbook_entry",
    indexes = [Index(
        name = "idx_fk_t_orga_visitorbook_val_per_employee_id", columnList = "visitorbook_id"
    )]
)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
open class VisitorbookEntryDO : Serializable, AbstractBaseDO<Int>() {
    @get:Id
    @get:GeneratedValue
    @get:Column(name = "pk")
    override var id: Int? = null

    @PropertyInfo(i18nKey = "orga.visitorbook")
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "visitorbook_fk", nullable = false)
    open var visitorbook: VisitorbookDO? = null

    @get:Column(name = "date_of_visit", nullable = false)
    open var dateOfVisit: LocalDate? = null

    @get:Column(name = "arrived", length = 100)
    open var arrived: String? = null

    @get:Column(name = "departed", length = 100)
    open var departed: String? = null

    @get:Column(name = "comment", length = Constants.LENGTH_TEXT)
    open var comment: String? = null
}
