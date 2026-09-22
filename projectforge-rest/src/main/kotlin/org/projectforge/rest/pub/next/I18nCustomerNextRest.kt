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

package org.projectforge.rest.pub.next

import org.projectforge.framework.i18n.I18nService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

/**
 * The customer-specific i18n overrides of this deployment, for projectforge-next.
 *
 * next ships a static next-intl catalog generated from the product bundle (`I18nResources`) at build time.
 * A customer's `CustomerI18nResources*.properties`, dropped into the runtime resourceDir, is a deploy-time
 * override the build never saw — so, unlike the former server-rendered UILayout pages (which resolved every
 * text through [org.projectforge.framework.i18n.I18nHelper] and therefore picked the customer bundle up for
 * free), the hand-built next screens would show the product text. This endpoint hands next exactly those
 * overrides so it can overlay them on its static catalog with highest priority.
 *
 * Public (no login): i18n labels are shown on the login page too, and are not sensitive. Answers an empty
 * object when the deployment ships no customer bundle — the common case.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/${I18nCustomerNextRest.REST_PATH}")
open class I18nCustomerNextRest {
    @Autowired
    private lateinit var i18nService: I18nService

    /**
     * @param locale The UI locale next resolved on the client (`de`, `en`). Falls back to the request's locale
     *   (set by the LocaleFilter) when absent.
     */
    @GetMapping
    fun getOverrides(
        @RequestParam("locale", required = false) locale: String?,
    ): Map<String, String> {
        val effectiveLocale = locale?.takeIf { it.isNotBlank() }?.let { Locale(it) }
            ?: ThreadLocalUserContext.locale
            ?: Locale.getDefault()
        return i18nService.getCustomerI18nOverrides(effectiveLocale)
    }

    companion object {
        const val REST_PATH = "i18nCustomerOverrides"
    }
}
