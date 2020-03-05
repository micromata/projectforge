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

package org.projectforge.rest.dto


/**
 * For exchanging data between server and client especially for edit pages. The client doesn't care about the
 * content, but sends this data back.
 */
class ServerData(
        /**
         * Due to security reasons regarding Cross-Site-Request-Forgery.
         */
        var csrfToken: String? = null,
        /**
         * If given, after editing/finishing the current page, the frontend should redirect to this caller.
         */
        var returnToCaller: String? = null,
        /**
         * If given, after editing/finishing the current page, the frontend should redirect to the caller
         * specified by [returnToCaller] with this query params.
         */
        var returnToCallerParams: Map<String, String>? = null
)
