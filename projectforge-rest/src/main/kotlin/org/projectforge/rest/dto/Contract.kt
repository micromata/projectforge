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

package org.projectforge.rest.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.business.orga.ContractDO
import org.projectforge.business.orga.ContractStatus
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.Attachment
import java.time.LocalDate

class Contract(id: Int? = null,
               var number: Int? = null,
               var date: LocalDate? = null,
               var validFrom: LocalDate? = null,
               var validUntil: LocalDate? = null,
               var title: String? = null,
               var coContractorA: String? = null,
               var contractPersonA: String? = null,
               var signerA: String? = null,
               var coContractorB: String? = null,
               var contractPersonB: String? = null,
               var signerB: String? = null,
               var signingDate: LocalDate? = null,
               var type: String? = null,
               var status: ContractStatus? = null,
               var text: String? = null,
               var reference: String? = null,
               var filing: String? = null,
               var resubmissionOnDate: LocalDate? = null,
               var dueDate: LocalDate? = null,
               override var attachmentsCounter: Int? = null,
               override var attachmentsSize: Long? = null,
               override var attachments: List<Attachment>? = null
) : BaseDTO<ContractDO>(id), AttachmentsSupport {
    @get:JsonProperty
    val statusAsString: String?
        get() {
            status?.let { return translate(it.i18nKey) }
            return null
        }
}
