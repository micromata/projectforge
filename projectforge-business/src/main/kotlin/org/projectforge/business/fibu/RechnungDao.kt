/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.persistence.history.HistoryLoadContext
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
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    /**
     * @return the rechnungCache
     */
    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var currencyConversionService: CurrencyConversionService

    @Autowired
    private lateinit var configurationService: org.projectforge.business.configuration.ConfigurationService

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
        // Initialize companion object services for currency conversion
        AbstractRechnungsStatistik.currencyConversionService = currencyConversionService
        AbstractRechnungsStatistik.configurationService = configurationService
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
    fun setKunde(rechnung: RechnungDO, kundeNummer: Long) {
        val kunde = kundeDao.findOrLoad(kundeNummer)
        rechnung.kunde = kunde
    }

    /**
     * @param rechnung
     * @param kundeId  If null, then kunde will be set to null;
     */
    fun setKunde(rechnung: RechnungDO, kunde: KundeDO?) {
        kunde ?: return
        setKunde(rechnung, kunde.nummer!!)
    }

    /**
     * @param rechnung
     * @param projektId If null, then projekt will be set to null;
     */
    fun setProjekt(rechnung: RechnungDO, projektId: Long) {
        val projekt = projektDao.findOrLoad(projektId)
        rechnung.projekt = projekt
    }

    override fun afterLoad(obj: RechnungDO) {
        obj.info = rechnungCache.ensureRechnungInfo(obj)
    }

    override fun afterInsertOrModify(obj: RechnungDO, operationType: OperationType) {
        rechnungCache.update(obj)
    }

    /**
     * Sets the scales of percentage and currency amounts. <br></br>
     * Gutschriftsanzeigen dürfen keine Rechnungsnummer haben. Wenn eine Rechnungsnummer für neue Rechnungen gegeben
     * wurde, so muss sie fortlaufend sein. Berechnet das Zahlungsziel in Tagen, wenn nicht gesetzt, damit es indiziert
     * wird.
     */
    override fun onInsertOrModify(obj: RechnungDO, operationType: OperationType) {
        if (RechnungTyp.RECHNUNG == obj.typ && obj.id != null) {
            val originValue = find(obj.id, checkAccess = false)
            if (RechnungStatus.GEPLANT == originValue!!.status && RechnungStatus.GEPLANT != obj.status) {
                obj.nummer = getNextNumber(obj)

                val day = now()
                obj.datum = day.localDate

                val zahlungsZielInTagen = obj.zahlungsZielInTagen
                if (zahlungsZielInTagen != null) {
                    val faelligkeitDay = day.plusDays(zahlungsZielInTagen.toLong())
                    obj.faelligkeit = faelligkeitDay.localDate
                }
            }
        }

        AuftragAndRechnungDaoHelper.onSaveOrModify(obj)

        validate(obj)

        if (obj.typ == RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN) {
            if (obj.nummer != null) {
                throw UserException("fibu.rechnung.error.gutschriftsanzeigeDarfKeineRechnungsnummerHaben")
            }
        } else {
            if (RechnungStatus.GEPLANT != obj.status && obj.nummer == null) {
                throw UserException(
                    "validation.required.valueNotPresent",
                    MessageParam("fibu.rechnung.nummer", MessageParamType.I18N_KEY)
                )
            }
            if (RechnungStatus.GEPLANT != obj.status) {
                if (obj.id == null) {
                    // Neue Rechnung
                    val next = getNextNumber(obj)
                    if (next != obj.nummer) {
                        throw UserException("fibu.rechnung.error.rechnungsNummerIstNichtFortlaufend")
                    }
                } else {
                    val other = persistenceService.selectNamedSingleResult(
                        RechnungDO.FIND_OTHER_BY_NUMMER,
                        RechnungDO::class.java,
                        Pair("nummer", obj.nummer),
                        Pair("id", obj.id)
                    )
                    if (other != null) {
                        throw UserException("fibu.rechnung.error.rechnungsNummerBereitsVergeben")
                    }
                }
            }
        }
        if (obj.zahlBetrag != null) {
            obj.zahlBetrag = obj.zahlBetrag!!.setScale(2, RoundingMode.HALF_UP)
        }
        obj.recalculate()
        if (obj.positionen.isNullOrEmpty()) {
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
        writeUiStatusToXml(obj)
    }

    private fun validate(rechnung: RechnungDO) {
        if (rechnung.datum == null) {
            throw UserException(
                "validation.required.valueNotPresent",
                MessageParam("fibu.rechnung.datum", MessageParamType.I18N_KEY)
            )
        }
        val status = rechnung.status
        val zahlBetrag = rechnung.zahlBetrag
        val zahlBetragExists = (zahlBetrag != null && zahlBetrag.compareTo(BigDecimal.ZERO) != 0)
        if (status == RechnungStatus.BEZAHLT && !zahlBetragExists) {
            throw UserException("fibu.rechnung.error.statusBezahltErfordertZahlBetrag")
        }

        val projektId = rechnung.projekt?.id
        val kundeId = rechnung.kunde?.nummer
        val kundeText = rechnung.kundeText
        if (projektId == null && kundeId == null && kundeText.isNullOrEmpty()) {
            throw UserException("fibu.rechnung.error.kundeTextOderProjektRequired")
        }
    }

    override fun prepareHibernateSearch(obj: RechnungDO, operationType: OperationType) {
        projektDao.initializeProjektManagerGroup(obj.projekt)
    }

    /**
     * Fetches the cost assignments.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.find
     */
    @Throws(AccessException::class)
    override fun find(id: Serializable?, checkAccess: Boolean, attached: Boolean): RechnungDO? {
        val rechnung = super.find(id, checkAccess = checkAccess, attached = attached)
        for (pos in rechnung!!.positionen!!) {
            val list: List<KostZuweisungDO>? = pos.kostZuweisungen
            if (list != null && list.size > 0) {
                // Kostzuweisung is initialized
            }
        }
        return rechnung
    }

    override fun select(filter: BaseSearchFilter): List<RechnungDO> {
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

        val list = select(queryFilter)
        if (myFilter.isShowAll || myFilter.deleted) {
            return list
        }

        val result: MutableList<RechnungDO> = ArrayList()
        for (rechnung in list) {
            val info = rechnungCache.getRechnungInfo(rechnung.id) ?: RechnungInfo(rechnung)
            if (myFilter.isShowUnbezahlt) {
                if (!info.isBezahlt) {
                    result.add(rechnung)
                }
            } else if (myFilter.isShowBezahlt) {
                if (info.isBezahlt) {
                    result.add(rechnung)
                }
            } else if (myFilter.isShowUeberFaellig) {
                if (info.isUeberfaellig) {
                    result.add(rechnung)
                }
            } else {
                log.error("Unknown filter setting: " + myFilter.listType)
                break
            }
        }
        return result
    }

    val nextNumber: Int
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
    fun getNextNumber(rechnung: RechnungDO?): Int {
        if (rechnung?.id != null) {
            val orig = find(rechnung.id, checkAccess = false)
            if (orig!!.nummer != null) {
                rechnung.nummer = orig.nummer
                return orig.nummer!!
            }
        }
        return persistenceService.getNextNumber("RechnungDO", "nummer", START_NUMBER)
    }

    /**
     * Gets history entries of super and adds all history entries of the RechnungsPositionDO children.
     */
    override fun addOwnHistoryEntries(obj: RechnungDO, context: HistoryLoadContext) {
        obj.positionen?.forEach { position ->
            historyService.loadAndMergeHistory(position, context)
            position.kostZuweisungen?.forEach { zuweisung ->
                historyService.loadAndMergeHistory(zuweisung, context)
            }
        }
    }

    override fun getHistoryPropertyPrefix(context: HistoryLoadContext): String? {
        val entry = context.requiredHistoryEntry
        val item = context.findLoadedEntity(entry)
        return if (item is RechnungsPositionDO) {
            item.number.toString()
        } else if (item is KostZuweisungDO) {
            "${item.rechnungsPosition?.number}: kost #${item.index}"
        } else {
            null
        }
    }

    /**
     * Returns also true, if idSet contains the id of any order position.
     */
    override fun contains(idSet: Set<Long>?, entry: RechnungDO): Boolean {
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
            "projekt.kunde.name", // "positionen.auftragsPosition.auftrag.nummer"
        )

        @JvmStatic
        fun getNettoSumme(col: Collection<RechnungPosInfo>?): BigDecimal {
            var nettoSumme = BigDecimal.ZERO
            if (col != null && col.size > 0) {
                for (pos in col) {
                    nettoSumme = nettoSumme.add(pos.netSum)
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
