package org.projectforge.rest.dto

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeStatus
import org.projectforge.business.fibu.Gender
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.math.BigDecimal
import java.util.*

class Employee(var user: PFUserDO? = null,
               var kost1: Kost1? = null,
               var status: EmployeeStatus? = null,
               var position: String? = null,
               var eintrittsDatum: Date? = null,
               var austrittsDatum: Date? = null,
               var abteilung: String? = null,
               var staffNumber: String? = null,
               var urlaubstage: Int? = null,
               var weeklyWorkingHours: BigDecimal? = null,
               var birthday: Date? = null,
               var accountHolder: String? = null,
               var iban: String? = null,
               var bic: String? = null,
               var gender: Gender? = null,
               var street: String? = null,
               var zipCode: String? = null,
               var city: String? = null,
               var country: String? = null,
               var state: String? = null,
               var comment: String? = null
               )
    : BaseObject<EmployeeDO>()
