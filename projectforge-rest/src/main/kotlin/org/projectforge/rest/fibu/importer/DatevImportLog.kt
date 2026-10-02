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

package org.projectforge.rest.fibu.importer

import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.admin.LogViewerPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.importer.ImportPairEntry

/**
 * The user's log of the DATEV import, shown by the log viewer ("view log" of the import page, as the legacy Wicket
 * page offers). One subscription serves both tabs (records and chart of accounts); it collects the messages of the
 * DATEV import classes of this package (upload, parsing, reconcile and the commit jobs, which keep the user's MDC).
 *
 * The subscription has to exist before the messages are logged, so it is ensured on every request of the import
 * (see `checkRight` of [DatevRecordImportRest] and [DatevAccountImportRest]), not only when the log is opened.
 */
internal object DatevImportLog {
    /**
     * Not the legacy page's title "Datev-Import": the title is the subscription's key per user, and the legacy
     * subscription only matches the legacy importer's packages.
     */
    private const val TITLE = "DATEV import (next)"

    /** All DATEV import classes of this package (DatevRecord*, DatevAccount*), not the other importers there. */
    private const val LOGGER_NAME_BEGINNING = "org.projectforge.rest.fibu.importer.Datev"

    fun ensureSubscription(): LogSubscription? {
        val username = ThreadLocalUserContext.loggedInUser?.username ?: return null
        return LogSubscription.ensureSubscription(
            title = TITLE,
            displayTitle = translate("fibu.datev.import"),
            user = username,
            create = { title, user -> LogSubscription(title, user, LogEventLoggerNameMatcher(LOGGER_NAME_BEGINNING)) },
        )
    }

    /**
     * The reconcile result per status, e.g. `NEW=3, MODIFIED=1, UNMODIFIED=1290, FAULTY=1`. Marks the entries as
     * reconciled (as [org.projectforge.rest.importer.ImportStorage.reconcileImportStorage] does right after), so
     * their status is known already inside `doReconcileImportStorage`.
     */
    fun reconcileSummary(entries: List<ImportPairEntry<*>>): String {
        entries.forEach { it.reconciled = true }
        return entries.groupingBy { it.status }.eachCount().entries
            .sortedBy { it.key.ordinal }
            .joinToString(", ") { "${it.key}=${it.value}" }
    }

    /** The (React) log viewer's url of the user's subscription, relative to the app root, e.g. `react/logViewer/dynamic/7`. */
    fun viewerUrl(): String? {
        val subscription = ensureSubscription() ?: return null
        return PagesResolver.getDynamicPageUrl(LogViewerPageRest::class.java, id = subscription.id)
    }
}
