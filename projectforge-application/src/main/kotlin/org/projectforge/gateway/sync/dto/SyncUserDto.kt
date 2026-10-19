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

package org.projectforge.gateway.sync.dto

data class SyncUserDto(
    val username: String,
    val idpExternalId: String? = null,
    val davToken: String? = null,
    val calendarRestToken: String? = null,
    val active: Boolean = true,
    /**
     * Only for observers of a data transfer area pushed to the gateway (see GatewaySyncPushService.pushUsers),
     * null otherwise: the gateway sends them the notification mails of these areas (e.g. on external uploads).
     */
    val email: String? = null,
)
