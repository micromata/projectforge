/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu

import jakarta.persistence.Tuple
import mu.KotlinLogging
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.ArrayUtils
import org.projectforge.business.fibu.AuftragAndRechnungDaoHelper.createQueryFilterWithDateRestriction
import org.projectforge.business.user.UserRightId
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isIn
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.history.HistoryFormatUtils
import org.projectforge.framework.persistence.utils.SQLHelper.getYearsByTupleOfLocalDate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.RoundingMode

private val log = KotlinLogging.logger {}

@Service
open class EingangsrechnungDao : BaseDao<EingangsrechnungDO>(EingangsrechnungDO::class.java) {
    @Autowired
    private val kontoDao: KontoDao? = null

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    override val additionalHistorySearchDOs: Array<Class<*>>
        get() = ADDITIONAL_HISTORY_SEARCH_DOS

    init {
        userRightId = USER_RIGHT_ID
    }

    val years: IntArray
        /**
         * List of all years with invoices: select min(datum), max(datum) from t_fibu_rechnung.
         */
        get() {
            val minMaxDate = persistenceService.selectNamedSingleResult(
                EingangsrechnungDO.SELECT_MIN_MAX_DATE,
                Tuple::class.java,
            )
            return getYearsByTupleOfLocalDate(minMaxDate)
        }

    fun buildStatistik(list: List<EingangsrechnungDO>?): EingangsrechnungsStatistik {
        val stats = EingangsrechnungsStatistik()
        if (list == null) {
            return stats
        }
        for (rechnung in list) {
            stats.add(rechnung)
        }
        return stats
    }

    /**
     * @param eingangsrechnung
     * @param kontoId          If null, then konto will be set to null;
     * @see BaseDao.findOrLoad
     */
    fun setKonto(eingangsrechnung: EingangsrechnungDO, kontoId: Long) {
        val konto = kontoDao!!.findOrLoad(kontoId)
        eingangsrechnung.konto = konto
    }

    /**
     * Sets the scales of percentage and currency amounts. <br></br>
     * Gutschriftsanzeigen dürfen keine Rechnungsnummer haben. Wenn eine Rechnungsnummer für neue Rechnungen gegeben
     * wurde, so muss sie fortlaufend sein. Berechnet das Zahlungsziel in Tagen, wenn nicht gesetzt, damit es indiziert
     * wird.
     */
    override fun onInsertOrModify(obj: EingangsrechnungDO, operationType: OperationType) {
        AuftragAndRechnungDaoHelper.onSaveOrModify(obj)

        if (obj.zahlBetrag != null) {
            obj.zahlBetrag = obj.zahlBetrag!!.setScale(2, RoundingMode.HALF_UP)
        }
        obj.recalculate()
        if (CollectionUtils.isEmpty(obj.positionen)) {
            throw UserException("fibu.rechnung.error.rechnungHatKeinePositionen")
        }
        val size = obj.positionen!!.size
        for (i in size - 1 downTo 1) {
            // Don't remove first position, remove only the last empty positions.
            val position = obj.positionen!![i]
            if (position.id == null && position.isEmpty) {
                obj.positionen!!.removeAt(i)
            } else {
                break
            }
        }
        RechnungDao.writeUiStatusToXml(obj)
    }

    override fun select(filter: BaseSearchFilter): List<EingangsrechnungDO> {
        val myFilter = if (filter is EingangsrechnungListFilter) {
            filter
        } else {
            EingangsrechnungListFilter(filter)
        }

        val queryFilter = createQueryFilterWithDateRestriction(myFilter)

        if (myFilter.paymentTypes != null && myFilter.paymentTypes.size > 0) {
            queryFilter.add(isIn<Any>("paymentType", myFilter.paymentTypes))
        }

        queryFilter.addOrder(desc("datum"))
        queryFilter.addOrder(desc("kreditor"))

        val list = select(queryFilter)
        if (myFilter.isShowAll || myFilter.isDeleted) {
            return list
        }

        val result: MutableList<EingangsrechnungDO> = ArrayList()
        for (rechnung in list) {
            if (myFilter.isShowUnbezahlt) {
                if (!rechnung.isBezahlt) {
                    result.add(rechnung)
                }
            } else if (myFilter.isShowBezahlt) {
                if (rechnung.isBezahlt) {
                    result.add(rechnung)
                }
            } else if (myFilter.isShowUeberFaellig) {
                if (rechnung.isUeberfaellig) {
                    result.add(rechnung)
                }
            } else {
                log.debug("Unknown filter setting (probably caused by serialize/de-serialize problems): " + myFilter.listType)
            }
        }
        return result
    }

    /**
     * Gets history entries of super and adds all history entries of the EingangsrechnungsPositionDO children.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.selectFlatDisplayHistoryEntries
     */
    override fun customizeHistoryEntries(obj: EingangsrechnungDO, list: MutableList<HistoryEntryDO>) {
        obj.positionen?.forEach { position ->
            val positionEntries = historyService.loadHistory(position)
            HistoryFormatUtils.setPropertyNameForListEntries(positionEntries, prefix = "pos", number = position.number)
            mergeHistoryEntries(list, positionEntries)
            position.kostZuweisungen?.forEach { zuweisung ->
                val kostEntries = historyService.loadHistory(zuweisung)
                HistoryFormatUtils.setPropertyNameForListEntries(
                    kostEntries,
                    Pair("pos", position.number),
                    Pair("kost", zuweisung.index),
                )
                mergeHistoryEntries(list, kostEntries)
            }
        }
    }

    /**
     * Returns also true, if idSet contains the id of any order position.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.contains
     */
    fun contains(idSet: Set<Long>, entry: EingangsrechnungDO): Boolean {
        if (super.contains(idSet, entry)) {
            return true
        }
        for (pos in entry.positionen!!) {
            if (idSet.contains(pos.id)) {
                return true
            }
        }
        return false
    }

    override fun newInstance(): EingangsrechnungDO {
        return EingangsrechnungDO()
    }

    fun findNewestByKreditor(kreditor: String?): EingangsrechnungDO? {
        val resultList = persistenceService.executeQuery(
            "SELECT er FROM EingangsrechnungDO er WHERE er.kreditor = :kreditor AND er.deleted = false ORDER BY er.created DESC",
            EingangsrechnungDO::class.java,
            Pair("kreditor", kreditor),
            maxResults = 1,
        )
        return if (resultList.isNotEmpty()) resultList[0] else null
    }

    override fun isAutocompletionPropertyEnabled(property: String?): Boolean {
        return ArrayUtils.contains(ENABLED_AUTOCOMPLETION_PROPERTIES, property)
    }

    companion object {
        val USER_RIGHT_ID: UserRightId = UserRightId.FIBU_EINGANGSRECHNUNGEN
        private val ADDITIONAL_HISTORY_SEARCH_DOS: Array<Class<*>> = arrayOf(EingangsrechnungsPositionDO::class.java)

        private val ADDITIONAL_SEARCH_FIELDS = arrayOf("positionen.text")

        private val ENABLED_AUTOCOMPLETION_PROPERTIES = arrayOf("kreditor")
    }
}
