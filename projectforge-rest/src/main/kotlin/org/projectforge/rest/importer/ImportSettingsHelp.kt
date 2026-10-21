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

package org.projectforge.rest.importer

import org.projectforge.framework.i18n.translate

/**
 * The help on the import settings of an entity (e.g. a bank account's settings of its csv import): the current
 * settings with every importable field, as markdown with a code block the user may copy into the settings field.
 * Formerly `AbstractImportPageRest.createSettingsHelp`, which wrapped the same text into a `UIAlert`.
 */
object ImportSettingsHelp {
    fun asMarkdown(importSettings: ImportSettings?): String {
        val sb = StringBuilder()
        sb.appendLine(translate("import.help.settings.info"))
        sb.appendLine("```")
        if (importSettings?.charSet == null) {
            sb.appendLine("# Uncomment the following line for other coding than UTF-8")
            sb.appendLine("#encoding=iso-8859-15")
        }
        if (importSettings != null) {
            sb.appendLine("encoding=${importSettings.charSet.name()}")
            importSettings.allFieldNames.forEach { fieldName ->
                if (!arrayOf("id", "created", "lastUpdate", "deleted", "importSettings").contains(fieldName)) {
                    val fieldSettings = importSettings.getFieldSettings(fieldName)
                    sb.append("$fieldName=")
                    if (fieldSettings != null) {
                        sb.appendLine(fieldSettings.getSettingsAsString(true))
                    } else {
                        sb.appendLine()
                    }
                }
            }
        }
        sb.appendLine("# Example of field 'birthday', found by header column:")
        sb.appendLine("#columnfield=[header column(s)]|:[format]")
        sb.appendLine("#birthday=birthday*|*born*|:dd.MM.yyyy|:dd.MM.yy")
        sb.appendLine("# header column(s) are mapped case-insensitive, wildchars '*' for text and '?' for single char are supported.")
        sb.appendLine("```")
        return sb.toString()
    }
}
