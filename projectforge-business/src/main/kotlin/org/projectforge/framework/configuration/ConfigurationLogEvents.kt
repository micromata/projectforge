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

package org.projectforge.framework.configuration

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogNotify

object ConfigurationLogEvents {
    /** See [ConfigurationLegacyProperties]. */
    @JvmField
    val LEGACY_PROPERTY_SET = LogEvent(
        code = "configuration.legacyPropertySet",
        category = LogCategory.CONFIG,
        notify = LogNotify.DIGEST_IF_NEW,
        explanation = "A property of projectforge.properties is still set, although its setting is a configuration " +
                "parameter now (configuration page). The property was taken over once, when the parameter was " +
                "created; since then it is ignored, also if it differs from the parameter.",
        action = "Check the parameter on the configuration page and remove the property from projectforge.properties " +
                "(logged on every start as long as it is set).",
    )

    /** See [ConfigXmlMigration]. */
    @JvmField
    val CONFIG_XML_NOT_MIGRATED = LogEvent(
        code = "configuration.configXmlNotMigrated",
        category = LogCategory.CONFIG,
        notify = LogNotify.DIGEST_IF_NEW,
        explanation = "The former config.xml in the base dir couldn't be taken over completely into the configuration " +
                "parameters (holidays, jira, contract types, accounting) and the properties projectforge.dir.* " +
                "(directories). It is kept and the take-over is retried on the next start.",
        action = "Check the log for the cause (e.g. config.xml not readable, projectforge.properties not writable). " +
                "Alternatively maintain the parameters on the configuration page and the directories in " +
                "projectforge.properties and remove config.xml.",
    )

    @JvmField
    val UNKNOWN_ENTRY = LogEvent(
        code = "configuration.unknownEntry",
        category = LogCategory.CONFIG,
        notify = LogNotify.DIGEST_IF_NEW,
        explanation = "The database contains a configuration parameter this version doesn't know: removed by an " +
                "update, or created by a newer version on the same database (e.g. after a downgrade, or several " +
                "servers of different versions on one database). The row is marked as deleted; a version knowing " +
                "the parameter restores it with its value.",
        action = "None after an update that removed the parameter. Otherwise check that only one version of " +
                "ProjectForge runs on this database.",
    )
}
