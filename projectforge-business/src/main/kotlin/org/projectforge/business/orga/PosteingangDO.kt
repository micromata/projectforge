/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.hibernate.search.annotations.*
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.sql.Date
import javax.persistence.*

/**
 * Posteingangsbuch
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_ORGA_POSTEINGANG",
        indexes = arrayOf(javax.persistence.Index(name = "idx_fk_t_orga_posteingang_tenant_id", columnList = "tenant_id")))
@NamedQueries(
        NamedQuery(name = PosteingangDO.SELECT_MIN_MAX_DATE, query = "select min(datum), max(datum) from PosteingangDO"))
open class PosteingangDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "date")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(nullable = false)
    open var datum: Date? = null

    @PropertyInfo(i18nKey = "orga.posteingang.absender")
    @Field
    @get:Column(name = "absender", length = 1000, nullable = false)
    open var absender: String? = null

    @PropertyInfo(i18nKey = "orga.posteingang.person")
    @Field
    @get:Column(name = "person", length = 1000)
    open var person: String? = null

    @PropertyInfo(i18nKey = "orga.post.inhalt", required = true)
    @Field
    @get:Column(length = 1000)
    open var inhalt: String? = null

    @PropertyInfo(i18nKey = "comment")
    @Field
    @get:Column(length = 4000)
    open var bemerkung: String? = null

    @PropertyInfo(i18nKey = "orga.post.type")
    @Field(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "post_type", length = 20, nullable = false)
    open var type: PostType? = null

    companion object {
        internal const val SELECT_MIN_MAX_DATE = "PosteingangDO_SelectMinMaxDate"
    }
}
