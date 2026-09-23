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

package org.projectforge.rest

import org.projectforge.Constants
import org.projectforge.SystemStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/**
 * Handles the bare root url. Without this the request reaches the DispatcherServlet, finds no
 * handler and fails with "No static resource" (there is no index.html at the static root since
 * the React app moved to /react and the app to /next).
 */
@Controller
class HomeController {
    @Autowired
    private lateinit var systemStatus: SystemStatus

    /**
     * Redirects to the setup page on a fresh installation (empty database), otherwise into the
     * application. The Next.js app then forwards to the login if no user is logged in.
     */
    @GetMapping("/")
    fun redirect(): String {
        return if (systemStatus.setupRequiredFirst == true) {
            "redirect:/${Constants.NEXT_APP_PATH}setup"
        } else {
            "redirect:/${Constants.NEXT_APP_PATH}"
        }
    }
}
