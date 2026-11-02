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
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
 * One row per installed app (the ProjectForge mobile client), holding the hash of the token the app sends with
 * every request (`Authorization: PFApp <token>`, see [AppDeviceDao]).
 *
 * Modelled on [StayLoggedInTokenDO] for the same reasons: credentials with churn (no history, no `deleted` flag, no
 * [org.projectforge.framework.persistence.api.BaseDao]), looked up before there is a logged-in user.
 *
 * Unlike a stay-logged-in token, the app carries the time of its last second factor ([last2FA]): a token is issued
 * only after a fresh 2FA in the browser (see [AppAuthService]), and the 2FA protected urls are checked against it.
 */
@Entity
@Table(
    name = "T_PF_USER_APP_DEVICE",
    uniqueConstraints = [UniqueConstraint(columnNames = ["token_hash"])],
    indexes = [Index(name = "idx_fk_t_pf_user_app_device_user", columnList = "user_fk")],
)
@NamedQueries(
    NamedQuery(
        name = AppDeviceDO.FIND_BY_TOKEN_HASH,
        query = "from AppDeviceDO t join fetch t.user where t.tokenHash = :tokenHash",
    ),
    NamedQuery(
        name = AppDeviceDO.FIND_BY_USER,
        query = "from AppDeviceDO t where t.user.id = :userId order by t.lastAccess desc",
    ),
    NamedQuery(
        name = AppDeviceDO.FIND_BY_ID_AND_USER,
        query = "from AppDeviceDO t where t.id = :id and t.user.id = :userId",
    ),
    NamedQuery(
        name = AppDeviceDO.UPDATE_LAST_ACCESS,
        query = "update AppDeviceDO t set t.lastAccess = :lastAccess, t.lastAccessIp = :lastAccessIp, t.appVersion = :appVersion where t.id = :id",
    ),
    NamedQuery(
        name = AppDeviceDO.FIND_PUSH_BY_USER,
        query = "from AppDeviceDO t where t.user.id = :userId and t.pushToken is not null and t.lastAccess >= :expireBefore",
    ),
    NamedQuery(
        name = AppDeviceDO.CLEAR_PUSH_TOKEN,
        query = "update AppDeviceDO t set t.pushToken = null, t.pushEnvironment = null where t.pushToken = :pushToken and t.id <> :exceptId",
    ),
    NamedQuery(
        name = AppDeviceDO.DELETE_BY_USER,
        query = "delete from AppDeviceDO t where t.user.id = :userId",
    ),
    NamedQuery(
        name = AppDeviceDO.DELETE_BY_ID_AND_USER,
        query = "delete from AppDeviceDO t where t.id = :id and t.user.id = :userId",
    ),
    NamedQuery(
        name = AppDeviceDO.DELETE_EXPIRED,
        // "is null": see StayLoggedInTokenDO.DELETE_EXPIRED.
        query = "delete from AppDeviceDO t where t.lastAccess < :expireBefore or t.lastAccess is null",
    ),
)
open class AppDeviceDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var user: PFUserDO? = null

    /**
     * Lowercase hex SHA-256 of the token, see [StayLoggedInTokenDO.tokenHash] (same reasoning).
     */
    @get:Column(name = "token_hash", length = 64, nullable = false)
    open var tokenHash: String? = null

    /**
     * Name of the device as the app reports it ("iPhone of …"), for displaying the device to its owner only.
     */
    @get:Column(name = "device_name", length = MAX_DEVICE_NAME_LENGTH)
    open var deviceName: String? = null

    /**
     * `ios` or `android` as the app reports it, for display only.
     */
    @get:Column(length = MAX_PLATFORM_LENGTH)
    open var platform: String? = null

    /**
     * Version of the app (header `X-PF-App-Version`), for display only.
     */
    @get:Column(name = "app_version", length = MAX_APP_VERSION_LENGTH)
    open var appVersion: String? = null

    @get:Column(nullable = false)
    open var created: Date? = null

    /**
     * Anchor of the sliding expiry, see [AppDeviceDao.getValidDevice].
     */
    @get:Column(name = "last_access")
    open var lastAccess: Date? = null

    /**
     * For displaying the device to its owner only, see [StayLoggedInTokenDO.lastAccessIp].
     */
    @get:Column(name = "last_access_ip", length = StayLoggedInTokenDO.MAX_IP_LENGTH)
    open var lastAccessIp: String? = null

    /**
     * The last second factor of the user done for this device (when the token was issued or renewed in the browser).
     * Taken as [org.projectforge.framework.persistence.user.api.UserContext.lastSuccessful2FA] of every request.
     */
    @get:Column(name = "last_2fa")
    open var last2FA: Date? = null

    /**
     * The APNs device token of the app (hex), null if it has none (no permission, push not set up). Set by the app
     * itself ([AppDeviceDao.setPushToken]), used by the push sender only; never sent back to a client.
     */
    @get:Column(name = "push_token", length = MAX_PUSH_TOKEN_LENGTH)
    open var pushToken: String? = null

    /**
     * Which APNs endpoint [pushToken] belongs to: a token sent to the wrong one fails with `BadDeviceToken`.
     */
    @get:Column(name = "push_environment", length = MAX_PLATFORM_LENGTH)
    @get:Enumerated(EnumType.STRING)
    open var pushEnvironment: PushEnvironment? = null

    companion object {
        internal const val FIND_PUSH_BY_USER = "AppDeviceDO_FindPushByUser"
        internal const val CLEAR_PUSH_TOKEN = "AppDeviceDO_ClearPushToken"
        internal const val FIND_BY_TOKEN_HASH = "AppDeviceDO_FindByTokenHash"
        internal const val FIND_BY_USER = "AppDeviceDO_FindByUser"
        internal const val FIND_BY_ID_AND_USER = "AppDeviceDO_FindByIdAndUser"
        internal const val UPDATE_LAST_ACCESS = "AppDeviceDO_UpdateLastAccess"
        internal const val DELETE_BY_USER = "AppDeviceDO_DeleteByUser"
        internal const val DELETE_BY_ID_AND_USER = "AppDeviceDO_DeleteByIdAndUser"
        internal const val DELETE_EXPIRED = "AppDeviceDO_DeleteExpired"

        const val MAX_DEVICE_NAME_LENGTH = 100
        const val MAX_PLATFORM_LENGTH = 20
        const val MAX_APP_VERSION_LENGTH = 50
        const val MAX_PUSH_TOKEN_LENGTH = 200
    }
}

/** The APNs endpoint a push token belongs to (debug builds of the app use the sandbox). */
enum class PushEnvironment { SANDBOX, PRODUCTION }
