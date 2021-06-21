/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.merlin

import de.micromata.merlin.word.WordDocument
import de.micromata.merlin.word.templating.Template
import de.micromata.merlin.word.templating.TemplateDefinition
import de.micromata.merlin.word.templating.WordTemplateChecker
import java.io.InputStream

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object MerlinRunner {
  fun analyzeWordDocument(istream: InputStream, filename: String) {
    val doc = WordDocument(istream, filename)
    val templateChecker = WordTemplateChecker(doc)
    val statistics = templateChecker.getTemplate().getStatistics()
  }
}
