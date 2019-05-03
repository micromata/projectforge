package org.projectforge.rest.dto

import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeStatus

class Kunde(var bereich: Int? = null,
            var name: String? = null,
            var identifier: String? = null,
            var division: String? = null,
            var status: KundeStatus? = null,
            var description: String? = null,
            var konto: Konto? = null
) : BaseObject<KundeDO>()
