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

package org.projectforge.plugins.banking

import de.micromata.genome.db.jpa.history.api.WithHistory
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import javax.persistence.*

@Entity
@Indexed
@Table(name = "T_PLUGIN_BANK_ACCOUNT", uniqueConstraints = [UniqueConstraint(columnNames = ["account_number", "tenant_id"])], indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_bank_account_tenant_id", columnList = "tenant_id")])
@WithHistory
open class BankAccountDO : DefaultBaseDO(), ShortDisplayNameCapable {

    @Field
    @get:Column(name = "account_number", length = 255, nullable = false)
    open var accountNumber: String? = null

    @Field
    @get:Column(length = 255)
    open var bank: String? = null

    @Field
    @get:Column(name = "bank_identification_code", length = 100)
    open var bankIdentificationCode: String? = null

    @Field
    @get:Column(length = 255)
    open var name: String? = null

    @Field
    @get:Column(length = 4000)
    open var description: String? = null

    @Transient
    override fun getShortDisplayName(): String {
        return accountNumber.toString()
    }
}
