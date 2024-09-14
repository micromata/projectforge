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
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.fibu.AuftragAndRechnungDaoHelper.createCriterionForPeriodOfPerformance
import org.projectforge.business.fibu.AuftragAndRechnungDaoHelper.createQueryFilterWithDateRestriction
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.business.user.UserRightId
import org.projectforge.common.i18n.MessageParam
import org.projectforge.common.i18n.MessageParamType
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.utils.SQLHelper.getYearsByTupleOfLocalDate
import org.projectforge.framework.time.PFDateTime.Companion.from
import org.projectforge.framework.time.PFDateTime.Companion.now
import org.projectforge.framework.xmlstream.XmlObjectWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

private val log = KotlinLogging.logger {}

@Service
open class RechnungDao : BaseDao<RechnungDO>(RechnungDO::class.java) {
    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    /**
     * @return the rechnungCache
     */
    @Autowired
    lateinit var rechnungCache: RechnungCache

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
                RechnungDO.SELECT_MIN_MAX_DATE,
                Tuple::class.java
            )
            return getYearsByTupleOfLocalDate(minMaxDate)
        }

    fun buildStatistik(list: List<RechnungDO>?): RechnungsStatistik {
        val stats = RechnungsStatistik()
        if (list == null) {
            return stats
        }
        for (rechnung in list) {
            stats.add(rechnung)
        }
        return stats
    }

    /**
     * @param rechnung
     * @param days
     */
    fun calculateFaelligkeit(rechnung: RechnungDO, days: Int): Date? {
        if (rechnung.datum == null) {
            return null
        }
        var dateTime = from(rechnung.datum!!) // not null
        dateTime = dateTime.plusDays(days.toLong())
        return dateTime.utilDate
    }

    /**
     * @param rechnung
     * @param kundeId  If null, then kunde will be set to null;
     */
    fun setKunde(rechnung: RechnungDO, kundeId: Long) {
        val kunde = kundeDao.getOrLoad(kundeId)
        rechnung.kunde = kunde
    }

    /**
     * @param rechnung
     * @param projektId If null, then projekt will be set to null;
     */
    fun setProjekt(rechnung: RechnungDO, projektId: Long) {
        val projekt = projektDao.getOrLoad(projektId)
        rechnung.projekt = projekt
    }

    /**
     * Sets the scales of percentage and currency amounts. <br></br>
     * Gutschriftsanzeigen dürfen keine Rechnungsnummer haben. Wenn eine Rechnungsnummer für neue Rechnungen gegeben
     * wurde, so muss sie fortlaufend sein. Berechnet das Zahlungsziel in Tagen, wenn nicht gesetzt, damit es indiziert
     * wird.
     */
    override fun onSaveOrModify(rechnung: RechnungDO) {
        if (RechnungTyp.RECHNUNG == rechnung.typ && rechnung.id != null) {
            val originValue = internalGetById(rechnung.id)
            if (RechnungStatus.GEPLANT == originValue!!.status && RechnungStatus.GEPLANT != rechnung.status) {
                rechnung.nummer = getNextNumber(rechnung)

                val day = now()
                rechnung.datum = day.localDate

                val zahlungsZielInTagen = rechnung.zahlungsZielInTagen
                if (zahlungsZielInTagen != null) {
                    val faelligkeitDay = day.plusDays(zahlungsZielInTagen.toLong())
                    rechnung.faelligkeit = faelligkeitDay.localDate
                }
            }
        }

        AuftragAndRechnungDaoHelper.onSaveOrModify(rechnung)

        validate(rechnung)

        if (rechnung.typ == RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN) {
            if (rechnung.nummer != null) {
                throw UserException("fibu.rechnung.error.gutschriftsanzeigeDarfKeineRechnungsnummerHaben")
            }
        } else {
            if (RechnungStatus.GEPLANT != rechnung.status && rechnung.nummer == null) {
                throw UserException(
                    "validation.required.valueNotPresent",
                    MessageParam("fibu.rechnung.nummer", MessageParamType.I18N_KEY)
                )
            }
            if (RechnungStatus.GEPLANT != rechnung.status) {
                if (rechnung.id == null) {
                    // Neue Rechnung
                    val next = getNextNumber(rechnung)
                    if (next != rechnung.nummer) {
                        throw UserException("fibu.rechnung.error.rechnungsNummerIstNichtFortlaufend")
                    }
                } else {
                    val other = persistenceService.selectNamedSingleResult(
                        RechnungDO.FIND_OTHER_BY_NUMMER,
                        RechnungDO::class.java,
                        Pair("nummer", rechnung.nummer),
                        Pair("id", rechnung.id)
                    )
                    if (other != null) {
                        throw UserException("fibu.rechnung.error.rechnungsNummerBereitsVergeben")
                    }
                }
            }
        }
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
        writeUiStatusToXml(rechnung)
    }

    private fun validate(rechnung: RechnungDO) {
        val status = rechnung.status
        val zahlBetrag = rechnung.zahlBetrag
        val zahlBetragExists = (zahlBetrag != null && zahlBetrag.compareTo(BigDecimal.ZERO) != 0)
        if (status == RechnungStatus.BEZAHLT && !zahlBetragExists) {
            throw UserException("fibu.rechnung.error.statusBezahltErfordertZahlBetrag")
        }

        val projektId = rechnung.projektId
        val kundeId = rechnung.kundeId
        val kundeText = rechnung.kundeText
        if (projektId == null && kundeId == null && StringUtils.isEmpty(kundeText)) {
            throw UserException("fibu.rechnung.error.kundeTextOderProjektRequired")
        }
    }

    override fun afterSaveOrModify(obj: RechnungDO) {
        rechnungCache.setExpired() // Expire the cache because assignments to order position may be changed.
        auftragsCache.setExpired()
    }

    override fun prepareHibernateSearch(obj: RechnungDO, operationType: OperationType) {
        projektDao.initializeProjektManagerGroup(obj.projekt)
    }

    /**
     * Fetches the cost assignments.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.getById
     */
    @Throws(AccessException::class)
    override fun getById(id: Serializable?): RechnungDO? {
        val rechnung = super.getById(id)
        for (pos in rechnung!!.positionen!!) {
            val list: List<KostZuweisungDO>? = pos.kostZuweisungen
            if (list != null && list.size > 0) {
                // Kostzuweisung is initialized
            }
        }
        return rechnung
    }

    override fun getList(filter: BaseSearchFilter): List<RechnungDO> {
        val myFilter = if (filter is RechnungListFilter) {
            filter
        } else {
            RechnungListFilter(filter)
        }

        val queryFilter = createQueryFilterWithDateRestriction(myFilter)
        queryFilter.addOrder(desc("datum"))
        queryFilter.addOrder(desc("nummer"))
        if (myFilter.isShowKostZuweisungStatus) {
            //queryFilter.setFetchMode("positionen.kostZuweisungen", FetchMode.JOIN);
        }

        createCriterionForPeriodOfPerformance(myFilter).ifPresent { predicate: DBPredicate? ->
            queryFilter.add(
                predicate!!
            )
        }

        val list = getList(queryFilter)
        if (myFilter.isShowAll || myFilter.isDeleted) {
            return list
        }

        val result: MutableList<RechnungDO> = ArrayList()
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
                log.error("Unknown filter setting: " + myFilter.listType)
                break
            }
        }
        return result
    }

    override fun sorted(list: List<RechnungDO>): List<RechnungDO> {
        Collections.sort(list)
        return list
    }

    val nextNumber: Int?
        /**
         * Gets the highest Rechnungsnummer.
         */
        get() = getNextNumber(null)

    /**
     * Gets the highest Rechnungsnummer.
     *
     * @param rechnung wird benötigt, damit geschaut werden kann, ob diese Rechnung ggf. schon existiert. Wenn sie schon
     * eine Nummer hatte, so kann verhindert werden, dass sie eine nächst höhere Nummer bekommt. Eine solche
     * Rechnung bekommt die alte Nummer wieder zugeordnet.
     */
    fun getNextNumber(rechnung: RechnungDO?): Int? {
        if (rechnung?.id != null) {
            val orig = internalGetById(rechnung.id)
            if (orig!!.nummer != null) {
                rechnung.nummer = orig.nummer
                return orig.nummer
            }
        }
        return persistenceService.getNextNumber("RechnungDO", "nummer", START_NUMBER)
    }

    /**
     * Gets history entries of super and adds all history entries of the RechnungsPositionDO children.
     */
    override fun getDisplayHistoryEntries(obj: RechnungDO): MutableList<DisplayHistoryEntry> {
        val list = super.getDisplayHistoryEntries(obj)
        if (!hasLoggedInUserHistoryAccess(obj, false)) {
            return list
        }
        if (CollectionUtils.isNotEmpty(obj.positionen)) {
            for (position in obj.positionen!!) {
                val entries: List<DisplayHistoryEntry> = internalGetDisplayHistoryEntries(position)
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
                        val kostEntries: List<DisplayHistoryEntry> = internalGetDisplayHistoryEntries(zuweisung)
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
        list.sortWith(Comparator { o1: DisplayHistoryEntry, o2: DisplayHistoryEntry -> (o2.timestamp.compareTo(o1.timestamp)) })
        return list
    }

    /**
     * Returns also true, if idSet contains the id of any order position.
     */
    override fun contains(idSet: Set<Long?>?, entry: RechnungDO): Boolean {
        idSet ?: return false
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

    override fun newInstance(): RechnungDO {
        return RechnungDO()
    }

    companion object {
        @JvmField
        val USER_RIGHT_ID: UserRightId = UserRightId.FIBU_AUSGANGSRECHNUNGEN

        const val START_NUMBER: Int = 1000

        val ADDITIONAL_HISTORY_SEARCH_DOS = arrayOf<Class<*>>(
            RechnungsPositionDO::class.java
        )

        val ADDITIONAL_SEARCH_FIELDS = arrayOf(
            "kunde.name", "projekt.name",
            "projekt.kunde.name", "positionen.auftragsPosition.auftrag.nummer"
        )

        @JvmStatic
        fun getNettoSumme(col: Collection<RechnungsPositionVO>?): BigDecimal {
            var nettoSumme = BigDecimal.ZERO
            if (col != null && col.size > 0) {
                for (pos in col) {
                    nettoSumme = nettoSumme.add(pos.nettoSumme)
                }
            }
            return nettoSumme
        }

        fun writeUiStatusToXml(rechnung: AbstractRechnungDO) {
            val uiStatusAsXml = XmlObjectWriter.writeAsXml(rechnung.uiStatus)
            rechnung.uiStatusAsXml = uiStatusAsXml
        }
    }
}
