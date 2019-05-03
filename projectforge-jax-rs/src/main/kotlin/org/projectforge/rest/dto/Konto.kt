package org.projectforge.rest.dto

import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KontoStatus

class Konto(var nummer: Int? = null,
            var bezeichnung: String? = null,
            var description: String? = null,
            var kontoStatus: KontoStatus? = null
) : BaseObject<KontoDO>()
