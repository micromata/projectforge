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

package org.projectforge.business.meb

import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.AbstractBaseDO

import java.util.Date

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.UniqueConstraint

/**
 * All imported meb entries (by mail or by SMS servlet) will be registered as imported MEB entry for avoiding multiple imports of the same
 * messages.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Table(name = "T_IMPORTED_MEB_ENTRY", uniqueConstraints = [UniqueConstraint(columnNames = ["sender", "date", "check_sum"])], indexes = [javax.persistence.Index(name = "idx_fk_t_imported_meb_entry_tenant_id", columnList = "tenant_id")])
class ImportedMebEntryDO : AbstractBaseDO<Int>() {

    private var id: Int? = null

    @PropertyInfo(i18nKey = "meb.sender")
    @get:Column(length = 255, nullable = false)
    var sender: String? = null

    /**
     * Only the check sum of an entry is registered for protecting privacy.
     */
    @get:Column(name = "check_sum", length = 255, nullable = false)
    var checkSum: String? = null

    @PropertyInfo(i18nKey = "date")
    @get:Column(nullable = false)
    var date: Date? = null

    /**
     * From which source was this entry imported (MAIL or SERVLET)?
     */
    @get:Column(length = 10)
    var source: String? = null

    @Id
    @GeneratedValue
    @Column(name = "pk")
    override fun getId(): Int? {
        return id
    }

    override fun setId(id: Int?) {
        this.id = id
    }
}
