/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.pub

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

/**
 * This rest service is available without login credentials but with an access key and only if SMS functionality
 * is configured as well as authentication key..
 */
@Configuration
open class MessagingServiceConfig {
    /**
     * If auth key isn't given or sms isn't configured, the messaging service isn't available.
     */
    @Value("\${projectforge.sms.publicRestCallAuthKey}")
    open var authkey: String? = null
}
