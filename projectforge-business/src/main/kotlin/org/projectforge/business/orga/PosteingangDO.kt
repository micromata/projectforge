/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import org.hibernate.search.annotations.Analyze
import org.hibernate.search.annotations.DateBridge
import org.hibernate.search.annotations.EncodingType
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Index
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.Resolution
import org.hibernate.search.annotations.Store
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO

import java.sql.Date

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table

/**
 * Posteingangsbuch
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_ORGA_POSTEINGANG",
        indexes = arrayOf(javax.persistence.Index(name = "idx_fk_t_orga_posteingang_tenant_id", columnList = "tenant_id")))
class PosteingangDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "date")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(nullable = false)
    var datum: Date? = null

    @PropertyInfo(i18nKey = "orga.posteingang.absender")
    @Field
    @get:Column(name = "absender", length = 1000, nullable = false)
    var absender: String? = null

    @PropertyInfo(i18nKey = "orga.posteingang.person")
    @Field
    @get:Column(name = "person", length = 1000)
    var person: String? = null

    @PropertyInfo(i18nKey = "orga.post.inhalt", required = true)
    @Field
    @get:Column(length = 1000)
    var inhalt: String? = null

    @PropertyInfo(i18nKey = "comment")
    @Field
    @get:Column(length = 4000)
    var bemerkung: String? = null

    @PropertyInfo(i18nKey = "orga.post.type")
    @Field(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "post_type", length = 20, nullable = false)
    var type: PostType? = null

    companion object {
        private val serialVersionUID = -4713747110526000256L
    }
}
