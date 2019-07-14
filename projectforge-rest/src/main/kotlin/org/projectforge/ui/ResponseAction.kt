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

package org.projectforge.ui

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg

/**
 * Given as response of a rest call to inform the client on how to proceed.
 */
class ResponseAction(val url: String? = null,
                     /**
                      * Default value is [TargetType.REDIRECT] for given url, otherwise null.
                      */
                     var targetType: TargetType? = null,
                     val validationErrors: List<ValidationError>? = null,
                     val message: Message? = null) {
    class Message(val i18nKey: String? = null,
                  /**
                   * The message to display for the user.
                   */
                  var message: String? = null,
                  /** The (technical) message. */
                  var technicalMessage: String? = null,
                  var color: UIColor? = null,
                  vararg messageParams: String) {
        init {
            if (message == null && i18nKey != null) {
                if (messageParams.isNotEmpty())
                    message = translateMsg(i18nKey, messageParams)
                else
                    message = translate(i18nKey)
            }
        }
    }


    init {
        if (message != null && targetType == null) {
            targetType = TargetType.TOAST
        } else if (!url.isNullOrEmpty() && targetType == null) {
            targetType = TargetType.REDIRECT
        }
    }

    /**
     * Variables sent to the client.
     */
    private var variables: MutableMap<String, Any>? = null

    /**
     * Adds a variable sent to the client.
     * @return this for chaining.
     */
    fun addVariable(variable: String, value: Any?): ResponseAction {
        if (value != null) {
            if (variables == null) {
                variables = mutableMapOf()
            }
            variables!![variable] = value
        }
        return this
    }
}

enum class TargetType {
    /**
     * The client should redirect to the given url. If no type is given, REDIRECT is used as default.
     */
    REDIRECT,
    /**
     * The client will receive a download file after calling the rest service with the given url.
     */
    DOWNLOAD,
    /**
     * Show the result message as toast message.
     */
    TOAST,
    /**
     * The client should update all values / states. The values to update are given as variable.
     */
    UPDATE,
    /**
     * The client should call the given url with http method GET.
     */
    GET,
    /**
     * The client should call the given url with http method PUT.
     */
    PUT,
    /**
     * The client should call the given url with http method POST.
     */
    POST,
    /**
     * The client should call the given url with http method DELETE.
     */
    DELETE
}
