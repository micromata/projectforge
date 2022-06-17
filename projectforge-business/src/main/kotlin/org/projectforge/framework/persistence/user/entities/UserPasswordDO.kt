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

package org.projectforge.framework.persistence.user.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import de.micromata.genome.db.jpa.history.api.NoHistory
import mu.KotlinLogging
import org.hibernate.search.util.AnalyzerUtils.log
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import javax.persistence.*

private val log = KotlinLogging.logger {}

/**
 * Users may have serveral authentication tokens, e. g. for CardDAV/CalDAV-Clients or other clients. ProjectForge shows the usage of this tokens and such tokens
 * may easily be revokable. In addition, no password may be stored on smartphone client e. g. for using ProjectForge's CardDAV/CalDAV service.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Table(
  name = "T_PF_USER_PASSWORD",
  uniqueConstraints = [UniqueConstraint(columnNames = ["user_id"])],
  indexes = [Index(name = "idx_fk_t_pf_user_id", columnList = "user_id")]
)
@NamedQueries(
  NamedQuery(
    name = UserPasswordDO.FIND_BY_USER_ID,
    query = "from UserPasswordDO t join fetch t.user where t.user.id = :userId"
  ),
)
open class UserPasswordDO : DefaultBaseDO() {
  @PropertyInfo(i18nKey = "user")
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "user_id")
  open var user: PFUserDO? = null

  /**
   * Encoded password of the user (SHA-256).
   */
  @JsonIgnore
  @field:NoHistory
  @get:Column(name="password_hash", length = 255)
  open var passwordHash: String? = null

  /**
   * The saltString for giving salt to hashed password.
   */
  @JsonIgnore
  @field:NoHistory
  @get:Column(name = "password_salt", length = 40)
  open var passwordSalt: String? = null

  /**
   * If password is not given as "SHA{..." then it will be set to null due to security reasons.
   */
  fun checkAndFixPassword() {
    val pw = this.passwordHash
    if (!pw.isNullOrEmpty() &&
      !pw.startsWith("SHA{") &&
      (pw != NOPASSWORD)
    ) {
      this.passwordHash = null
      log.error("Password for user '${this.user?.id}' is not given SHA encrypted. Ignoring it.")
    }
  }

  /**
   * @return this for chaining.
   */
  fun setNoPassword() {
    this.passwordHash = NOPASSWORD
  }

  companion object {
    internal const val FIND_BY_USER_ID = "UserPasswordDO_FindByUserId"

    private const val NOPASSWORD = "--- none ---"
  }
}
