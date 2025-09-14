/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.importer.ImportFieldSettings
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportSettings
import org.projectforge.rest.importer.ImportStorage

class EingangsrechnungImportStorage(importSettings: String? = null) :
    ImportStorage<EingangsrechnungPosImportDTO>(
        ImportSettings()
            .addFieldSettings(ImportFieldSettings("kreditor").withLabel(translate("fibu.common.creditor")))
            .addFieldSettings(ImportFieldSettings("referenz").withLabel(translate("fibu.common.reference")))
            .addFieldSettings(ImportFieldSettings("datum").withLabel(translate("fibu.rechnung.datum")))
            .parseSettings(
                importSettings,
                EingangsrechnungDO::class.java,
                EingangsrechnungDO::kreditor.name
            )
    ) {

    var readInvoices = mutableListOf<EingangsrechnungPosImportDTO>()

    /**
     * Map of consolidated invoices grouped by RENR (invoice number).
     * Key: RENR (invoice number)
     * Value: List of positions belonging to that invoice
     */
    var consolidatedInvoices = mapOf<String, List<EingangsrechnungPosImportDTO>>()

    override fun prepareEntity(): EingangsrechnungPosImportDTO {
        return EingangsrechnungPosImportDTO()
    }

    override fun commitEntity(obj: EingangsrechnungPosImportDTO) {
        readInvoices.add(obj)
        val pairEntry = ImportPairEntry(read = obj)
        addEntry(pairEntry)
    }
}
