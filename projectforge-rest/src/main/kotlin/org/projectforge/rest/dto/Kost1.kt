package org.projectforge.rest.dto

import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.KostentraegerStatus

class Kost1(
        id: Int? = null,
        var nummernkreis: Int = 0,
        var bereich: Int = 0,
        var teilbereich: Int = 0,
        var endziffer: Int = 0,
        var kostentraegerStatus: KostentraegerStatus? = null,
        var description: String? = null,
        var formattedNumber: String? = null
) : BaseObject<Kost1DO>(id)
