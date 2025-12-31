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

package org.projectforge.plugins.datatransfer

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.PfCaches
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.user.service.UserService
import org.projectforge.common.DataSizeConfig
import org.projectforge.common.StringHelper
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.configuration.ConfigurationChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.AttachmentsEventListener
import org.projectforge.framework.jcr.AttachmentsEventType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.jcr.FileInfo
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.RestResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * This is the base data access object class. Most functionality such as access checking, select, insert, update, save,
 * delete etc. is implemented by the super class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class DataTransferAreaDao : BaseDao<DataTransferAreaDO>(DataTransferAreaDO::class.java), AttachmentsEventListener {
    @Autowired
    private lateinit var configurationChecker: ConfigurationChecker

    @Autowired
    private lateinit var dataTransferAuditDao: DataTransferAuditDao

    @Autowired
    private lateinit var dataTransferNotificationMailService: DataTransferNotificationMailService

    @Autowired
    private lateinit var userService: UserService

    @Value("\${${MAX_FILE_SIZE_SPRING_PROPERTY}:100MB}")
    internal open lateinit var maxFileSizeConfig: String

    @PostConstruct
    private fun postConstruct() {
        globalMaxFileSize = DataSizeConfig.init(maxFileSizeConfig, DataUnit.MEGABYTES)
        log.info { "Maximum configured size of uploads: ${MAX_FILE_SIZE_SPRING_PROPERTY}=$maxFileSizeConfig." }
        dataTransferNotificationMailService.dataTransferAreaDao = this
        dataTransferAuditDao.dataTransferAreaDao = this
    }

    @Autowired
    private lateinit var domainService: DomainService

    open fun createInitializedFile(): DataTransferAreaDO {
        val file = DataTransferAreaDO()
        file.adminIds = "${ThreadLocalUserContext.loggedInUserId}"
        file.observerIds = file.adminIds
        file.externalAccessToken = generateExternalAccessToken()
        file.externalPassword = generateExternalPassword()
        file.expiryDays = 7
        file.maxUploadSizeKB = MAX_UPLOAD_SIZE_DEFAULT_VALUE_KB // 100MB
        return file
    }

    override fun onUpdate(obj: DataTransferAreaDO, dbObj: DataTransferAreaDO) {
        if (dbObj.isPersonalBox()) {
            if (obj.adminIds != dbObj.adminIds || obj.areaName != dbObj.areaName) {
                throw IllegalArgumentException("Can't modify personal boxes: $obj")
            }
            securePersonalBox(obj)
        }
        ensureSecureExternalAccess(obj)
    }

    override fun onInsert(obj: DataTransferAreaDO) {
        if (obj.isPersonalBox()) {
            if (obj.modifyPersonalBox != true) {
                // Prevent from saving or changing personal boxes.
                throw IllegalArgumentException("Can't save or update personal boxes.")
            }
            securePersonalBox(obj)
        }
        ensureSecureExternalAccess(obj)
    }

    /**
     * Removes personal boxes of other users in result list.
     */
    override fun select(
        filter: QueryFilter,
        customResultFilters: List<CustomResultFilter<DataTransferAreaDO>>?,
        checkAccess: Boolean,
    ): List<DataTransferAreaDO> {
        val loggedInUserId = ThreadLocalUserContext.loggedInUserId
        // Don't search for personal boxes of other users (they will be added afterwards):
        filter.add(
            DBPredicate.Or(
                // Either not a personal box,
                DBPredicate.NotEqual("areaName", DataTransferAreaDO.PERSONAL_BOX_AREA_NAME),
                // or the personal box of the logged-in user:
                DBPredicate.Equal("adminIds", loggedInUserId.toString()),
            )
        )
        var result = super.select(filter, customResultFilters, checkAccess)
        // searchString contains trailing %:
        val searchString = filter.fulltextSearchString?.replace("%", "")
        if (searchString == null || searchString.length < 2) { // Search string is given and has at least 2 chars:
            return result
        }
        result = result.toMutableList()
        userService.sortedUsers.filter { user ->
            user.username?.contains(searchString, ignoreCase = true) == true ||
                    user.getFullname().contains(searchString, ignoreCase = true)
        }.forEach { user ->
            // User name matches given string, so add personal box of this active user:
            val personalBox = ensurePersonalBox(user.id!!)!!
            result.add(personalBox)
        }
        return result
    }

    /**
     * Prevents changing some base values due to security reasons (such as don't allow external access and access to
     * other users/groups).
     * Sets also default values (expiry days to 30, and max upload size to max value).
     */
    private fun securePersonalBox(obj: DataTransferAreaDO) {
        // No external access to personal boxed (due to security reasons)
        obj.observerIds = obj.adminIds // Owner is always observer
        obj.accessGroupIds = null
        obj.accessUserIds = null
        obj.externalDownloadEnabled = false
        obj.externalUploadEnabled = false
        obj.externalPassword = null
        obj.externalAccessToken = null
        obj.expiryDays = 60 // 60 days as standard (if user is on holiday etc.)
        val springServletMultipartMaxFileSize = configurationChecker.springServletMultipartMaxFileSize.toBytes()
        obj.maxUploadSizeKB = (springServletMultipartMaxFileSize / 1024).toInt()
    }

    override fun afterLoad(obj: DataTransferAreaDO) {
        if (obj.maxUploadSizeKB == null)
            obj.maxUploadSizeKB = MAX_UPLOAD_SIZE_DEFAULT_VALUE_KB
    }

    open fun ensurePersonalBox(userId: Long): DataTransferAreaDO? {
        userGroupCache.getUser(userId) ?: return null
        var dbo = persistenceService.selectNamedSingleResult(
            DataTransferAreaDO.FIND_PERSONAL_BOX,
            DataTransferAreaDO::class.java,
            Pair("areaName", DataTransferAreaDO.PERSONAL_BOX_AREA_NAME),
            Pair("adminIds", "$userId"),
        )
        if (dbo != null) {
            securePersonalBox(dbo)
            update(dbo, checkAccess = false)
            return dbo
        }
        dbo = DataTransferAreaDO()
        dbo.areaName = DataTransferAreaDO.PERSONAL_BOX_AREA_NAME
        dbo.adminIds = "$userId"
        dbo.observerIds = "$userId"
        dbo.modifyPersonalBox = true
        insert(dbo, checkAccess = false)
        return dbo
    }

    open fun getAnonymousArea(externalAccessToken: String?): DataTransferAreaDO? {
        if (externalAccessToken?.length ?: 0 < ACCESS_TOKEN_LENGTH) {
            throw IllegalArgumentException("externalAccessToken to short.")
        }
        val dbo = persistenceService.selectNamedSingleResult(
            DataTransferAreaDO.FIND_BY_EXTERNAL_ACCESS_TOKEN,
            DataTransferAreaDO::class.java,
            Pair("externalAccessToken", externalAccessToken),
        ) ?: return null
        return dbo
    }

    open fun getExternalBaseLinkUrl(): String {
        return domainService.getDomain("${RestResolver.REACT_PUBLIC_PATH}/datatransfer/dynamic/")
    }

    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true // Select access in general for all registered users
    }

    override fun hasAccess(
        user: PFUserDO,
        obj: DataTransferAreaDO?,
        oldObj: DataTransferAreaDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        obj ?: return false
        if (obj.isPersonalBox() == true) {
            if (operationType != OperationType.SELECT && throwException) {
                // Select only on inboxes:
                throw AccessException(user, "access.exception.userHasNotRight")
            }
            // Select only on inboxes:
            return operationType == OperationType.SELECT
        }
        val adminIds = StringHelper.splitToLongs(obj.adminIds, ",")
        if (adminIds?.contains(user.id!!) == true) {
            return true
        }
        if (operationType == OperationType.SELECT) {
            //em.detach(obj)
            // Delete them due to security reasons:
            obj.externalAccessToken = null
            obj.externalPassword = null
            obj.externalAccessLogs = null
            // Select access also for those users:
            StringHelper.splitToLongs(obj.accessUserIds, ",")?.let {
                if (it.contains(user.id!!)) {
                    return true
                }
            }
            StringHelper.splitToLongs(obj.accessGroupIds, ",")?.let {
                if (userGroupCache.isUserMemberOfAtLeastOneGroup(user.id, *it.toTypedArray())) {
                    return true
                }
            }
        }
        if (throwException) {
            throw AccessException(user, "access.exception.userHasNotRight")
        }
        return false
    }

    override val defaultSortProperties: Array<SortProperty>?
        get() = arrayOf(SortProperty.desc("lastUpdate"))

    override fun newInstance(): DataTransferAreaDO {
        return DataTransferAreaDO()
    }

    override fun onAttachmentEvent(
        event: AttachmentsEventType,
        file: FileInfo,
        data: Any?,
        byUser: PFUserDO?,
        byExternalUser: String?
    ) {
        check(data != null)
        try {
            if (file.encryptionInProgress == true) {
                // Don't notificate observers if one user encrypts a file.
                return
            }
            dataTransferAuditDao.insertAudit(event, data as DataTransferAreaDO, byUser, byExternalUser, file)

        } catch (ex: Exception) {
            log.error("Exception while calling SendMailService: ${ex.message}.", ex)
        }
    }

    companion object {
        /**
         * No data transfer area may have more than this global max file size.
         */
        lateinit var globalMaxFileSize: DataSize
            internal set

        fun generateExternalAccessToken(): String {
            return NumberHelper.getSecureRandomAlphanumeric(ACCESS_TOKEN_LENGTH)
        }

        fun generateExternalPassword(): String {
            return NumberHelper.getSecureRandomReducedAlphanumericWithSpecialChars(PASSWORD_LENGTH)
        }

        fun ensureSecureExternalAccess(obj: IDataTransferArea) {
            if (obj.externalDownloadEnabled == true || obj.externalUploadEnabled == true) {
                if ((obj.externalAccessToken?.length ?: 0) < ACCESS_TOKEN_LENGTH) {
                    obj.externalAccessToken = generateExternalAccessToken()
                }
                if (obj.externalPassword.isNullOrBlank()) {
                    obj.externalPassword = generateExternalPassword()
                }
            }
        }

        /**
         * @return Maximum file size in kb: The minimum value of free area capacity and configured max-upload-file-size.
         */
        fun calculateMaxUploadFileSize(data: DataTransferAreaDO): Long {
            val used = data.attachmentsSize ?: 0
            val freeCapacity = data.capacity - used
            return minOf(freeCapacity, getMaxUploadFileSizeKB(data) * 1024L)
        }

        fun getMaxUploadFileSizeKB(data: DataTransferAreaDO): Int {
            return data.maxUploadSizeKB ?: MAX_UPLOAD_SIZE_DEFAULT_VALUE_KB
        }

        const val MAX_FILE_SIZE_SPRING_PROPERTY = "projectforge.plugin.datatransfer.maxFileSize"
        val EXPIRY_DAYS_VALUES = "duration.days".let {
            mapOf(
                1 to "$it.one",
                3 to it,
                7 to it,
                14 to it,
                30 to it,
                60 to it,
                90 to it,
                180 to it,
                365 to it
            )
        }
        private const val MB = 1024
        private const val GB = 1024 * MB

        val MAX_UPLOAD_SIZE_VALUES =
            arrayOf(20 * MB, 50 * MB, 100 * MB, 200 * MB, 500 * MB, GB, 1536 * MB, 2 * GB, 3 * GB, 5 * GB, 10 * GB)

        const val MAX_UPLOAD_SIZE_DEFAULT_VALUE_KB = 100 * MB

        const val ACCESS_TOKEN_LENGTH = 30
        const val PASSWORD_LENGTH = 10

        /**
         * External (anonymous user are marked by this prefix), needed for internationalization.
         */
        const val EXTERNAL_USER_PREFIX = "#EXTERNAL#:"


        internal fun getExternalUserString(request: HttpServletRequest, userString: String?): String {
            return "${EXTERNAL_USER_PREFIX}${RestUtils.getClientIp(request)} ('${userString?.take(255) ?: "???"}')"
        }

        internal fun getTranslatedUserString(user: PFUserDO?, externalUser: String?, locale: Locale? = null): String {
            if (user != null) {
                return PfCaches.instance.getUserIfNotInitialized(user)?.getFullname() ?: "???"
            }
            if (externalUser != null) {
                if (externalUser.startsWith(EXTERNAL_USER_PREFIX)) {
                    val marker = translate(locale, "plugins.datatransfer.external.userPrefix")
                    return "${externalUser.removePrefix(EXTERNAL_USER_PREFIX)}, $marker"
                } else {
                    return externalUser // Shouldn't occur (only, if EXTERNAL_USER_PREFIX was changed).
                }
            }
            return ""
        }
    }
}
