/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.bouncycastle.asn1.x500.style.RFC4519Style.title
import org.hibernate.search.annotations.*
import org.projectforge.Constants
import org.projectforge.business.common.BaseUserGroupRightsDO
import org.projectforge.business.teamcal.admin.model.HibernateSearchUsersGroupsBridge
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.props.PropertyType
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.persistence.user.entities.PFUserDO
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.Transient

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@ClassBridge(name = "usersgroups", index = Index.YES, store = Store.NO, impl = HibernateSearchUsersGroupsBridge::class)
@Table(
  name = "T_PLUGIN_BANKING_ACCOUNT",
)
open class BankAccountDO : BaseUserGroupRightsDO(), DisplayNameCapable {
  override val displayName: String
    @Transient
    get() {
      val sb = StringBuilder()
      iban?.let {
        if (it.isNotBlank()) {
          sb.append(it).append(" ")
        }
      }
      sb.append(title)
      return sb.toString()
    }


  /**
   * Unused field
   */
  @get:Transient
  override var owner: PFUserDO? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.name", type = PropertyType.INPUT)
  @Field
  @get:Column(length = Constants.LENGTH_TITLE, nullable = false)
  open var name: String? = null

  @PropertyInfo(i18nKey = "description")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT)
  open var description: String? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.iban", type = PropertyType.INPUT)
  @Field
  @get:Column(length = Constants.LENGTH_TITLE)
  open var iban: String? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.bic", type = PropertyType.INPUT)
  @Field
  @get:Column(length = Constants.LENGTH_TITLE)
  open var bic: String? = null

  @PropertyInfo(i18nKey = "plugins.banking.account.bank", type = PropertyType.INPUT)
  @Field
  @get:Column(length = Constants.LENGTH_TITLE, nullable = false)
  open var bank: String? = null

  /**
   *
   */
  @PropertyInfo(i18nKey = "plugins.banking.account.importSettings")
  @Field
  @get:Column(length = 10000, name = "import_settings")
  open var importSettings: String? = null
}
