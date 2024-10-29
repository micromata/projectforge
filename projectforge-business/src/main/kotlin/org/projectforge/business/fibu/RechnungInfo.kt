package org.projectforge.business.fibu

import java.awt.SystemColor.info
import java.io.Serializable
import java.math.BigDecimal

class RechnungInfo(val invoice: AbstractRechnungDO) : Serializable {
    class PositionInfo(position: IRechnungsPosition) : IRechnungsPosition, Serializable {
        override var id = position.id
        override var deleted: Boolean = position.deleted
        override var menge: BigDecimal? = position.menge
        override var einzelNetto: BigDecimal? = position.einzelNetto
        override var netSum: BigDecimal = position.netSum
        override var vat: BigDecimal? = position.vat

    }

    var positions: List<PositionInfo>? = null
        private set

    var netSum = BigDecimal.ZERO

    fun getPosition(id: Long?): PositionInfo? {
        id ?: return null
        return positions?.find { it.id == id }
    }

    init {
        positions = invoice.positionen?.filter { !it.deleted }?.map { PositionInfo(it) }
        netSum = calculateNetSum(invoice.positionen)
    }

    private companion object {
        fun calculateNetSum(positions: Collection<IRechnungsPosition>?): BigDecimal {
            positions ?: return BigDecimal.ZERO
            return positions
                .filter { !it.deleted  }
                .sumOf { it.netSum }
        }
    }
}
