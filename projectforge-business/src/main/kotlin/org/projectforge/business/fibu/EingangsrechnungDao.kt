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
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isIn
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.utils.SQLHelper.getYearsByTupleOfLocalDate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.math.RoundingMode

private val log = KotlinLogging.logger {}

@Repository
class EingangsrechnungDao : BaseDao<EingangsrechnungDO>(EingangsrechnungDO::class.java) {
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
            val minMaxDate = persistenceService.selectSingleResult(
                EingangsrechnungDO.SELECT_MIN_MAX_DATE,
                Tuple::class.java,
                namedQuery = true,
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
     * @see BaseDao.getOrLoad
     */
    fun setKonto(eingangsrechnung: EingangsrechnungDO, kontoId: Int) {
        val konto = kontoDao!!.getOrLoad(kontoId)
        eingangsrechnung.konto = konto
    }

    /**
     * Sets the scales of percentage and currency amounts. <br></br>
     * Gutschriftsanzeigen dürfen keine Rechnungsnummer haben. Wenn eine Rechnungsnummer für neue Rechnungen gegeben
     * wurde, so muss sie fortlaufend sein. Berechnet das Zahlungsziel in Tagen, wenn nicht gesetzt, damit es indiziert
     * wird.
     */
    override fun onSaveOrModify(rechnung: EingangsrechnungDO) {
        AuftragAndRechnungDaoHelper.onSaveOrModify(rechnung)

        if (rechnung.zahlBetrag != null) {
            rechnung.zahlBetrag = rechnung.zahlBetrag!!.setScale(2, RoundingMode.HALF_UP)
        }
        rechnung.recalculate()
        if (CollectionUtils.isEmpty(rechnung.positionen)) {
            throw UserException("fibu.rechnung.error.rechnungHatKeinePositionen")
        }
        val size = rechnung.positionen!!.size
        for (i in size - 1 downTo 1) {
            // Don't remove first position, remove only the last empty positions.
            val position = rechnung.positionen!![i]
            if (position.id == null && position.isEmpty) {
                rechnung.positionen!!.removeAt(i)
            } else {
                break
            }
        }
        RechnungDao.writeUiStatusToXml(rechnung)
    }

    override fun getList(filter: BaseSearchFilter): List<EingangsrechnungDO> {
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

        val list = getList(queryFilter)
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
     * @see org.projectforge.framework.persistence.api.BaseDao.getDisplayHistoryEntries
     */
    override fun getDisplayHistoryEntries(obj: EingangsrechnungDO): MutableList<DisplayHistoryEntry> {
        val list = super.getDisplayHistoryEntries(obj)
        if (!hasLoggedInUserHistoryAccess(obj, false)) {
            return list
        }
        if (CollectionUtils.isNotEmpty(obj.positionen)) {
            for (position in obj.positionen!!) {
                val entries = internalGetDisplayHistoryEntries(position)
                for (entry in entries) {
                    val propertyName = entry.propertyName
                    if (propertyName != null) {
                        entry.propertyName =
                            "#" + position.number + ":" + entry.propertyName // Prepend number of positon.
                    } else {
                        entry.propertyName = "#" + position.number
                    }
                }
                list.addAll(entries)
                if (CollectionUtils.isNotEmpty(position.kostZuweisungen)) {
                    for (zuweisung in position.kostZuweisungen!!) {
                        val kostEntries = internalGetDisplayHistoryEntries(zuweisung)
                        for (entry in kostEntries) {
                            val propertyName = entry.propertyName
                            if (propertyName != null) {
                                entry.propertyName =
                                    "#" + position.number + ".kost#" + zuweisung.index + ":" + entry.propertyName // Prepend
                                // number of positon and index of zuweisung.
                            } else {
                                entry.propertyName = "#" + position.number + ".kost#" + zuweisung.index
                            }
                        }
                        list.addAll(kostEntries)
                    }
                }
            }
        }
        list.sortWith(Comparator<DisplayHistoryEntry> { o1, o2 ->
            (o2.timestamp.compareTo(
                o1.timestamp
            ))
        })
        return list
    }

    /**
     * Returns also true, if idSet contains the id of any order position.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.contains
     */
    fun contains(idSet: Set<Int?>, entry: EingangsrechnungDO): Boolean {
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

    override fun sorted(list: List<EingangsrechnungDO>): List<EingangsrechnungDO> {
        return list.sorted()
    }

    override fun newInstance(): EingangsrechnungDO {
        return EingangsrechnungDO()
    }

    fun findNewestByKreditor(kreditor: String?): EingangsrechnungDO? {
        val resultList = persistenceService.query(
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
