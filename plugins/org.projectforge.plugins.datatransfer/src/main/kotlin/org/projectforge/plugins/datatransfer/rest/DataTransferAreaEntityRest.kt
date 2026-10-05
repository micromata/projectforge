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

package org.projectforge.plugins.datatransfer.rest

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.configuration.ConfigurationChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.plugins.datatransfer.DataTransferAccessChecker
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.ui.UISelectValue
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the data transfer areas (list and admin form), serving the hand-built
 * projectforge-next page (see components/features/datatransfer). Replaces the retired
 * `DataTransferAreaEntityRest`, whose server-side `UILayout` moved onto the frontend. The file view of an area
 * is served by [DataTransferFilesRest].
 *
 * The category (`datatransfer`) is also the attachment category of the areas, so it must not change.
 */
@RestController
@RequestMapping("${Rest.URL}/datatransfer")
class DataTransferAreaEntityRest :
    AbstractDTOEntityRest<DataTransferAreaDO, DataTransferArea, DataTransferAreaDao>(
        DataTransferAreaDao::class.java,
        "plugins.datatransfer.title"
    ) {
    /**
     * The choices of the admin form and whether external access may be configured here or has to be
     * administered on the gateway instead (see [getOptions]).
     */
    class Options(
        val expiryDays: List<UISelectValue<Int>>,
        val maxUploadSizes: List<UISelectValue<Int>>,
        val externalAccessAllowed: Boolean,
        val gatewayHost: String,
    )

    @Autowired
    private lateinit var configurationChecker: ConfigurationChecker

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Value("\${projectforge.gateway.push.enabled:false}")
    private var gatewayPushEnabled: Boolean = false

    @Value("\${projectforge.gateway.push.url:}")
    private var gatewayPushUrl: String = ""

    /**
     * Whether areas with external access may be configured on this instance. Unset: only without a gateway
     * (see [externalAccessAllowed]).
     */
    @Value("\${projectforge.datatransfer.externalAccess.localAllowed:#{null}}")
    private var externalAccessLocalAllowed: Boolean? = null

    @PostConstruct
    private fun postConstruct() {
        enableJcr(
            attachmentsAccessChecker = DataTransferAccessChecker(baseDao)
        )
    }

    override fun transformForDB(dto: DataTransferArea): DataTransferAreaDO {
        val obj = DataTransferAreaDO()
        dto.copyTo(obj)
        return obj
    }

    override fun transformFromDB(obj: DataTransferAreaDO, editMode: Boolean): DataTransferArea {
        val dto = DataTransferArea.transformFromDB(obj, baseDao)
        // An access user never gets the password: the DAO's select access already removes it for them.
        if (editMode) {
            dto.externalPassword = obj.externalPassword
        }
        return dto
    }

    /**
     * Initializes new DataTransferFiles for adding.
     */
    override fun newBaseDO(request: HttpServletRequest?): DataTransferAreaDO {
        return baseDao.createInitializedFile()
    }

    /**
     * The choices of the admin form: the expiry days and the maximum upload sizes (in KB, limited by the global
     * maximum of Spring's multipart configuration).
     */
    @AccessChecked("No data: static choices and the gateway flag only")
    @GetMapping("options")
    fun getOptions(): Options {
        val globalMaxBytes = configurationChecker.springServletMultipartMaxFileSize.toBytes()
        return Options(
            expiryDays = DataTransferAreaDao.EXPIRY_DAYS_VALUES.map { UISelectValue(it.key, translateMsg(it.value, it.key)) },
            maxUploadSizes = DataTransferAreaDao.MAX_UPLOAD_SIZE_VALUES
                .filter { 1024L * it <= globalMaxBytes }
                .map { UISelectValue(it, FormatterUtils.formatBytes(1024L * it)) },
            externalAccessAllowed = externalAccessAllowed,
            gatewayHost = gatewayHost,
        )
    }

    /** A new random access token for the external link, stored on save only. */
    @AccessChecked("No data: returns a new random token, stored by the DAO-checked save only")
    @PostMapping("renewAccessToken")
    fun renewAccessToken(): Map<String, String> {
        return mapOf("externalAccessToken" to DataTransferAreaDao.generateExternalAccessToken())
    }

    /** A new random password for the external access, stored on save only. */
    @AccessChecked("No data: returns a new random password, stored by the DAO-checked save only")
    @PostMapping("renewPassword")
    fun renewPassword(): Map<String, String> {
        return mapOf("externalPassword" to DataTransferAreaDao.generateExternalPassword())
    }

    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter
    ): List<CustomResultFilter<DataTransferAreaDO>>? {
        source.sortProperties.find { it.property == "lastUpdateTimeAgo" }?.property = "lastUpdate"
        return super.preProcessMagicFilter(target, source)
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: DataTransferArea) {
        validateExternalAccessAllowed(validationErrors, dto)
        if (dto.externalAccessEnabled) {
            if (!NumberHelper.checkSecureRandomAlphanumeric(
                    dto.externalAccessToken,
                    DataTransferAreaDao.ACCESS_TOKEN_LENGTH
                )
            ) {
                validationErrors.add(
                    ValidationError(translate("plugins.datatransfer.validation.error.token"))
                )
            }
            if (dto.externalPassword?.trim()?.length ?: 0 < 6) {
                validationErrors.add(
                    ValidationError(
                        translate("plugins.datatransfer.validation.error.password"), fieldId = "externalPassword"
                    )
                )
            }
        }
        if (!DataTransferAreaDao.EXPIRY_DAYS_VALUES.containsKey(dto.expiryDays)) {
            validationErrors.add(
                ValidationError(
                    translate("plugins.datatransfer.validation.error.expiryDays"), fieldId = "expiryDays"
                )
            )
        }
        if (!DataTransferAreaDao.MAX_UPLOAD_SIZE_VALUES.contains(dto.maxUploadSizeKB)) {
            validationErrors.add(
                ValidationError(
                    translate("plugins.datatransfer.validation.error.maxUploadSizeKB"), fieldId = "maxUploadSizeKB"
                )
            )
        }
        dto.maxUploadSizeKB?.let {
            val springServletMultipartMaxFileSize = configurationChecker.springServletMultipartMaxFileSize.toBytes()
            if (1024L * it > springServletMultipartMaxFileSize) {
                validationErrors.add(
                    ValidationError(
                        translateMsg(
                            "plugins.datatransfer.validation.error.maxUploadSizeKB.exceededGlobalMaxUploadSize",
                            FormatterUtils.formatBytes(springServletMultipartMaxFileSize)
                        ), fieldId = "maxUploadSizeKB"
                    )
                )
            }
        }
        dto.observers?.let { observers ->
            if (observers.isNotEmpty()) {
                val adminIds = dto.admins?.mapNotNull { it.id }?.toSet() ?: emptySet()
                val accessUserIds = dto.accessUsers?.mapNotNull { it.id }?.toSet() ?: emptySet()
                val accessGroupIds = dto.accessGroups?.mapNotNull { it.id }?.toTypedArray() ?: emptyArray()
                val observersWithoutAccess = observers.filter { observer ->
                    val observerId = observer.id ?: return@filter false
                    observerId !in adminIds
                            && observerId !in accessUserIds
                            && (accessGroupIds.isEmpty() || !userGroupCache.isUserMemberOfAtLeastOneGroup(observerId, *accessGroupIds))
                }
                if (observersWithoutAccess.isNotEmpty()) {
                    val names = observersWithoutAccess.joinToString { it.displayName ?: "???" }
                    validationErrors.add(
                        ValidationError(
                            translateMsg("plugins.datatransfer.validation.error.observerWithoutAccess", names),
                            fieldId = "observers"
                        )
                    )
                }
            }
        }
    }

    /**
     * Where external access isn't allowed, it can't be switched on. An area that already has it (created
     * before) may keep it or switch it off.
     */
    private fun validateExternalAccessAllowed(validationErrors: MutableList<ValidationError>, dto: DataTransferArea) {
        if (externalAccessAllowed || !dto.externalAccessEnabled) {
            return
        }
        val stored = dto.id?.let { baseDao.find(it, checkAccess = false) }
        val message = translateMsg("plugins.datatransfer.validation.error.externalAccessNotAllowed", gatewayHost)
        if (dto.externalDownloadEnabled == true && stored?.externalDownloadEnabled != true) {
            validationErrors.add(ValidationError(message, fieldId = "externalDownloadEnabled"))
        }
        if (dto.externalUploadEnabled == true && stored?.externalUploadEnabled != true) {
            validationErrors.add(ValidationError(message, fieldId = "externalUploadEnabled"))
        }
    }

    private val externalAccessAllowed: Boolean
        get() = externalAccessLocalAllowed ?: !gatewayPushEnabled

    private val gatewayHost: String
        get() = gatewayPushUrl.ifBlank { "Gateway" }
}
