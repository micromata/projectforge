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

package org.projectforge.business.teamcal.event.model

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.annotations.Type
import org.hibernate.search.annotations.Indexed
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.mail.MailAttachment
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */

@Entity
@Indexed
@Table(name = "T_PLUGIN_CALENDAR_EVENT_ATTACHMENT", indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_attachment_team_event_fk2", columnList = "team_event_fk2"), javax.persistence.Index(name = "idx_fk_t_plugin_calendar_event_attachment_tenant_id", columnList = "tenant_id")])
open class TeamEventAttachmentDO : DefaultBaseDO(), Comparable<TeamEventAttachmentDO>, MailAttachment {

    private var filename: String? = null

    private var content: ByteArray? = null

    @Column
    override fun getFilename(): String? {
        return filename
    }

    open fun setFilename(filename: String): TeamEventAttachmentDO {
        this.filename = filename
        return this
    }

    @Column
    @Type(type = "binary")
    override fun getContent(): ByteArray? {
        return content
    }

    open fun setContent(content: ByteArray): TeamEventAttachmentDO {
        this.content = content
        return this
    }

    /**
     * @see java.lang.Comparable.compareTo
     */
    override fun compareTo(other: TeamEventAttachmentDO): Int {
        return if (id != null && this.id == other.id) {
            0
        } else this.toString().toLowerCase().compareTo(other.toString().toLowerCase())
    }

    /**
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(id)
        if (id != null) {
            return hcb.toHashCode()
        }
        hcb.append(this.filename)
        hcb.append(this.content)
        return hcb.toHashCode()
    }

    /**
     * @see java.lang.Object.equals
     */
    override fun equals(other: Any?): Boolean {
        if (!(other is TeamEventAttachmentDO)) {
            return false
        }
        val o = other as TeamEventAttachmentDO?
        if (id != null && this.id == o!!.id) {
            return true
        }
        if (!StringUtils.equals(this.getFilename(), o!!.getFilename())) {
            return false
        }
        return o.getContent()?.let { this.getContent()?.contentEquals(it) }!!
    }

    /**
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        return if (StringUtils.isBlank(filename)) {
            id.toString()
        } else StringUtils.defaultString(this.filename)
    }
}
