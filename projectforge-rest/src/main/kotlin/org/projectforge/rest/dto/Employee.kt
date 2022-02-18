/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeStatus
import org.projectforge.business.fibu.IsoGender
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.math.BigDecimal
import java.time.LocalDate

class Employee(id: Int? = null,
               displayName: String? = null,
               var user: PFUserDO? = null,
               var kost1: Kost1? = null,
               var status: EmployeeStatus? = null,
               var position: String? = null,
               var eintrittsDatum: LocalDate? = null,
               var austrittsDatum: LocalDate? = null,
               var abteilung: String? = null,
               var staffNumber: String? = null,
               var urlaubstage: Int? = null,
               var weeklyWorkingHours: BigDecimal? = null,
               var birthday: LocalDate? = null,
               var accountHolder: String? = null,
               var iban: String? = null,
               var bic: String? = null,
               var gender: IsoGender? = null,
               var street: String? = null,
               var zipCode: String? = null,
               var city: String? = null,
               var country: String? = null,
               var state: String? = null,
               var comment: String? = null
) : BaseDTODisplayObject<EmployeeDO>(id, displayName = displayName)
