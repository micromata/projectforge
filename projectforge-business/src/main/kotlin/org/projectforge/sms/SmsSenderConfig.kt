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

package org.projectforge.sms

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration


@Configuration
open class SmsSenderConfig {
    enum class HttpMethodType {
        POST, GET
    }

    @Value("\${projectforge.sms.httpMethod}")
    open var httpMethodType: HttpMethodType? = null

    @Value("\${projectforge.sms.url}")
    open var url: String? = null

    @Value("#{\${projectforge.sms.httpParameters}}")
    open var httpParams: Map<String, String>? = null

    @Value("\${projectforge.sms.returnCodePattern.success}")
    open var smsReturnPatternSuccess: String? = null

    @Value("\${projectforge.sms.returnCodePattern.numberError}")
    open var smsReturnPatternNumberError: String? = null

    @Value("\${projectforge.sms.returnCodePattern.messageToLargeError}")
    open var smsReturnPatternMessageToLargeError: String? = null

    @Value("\${projectforge.sms.returnCodePattern.messageError}")
    open var smsReturnPatternMessageError: String? = null

    @Value("\${projectforge.sms.returnCodePattern.error}")
    open var smsReturnPatternError: String? = null

    @Value("\${projectforge.sms.smsMaxMessageLength}")
    open var smsMaxMessageLength = 160

    fun isSmsConfigured(): Boolean {
        return StringUtils.isNotBlank(url)
    }

    fun setHttpMethodType(httpMethodType: String): SmsSenderConfig {
        this.httpMethodType = if (StringUtils.equalsIgnoreCase("get", httpMethodType)) HttpMethodType.GET else HttpMethodType.POST
        return this
    }
}
