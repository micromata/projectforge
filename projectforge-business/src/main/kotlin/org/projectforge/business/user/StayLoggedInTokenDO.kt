/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.user

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedQueries
import jakarta.persistence.NamedQuery
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.Date

/**
 * One stay-logged-in token per device (browser profile), so a logout can invalidate the calling device
 * without logging the user's other devices out.
 *
 * Deliberately **not** a [org.projectforge.framework.persistence.entities.DefaultBaseDO] (and no
 * [org.projectforge.framework.persistence.api.BaseDao]), modelled on
 * [org.projectforge.security.webauthn.WebAuthnEntryDO] instead:
 * - These rows are credentials with a high churn (one per login, deleted on logout, bulk deleted on a
 *   password change). Being historized would write a history entry per operation - and the history
 *   payload would contain the token hash.
 * - An invalidated token has to be **gone**; a `deleted` flag that still matches a lookup is a footgun.
 * - The lookup happens *before* the login, so there is no logged-in user for a `BaseDao` access check.
 *
 * [tokenHash] is a SHA-256 of the token, not the token itself and not reversibly encrypted: the value in
 * the cookie must not be reconstructable from a database dump. It is globally unique, so the token alone
 * identifies the device - nothing else in the cookie has to be trusted.
 */
@Entity
@Table(
    name = "T_PF_USER_STAY_LOGGED_IN",
    uniqueConstraints = [UniqueConstraint(columnNames = ["token_hash"])],
    indexes = [Index(name = "idx_fk_t_pf_user_stay_logged_in_user", columnList = "user_fk")],
)
@NamedQueries(
    NamedQuery(
        name = StayLoggedInTokenDO.FIND_BY_TOKEN_HASH,
        query = "from StayLoggedInTokenDO t join fetch t.user where t.tokenHash = :tokenHash",
    ),
    NamedQuery(
        name = StayLoggedInTokenDO.FIND_BY_USER,
        query = "from StayLoggedInTokenDO t where t.user.id = :userId order by t.lastAccess desc",
    ),
    NamedQuery(
        name = StayLoggedInTokenDO.UPDATE_LAST_ACCESS,
        query = "update StayLoggedInTokenDO t set t.lastAccess = :lastAccess, t.lastAccessIp = :lastAccessIp, t.userAgent = :userAgent where t.id = :id",
    ),
    NamedQuery(
        name = StayLoggedInTokenDO.DELETE_BY_TOKEN_HASH,
        query = "delete from StayLoggedInTokenDO t where t.tokenHash = :tokenHash",
    ),
    NamedQuery(
        name = StayLoggedInTokenDO.DELETE_BY_USER,
        query = "delete from StayLoggedInTokenDO t where t.user.id = :userId",
    ),
    NamedQuery(
        name = StayLoggedInTokenDO.DELETE_EXPIRED,
        query = "delete from StayLoggedInTokenDO t where t.lastAccess < :expireBefore",
    ),
)
open class StayLoggedInTokenDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var user: PFUserDO? = null

    /**
     * Lowercase hex SHA-256 of the token, see [StayLoggedInTokenDao.hash]. Unsalted on purpose: the token
     * is high entropy random, so there is no dictionary attack to protect against, and a per row salt
     * would turn the lookup into a table scan.
     */
    @get:Column(name = "token_hash", length = 64, nullable = false)
    open var tokenHash: String? = null

    /**
     * When this device logged in.
     */
    @get:Column(nullable = false)
    open var created: Date? = null

    /**
     * Last successful restore, the anchor of the sliding expiry (see
     * [StayLoggedInTokenDao.getValidToken]). Not written on every request, see
     * [StayLoggedInTokenDao.updateLastAccessIfDue].
     */
    @get:Column(name = "last_access")
    open var lastAccess: Date? = null

    /**
     * For displaying the device to its owner only, never a criterion of the check: it is taken from
     * [org.projectforge.web.WebUtils.getClientIp], which trusts `X-Forwarded-For`.
     */
    @get:Column(name = "last_access_ip", length = 50)
    open var lastAccessIp: String? = null

    /**
     * For displaying the device to its owner only, never a criterion of the check (a user agent is trivially
     * forged). Truncated on write, see [StayLoggedInTokenDao].
     */
    @get:Column(name = "user_agent", length = 255)
    open var userAgent: String? = null

    companion object {
        internal const val FIND_BY_TOKEN_HASH = "StayLoggedInTokenDO_FindByTokenHash"
        internal const val FIND_BY_USER = "StayLoggedInTokenDO_FindByUser"
        internal const val UPDATE_LAST_ACCESS = "StayLoggedInTokenDO_UpdateLastAccess"
        internal const val DELETE_BY_TOKEN_HASH = "StayLoggedInTokenDO_DeleteByTokenHash"
        internal const val DELETE_BY_USER = "StayLoggedInTokenDO_DeleteByUser"
        internal const val DELETE_EXPIRED = "StayLoggedInTokenDO_DeleteExpired"

        const val MAX_USER_AGENT_LENGTH = 255
        const val MAX_IP_LENGTH = 50
    }
}
