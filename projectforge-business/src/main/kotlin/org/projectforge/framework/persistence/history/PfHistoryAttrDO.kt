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

package org.projectforge.framework.persistence.history

import jakarta.persistence.*
import mu.KotlinLogging
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Stores history attributes.
 *
 * Table t_pf_history_attr
 *  withdata            | character(1)                |           | not null | -- 0, 1
 *  pk                  | bigint                      |           | not null |
 *  createdat           | timestamp without time zone |           | not null | -- equals to modifiedat and parent.modifiedat
 *  createdby           | character varying(60)       |           | not null | -- equals to modifiedby and parent.modifiedby
 *  modifiedat          | timestamp without time zone |           | not null | -- equals to createdat and parent.createdat
 *  modifiedby          | character varying(60)       |           | not null | -- equals to createdby and parent.createdby
 *  updatecounter       | integer                     |           | not null | -- always 0
 *  value               | character varying(3000)     |           |          |
 *  propertyname        | character varying(255)      |           |          |
 *  type                | character(1)                |           | not null | N, V
 *  property_type_class | character varying(128)      |           |          |
 *  master_fk           | bigint                      |           | not null |
 *
 *  propertyname:
 *  - timeableAttributes.timeofvisit.2017-09-28 00:00:00:000.arrive:op
 *  - timeableAttributes.timeofvisit.2021-10-04 00:00:00:000.arrive:nv
 *  - timeableAttributes.timeofvisit.2022-09-20 00:00:00:000.startTime:op
 *  - attrs.previousyearleave:op
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Entity
@Table(
    name = "t_pf_history_attr",
    //indexes = [Index(
    //    name = "ix_pf_history_ent",
    //    columnList = "ENTITY_ID,ENTITY_NAME"
    //), Index(name = "ix_pf_history_mod", columnList = "MODIFIEDAT")]
)
@Indexed
//@ClassBridge(impl = HistoryMasterClassBridge::class)
class PfHistoryAttrDO {
    @get:GeneratedValue
    @get:Column(name = "pk")
    @get:Id
    var id: Long? = null

    @get:Column(name = "withdata")
    var withdata: String? = null

    /**
     * User id (same as modifiedBy and master.modifiedBy)
     */
    @get:Column(name = "createdby")
    var createdBy: String? = null

    /**
     * Same as modifiedAt and master.modifiedAt.
     */
    @get:Column(name = "createdat")
    var createdAt: Date? = null

    /**
     * User id (same as createdBy and master.createdBy)
     */
    @get:Column(name = "modifiedby")
    var modifiedBy: String? = null

    /**
     * Same as createdAt and master.createdAt.
     */
    @get:Column(name = "modifiedat")
    var modifiedAt: Date? = null

    @get:Column(name = "value")
    var value: String? = null

    @get:Column(name = "propertyname")
    var propertyName: String? = null

    /**
     * de.micromata.genome.util.strings.converter.ConvertedStringTypes
     * N (Null) or V (String)
     */
    @get:Column(name = "type")
    var type: String? = null

    @get:Column(name = "property_type_class")
    var propertyTypeClass: String? = null
}
