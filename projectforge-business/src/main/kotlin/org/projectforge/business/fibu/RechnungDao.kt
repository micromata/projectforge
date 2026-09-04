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
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightServiceImpl
import org.projectforge.business.user.UserRightValue
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
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.utils.SQLHelper.getYearsByTupleOfLocalDate
import org.projectforge.framework.time.PFDateTime.Companion.from
import org.projectforge.framework.time.PFDateTime.Companion.now
import org.projectforge.framework.xmlstream.XmlObjectWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

@Service
open class RechnungDao : BaseDao<RechnungDO>(RechnungDO::class.java) {
    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var projektCache: ProjektCache

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

    override val additionalHistoryEntityClasses: List<Class<*>> =
        listOf(RechnungsPositionDO::class.java, KostZuweisungDO::class.java)

    init {
        userRightId = USER_RIGHT_ID
    }

    override fun isAutocompletionPropertyEnabled(property: String?): Boolean {
        return property == "kundeText"
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
     * Touching them is the whole point: both collections are lazy, and a caller outside the transaction
     * would get a `LazyInitializationException` instead. Null safe throughout, because an unknown id must
     * answer null and let the caller report a 404 - not throw and turn into a 500.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.find
     */
    @Throws(AccessException::class)
    override fun find(id: Serializable?, checkAccess: Boolean, attached: Boolean): RechnungDO? {
        val rechnung = super.find(id, checkAccess = checkAccess, attached = attached)
        rechnung?.positionen?.forEach { it.kostZuweisungen?.size }
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
        // Batch the children's history (one query per child class) instead of once per instance, see
        // HistoryService.loadAndMergeHistory(entityClass, entityIds, ...). The kostZuweisungen of all positions are
        // aggregated into a single call. Display prefixes are resolved post-hoc via getHistoryPropertyPrefix.
        val positionen = obj.positionen
        positionen?.mapNotNull { it.id }?.takeIf { it.isNotEmpty() }?.let { ids ->
            historyService.loadAndMergeHistory(RechnungsPositionDO::class.java, ids, context)
        }
        positionen?.flatMap { it.kostZuweisungen ?: emptyList() }?.mapNotNull { it.id }?.takeIf { it.isNotEmpty() }
            ?.let { ids ->
                historyService.loadAndMergeHistory(KostZuweisungDO::class.java, ids, context)
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

    /**
     * In addition to the invoice right (FIBU_AUSGANGSRECHNUNGEN), users with order book access
     * ([UserRightId.PM_ORDER_BOOK]) may open the outgoing invoice list. They only get to see the invoices
     * linked to an order they may see, filtered per row in [hasUserSelectAccess] (obj form). Read-only:
     * insert/update/delete still require the invoice right.
     */
    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        if (super.hasUserSelectAccess(user, false)) {
            return true
        }
        if (hasOrderBookSelectAccess(user)) {
            // Order book users may open the (per-row filtered) list.
            return true
        }
        // Delegate to super to throw the standard AccessException if requested.
        return super.hasUserSelectAccess(user, throwException)
    }

    /**
     * Grants read-only access to a single outgoing invoice for order book users if it is linked to an order
     * the user may see and isn't older than [MAX_YEARS_OF_VISIBILITY_4_ORDER_BOOK_USER] years. This is also
     * the per-row filter of the list query (see [org.projectforge.framework.persistence.api.impl.DBQuery]).
     */
    override fun hasUserSelectAccess(user: PFUserDO, obj: RechnungDO, throwException: Boolean): Boolean {
        if (super.hasUserSelectAccess(user, obj, false)) {
            return true
        }
        if (hasOrderBookUserReadAccess(user, obj)) {
            return true
        }
        // Delegate to super to throw the standard AccessException if requested.
        return super.hasUserSelectAccess(user, obj, throwException)
    }

    /**
     * @return true if the user has (at least read-only) access to the order book ([UserRightId.PM_ORDER_BOOK]).
     */
    private fun hasOrderBookSelectAccess(user: PFUserDO): Boolean {
        return accessChecker.hasRight(
            user, AuftragDao.USER_RIGHT_ID,
            UserRightValue.READONLY, UserRightValue.PARTLYREADWRITE, UserRightValue.READWRITE
        )
    }

    /**
     * Read-only fallback for order book users: the invoice must be linked (via one of its positions ->
     * order position -> order) to an order the user is allowed to see, and it must not be older than
     * [MAX_YEARS_OF_VISIBILITY_4_ORDER_BOOK_USER] years.
     */
    private fun hasOrderBookUserReadAccess(user: PFUserDO, obj: RechnungDO?): Boolean {
        obj ?: return false
        if (!hasOrderBookSelectAccess(user)) {
            return false
        }
        val datum = obj.datum ?: return false
        if (datum.isBefore(LocalDate.now().minusYears(MAX_YEARS_OF_VISIBILITY_4_ORDER_BOOK_USER.toLong()))) {
            return false
        }
        // An invoice position doesn't know its order directly; resolve it through the caches (no lazy init).
        val auftragIds = rechnungCache.getRechnungInfo(obj.id)?.positions
            ?.mapNotNull { auftragsCache.getOrderPositionInfo(it.auftragsPositionId)?.auftragId }
            ?.distinct()
            ?: return false
        for (auftragId in auftragIds) {
            val orderInfo = auftragsCache.getOrderInfo(auftragId) ?: continue
            if (hasOrderSelectAccess(user, orderInfo)) {
                return true
            }
        }
        return false
    }

    /**
     * Cache-only reimplementation of [AuftragRight.hasAccess] for [OperationType.SELECT]. This is called
     * once per candidate invoice row of the list, so it must not touch the database: loading the order via
     * [AuftragDao.find] (and letting [AuftragRight.hasAccess] initialize the order's positions and payment
     * schedules) turned the list into an N+1 storm. All fields the SELECT path needs are available from the
     * [AuftragsCache] order info and the [ProjektCache] project (whose *Id getters read the foreign key
     * without initializing the lazy association).
     */
    private fun hasOrderSelectAccess(user: PFUserDO, orderInfo: OrderInfo): Boolean {
        if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.CONTROLLING_GROUP)) {
            return true
        }
        if (!hasOrderBookSelectAccess(user)) {
            return false
        }
        if (accessChecker.isUserMemberOfGroup(user, *UserRightServiceImpl.FIBU_ORGA_GROUPS)
            && accessChecker.hasRight(user, AuftragDao.USER_RIGHT_ID, UserRightValue.READONLY, UserRightValue.READWRITE)
        ) {
            // Members of the finance/orga groups with (at least) read access to the order book see every order.
            return true
        }
        // Otherwise the user must be tied to the order as contact person or through the project's manager group.
        var hasAccess = false
        if (accessChecker.userEquals(user, orderInfo.contactPerson)) {
            hasAccess = true
        }
        projektCache.getProjekt(orderInfo.projektId)?.let { projekt ->
            if (UserGroupCache.getInstance().isUserMemberOfGroup(user.id, projekt.projektManagerGroupId)
                || projekt.headOfBusinessManagerId == user.id
                || projekt.salesManagerId == user.id
            ) {
                hasAccess = true
            }
        }
        if (!hasAccess) {
            return false
        }
        if (!orderInfo.isVollstaendigFakturiert) {
            return true
        }
        // Fully invoiced orders stay visible for a while, keyed off the performance/offer date (see AuftragRight).
        val endDate = orderInfo.periodOfPerformanceEnd ?: orderInfo.angebotsDatum ?: return false
        return LocalDate.now().toEpochDay() - endDate.toEpochDay() <= AuftragRight.MAX_DAYS_OF_VISIBILITY_4_PROJECT_MANGER
    }

    companion object {
        @JvmField
        val USER_RIGHT_ID: UserRightId = UserRightId.FIBU_AUSGANGSRECHNUNGEN

        /**
         * Outgoing invoices older than this number of years won't be visible for order book users
         * (users with [UserRightId.PM_ORDER_BOOK] but without the invoice right).
         */
        const val MAX_YEARS_OF_VISIBILITY_4_ORDER_BOOK_USER: Int = 2

        const val START_NUMBER: Int = 1000

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
