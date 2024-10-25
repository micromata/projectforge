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
import jakarta.persistence.criteria.JoinType
import org.apache.commons.collections4.CollectionUtils
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.fibu.AuftragAndRechnungDaoHelper.createCriterionForPeriodOfPerformance
import org.projectforge.business.fibu.AuftragsPositionsPaymentTypeFilter.Companion.create
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserRightId
import org.projectforge.common.i18n.MessageParam
import org.projectforge.common.i18n.MessageParamType
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.I18nHelper.getLocalizedMessage
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.between
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ge
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isIn
import org.projectforge.framework.persistence.api.QueryFilter.Companion.le
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.persistence.history.DisplayHistoryConvertContext
import org.projectforge.framework.persistence.history.FlatHistoryFormatService
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.history.HistoryFormatUtils
import org.projectforge.framework.persistence.utils.SQLHelper.getYearsByTupleOfLocalDate
import org.projectforge.framework.utils.NumberHelper.parseInteger
import org.projectforge.framework.utils.NumberHelper.parseShort
import org.projectforge.framework.xmlstream.XmlObjectWriter
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*
import java.util.stream.Collectors

@Service
open class AuftragDao : BaseDao<AuftragDO>(AuftragDO::class.java) {
    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var flatHistoryFormatService: FlatHistoryFormatService

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var sendMail: SendMail

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var userDao: UserDao

    // Not autowired (due to cyclic dependency).
    private lateinit var taskTree: TaskTree

    override val additionalHistorySearchDOs: Array<Class<*>>
        get() = ADDITIONAL_HISTORY_SEARCH_DOS

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    init {
        userRightId = USER_RIGHT_ID
    }

    /**
     * Could not use injection by spring, because TaskTree is already injected in AuftragDao.
     *
     * @param taskTree
     */
    fun registerTaskTree(taskTree: TaskTree) {
        this.taskTree = taskTree
    }

    val years: IntArray
        /**
         * List of all years with invoices: select min(datum), max(datum) from t_fibu_rechnung.
         *
         * @return
         */
        get() {
            val minMaxDate = persistenceService.selectNamedSingleResult(
                AuftragDO.SELECT_MIN_MAX_DATE,
                Tuple::class.java
            )
            return getYearsByTupleOfLocalDate(minMaxDate)
        }

    val taskReferences: Map<Long?, MutableSet<AuftragsPositionVO>>
        /**
         * @return Map with all order positions referencing a task. The key of the map is the task id.
         */
        get() {
            val result: MutableMap<Long?, MutableSet<AuftragsPositionVO>> = HashMap()
            val list = persistenceService.executeQuery(
                "from AuftragsPositionDO a where a.task.id is not null and a.deleted = false",
                AuftragsPositionDO::class.java,
            )
            for (pos in list) {
                if (pos.taskId == null) {
                    log.error(
                        "Oups, should not occur, that in getTaskReference a order position without a task reference is found."
                    )
                    continue
                }
                val vo = AuftragsPositionVO(pos)
                var set = result[pos.taskId]
                if (set == null) {
                    set = TreeSet()
                    result[pos.taskId] = set
                }
                set.add(vo)
            }
            return result
        }

    fun buildStatistik(list: List<AuftragDO>?): AuftragsStatistik {
        val stats = AuftragsStatistik()
        if (list == null) {
            return stats
        }
        for (auftrag in list) {
            stats.add(auftrag)
        }
        return stats
    }

    /**
     * @param auftrag
     * @param contactPersonId If null, then contact person will be set to null;
     */
    fun setContactPerson(auftrag: AuftragDO, contactPersonId: Long?) {
        if (contactPersonId == null) {
            auftrag.contactPerson = null
        } else {
            val contactPerson = userDao.findOrLoad(contactPersonId)
            auftrag.contactPerson = contactPerson
        }
    }

    /**
     * @param position
     * @param taskId
     */
    fun setTask(position: AuftragsPositionDO, taskId: Long) {
        val task = taskDao.findOrLoad(taskId)
        position.task = task
    }

    /**
     * @param auftrag
     * @param kundeId If null, then kunde will be set to null;
     * @see BaseDao.findOrLoad
     */
    fun setKunde(auftrag: AuftragDO, kundeId: Long) {
        val kunde = kundeDao.findOrLoad(kundeId)
        auftrag.kunde = kunde
    }

    /**
     * @param auftrag
     * @param projektId If null, then projekt will be set to null;
     * @see BaseDao.findOrLoad
     */
    fun setProjekt(auftrag: AuftragDO, projektId: Long) {
        val projekt = projektDao.findOrLoad(projektId)
        auftrag.projekt = projekt
    }

    /**
     * @param posString Format ###.## (&lt;order number&gt;.&lt;position number&gt;).
     */
    fun getAuftragsPosition(posString: String?): AuftragsPositionDO? {
        if (posString == null) {
            return null
        }
        val sep = posString.indexOf('.')
        if (sep <= 0 || sep + 1 >= posString.length) {
            return null
        }
        val auftragsNummer = parseInteger(posString.substring(0, posString.indexOf('.')))
        val positionNummer = parseShort(posString.substring(posString.indexOf('.') + 1))
        if (auftragsNummer == null || positionNummer == null) {
            log.info("Cannot parse order number (format ###.## expected: $posString")
            return null
        }
        val auftrag = persistenceService.selectNamedSingleResult(
            AuftragDO.FIND_BY_NUMMER,
            AuftragDO::class.java,
            Pair("nummer", auftragsNummer),
        )
        return if (auftrag != null) auftrag.getPosition(positionNummer) else null
    }

    fun getAuftragsPosition(id: Long?): AuftragsPositionDO? {
        return persistenceService.selectNamedSingleResult(
            AuftragsPositionDO.FIND_BY_ID,
            AuftragsPositionDO::class.java,
            Pair("id", id),
        )
    }

    override fun select(filter: BaseSearchFilter, checkAccess: Boolean): List<AuftragDO> {
        super.select(filter, checkAccess)
        val myFilter = if (filter is AuftragFilter) {
            filter
        } else {
            AuftragFilter(filter)
        }

        val queryFilter = QueryFilter(myFilter)

        var positionStatusAlreadyFilterd = false
        queryFilter.createJoin("positionen")
        if (myFilter.auftragFakturiertFilterStatus == AuftragFakturiertFilterStatus.ZU_FAKTURIEREN) {
            // Show all orders to be invoiced (ignore status values on orders and their positions).
            queryFilter.createJoin("paymentSchedules", JoinType.LEFT)
            queryFilter.add(
                or(
                    eq("auftragsStatus", AuftragsStatus.ABGESCHLOSSEN),
                    eq("positionen.status", AuftragsPositionsStatus.ABGESCHLOSSEN),
                    eq("paymentSchedules.reached", true)
                )
            )
        } else {
            addCriterionForAuftragsStatuses(myFilter, queryFilter)
            positionStatusAlreadyFilterd = true
        }

        if (myFilter.user != null) {
            queryFilter.add(
                or(
                    eq("contactPerson", myFilter.user!!),
                    eq("projectManager", myFilter.user!!),
                    eq("headOfBusinessManager", myFilter.user!!),
                    eq("salesManager", myFilter.user!!)
                )
            )
        }
        if (CollectionUtils.isNotEmpty(myFilter.projectList)) {
            queryFilter.add(isIn<Any?>("projekt", myFilter.projectList))
        }

        createCriterionForErfassungsDatum(myFilter).ifPresent { predicate: DBPredicate? ->
            queryFilter.add(
                predicate!!
            )
        }

        createCriterionForPeriodOfPerformance(myFilter).ifPresent { predicate: DBPredicate? ->
            queryFilter.add(
                predicate!!
            )
        }

        queryFilter.addOrder(desc("nummer"))

        var list = select(queryFilter, checkAccess)

        list = myFilter.filterFakturiert(list)

        if (myFilter.auftragFakturiertFilterStatus != AuftragFakturiertFilterStatus.ZU_FAKTURIEREN) {
            // Don't use filter for orders to be invoiced.
            list = list.toMutableList() // Make mutable list of Kotlin's immutable list.
            filterPositionsArten(myFilter, list)
            if (!positionStatusAlreadyFilterd) { // Don't filter position status' again.
                filterPositionsStatus(myFilter, list)
            }
            filterPositionsPaymentTypes(myFilter, list)
        }

        return list
    }

    private fun addCriterionForAuftragsStatuses(myFilter: AuftragFilter, queryFilter: QueryFilter) {
        val auftragsStatuses: Collection<AuftragsStatus?> = myFilter.auftragsStatuses
        if (CollectionUtils.isEmpty(auftragsStatuses)) {
            // nothing to do
            return
        }
        val orCriterions: MutableList<DBPredicate> = ArrayList()

        orCriterions.add(isIn<Any>("auftragsStatus", auftragsStatuses))

        orCriterions.add(isIn<Any>("positionen.status", myFilter.auftragsPositionStatuses))

        queryFilter.add(or(*orCriterions.toTypedArray<DBPredicate>()))

        // check deleted
        if (!myFilter.isIgnoreDeleted) {
            queryFilter.add(eq("positionen.deleted", myFilter.isDeleted))
        }
    }

    private fun createCriterionForErfassungsDatum(myFilter: AuftragFilter): Optional<DBPredicate> {
        val startDate = myFilter.startDate
        val endDate = myFilter.endDate

        if (startDate != null && endDate != null) {
            return Optional.of(
                between("erfassungsDatum", startDate, endDate)
            )
        }

        if (startDate != null) {
            return Optional.of(
                ge("erfassungsDatum", startDate)
            )
        }

        if (endDate != null) {
            return Optional.of(
                le("erfassungsDatum", endDate)
            )
        }

        return Optional.empty()
    }

    private fun filterPositionsArten(myFilter: AuftragFilter, list: List<AuftragDO>) {
        if (CollectionUtils.isEmpty(myFilter.auftragsPositionsArten)) {
            return
        }
        val artFilter = AuftragsPositionsArtFilter(myFilter.auftragsPositionsArten)
        CollectionUtils.filter(
            list
        ) { `object`: AuftragDO? ->
            artFilter.match(
                list.toMutableList(),
                `object`!!
            )
        }
    }

    private fun filterPositionsStatus(myFilter: AuftragFilter, list: List<AuftragDO>) {
        if (CollectionUtils.isEmpty(myFilter.auftragsPositionStatuses)) {
            return
        }
        val statusFilter = AuftragsPositionsStatusFilter(myFilter.auftragsPositionStatuses)
        CollectionUtils.filter(
            list
        ) { `object`: AuftragDO? ->
            statusFilter.match(
                list.toMutableList(),
                `object`!!
            )
        }
    }

    private fun filterPositionsPaymentTypes(myFilter: AuftragFilter, list: List<AuftragDO>) {
        if (myFilter.auftragsPositionsPaymentType == null) {
            return
        }
        val paymentTypeFilter = create(myFilter.auftragsPositionsPaymentType)
        CollectionUtils.filter(
            list
        ) { `object`: AuftragDO? ->
            paymentTypeFilter.match(
                list.toMutableList(),
                `object`!!
            )
        }
    }

    override fun onInsertOrModify(obj: AuftragDO, operationType: OperationType) {
        if (obj.nummer == null) {
            throw UserException(
                "validation.required.valueNotPresent",
                MessageParam("fibu.auftrag.nummer", MessageParamType.I18N_KEY)
            )
        }
        if (obj.id == null) {
            // Neuer Auftrag/Angebot
            val next = getNextNumber(obj)
            if (next != obj.nummer) {
                throw UserException("fibu.auftrag.error.nummerIstNichtFortlaufend")
            }
        } else {
            val other = persistenceService.selectNamedSingleResult(
                AuftragDO.FIND_OTHER_BY_NUMMER,
                AuftragDO::class.java,
                Pair("nummer", obj.nummer),
                Pair("id", obj.id),
            )
            if (other != null) {
                throw UserException("fibu.auftrag.error.nummerBereitsVergeben")
            }
        }
        if (obj.positionen.isNullOrEmpty()) {
            throw UserException("fibu.auftrag.error.auftragHatKeinePositionen")
        }
        val positionen = obj.positionen
        if (!positionen.isNullOrEmpty()) {
            val size = positionen.size
            for (i in size - 1 downTo 1) {
                // Don't remove first position, remove only the last empty positions.
                val position = positionen[i]
                if (position.id == null && position.isEmpty) {
                    positionen.removeAt(i)
                } else {
                    break
                }
            }
        }
        positionen?.forEach { position ->
            position.checkVollstaendigFakturiert()
        }
        val uiStatusAsXml = XmlObjectWriter.writeAsXml(obj.getUiStatus())
        obj.uiStatusAsXml = uiStatusAsXml
        val paymentSchedules: List<PaymentScheduleDO>? = obj.paymentSchedules
        val pmSize = paymentSchedules?.size ?: -1
        if (pmSize > 1) {
            for (i in pmSize - 1 downTo 1) {
                // Don't remove first payment schedule, remove only the last empty payment schedules.
                val schedule = obj.paymentSchedules!![i]
                if (schedule.id == null && schedule.isEmpty) {
                    obj.paymentSchedules!!.removeAt(i)
                } else {
                    break
                }
            }
        }
        validateDatesInPaymentScheduleWithinPeriodOfPerformanceOfPosition(obj)
        validateAmountsInPaymentScheduleNotGreaterThanNetSumOfPosition(obj)
    }

    fun validateDatesInPaymentScheduleWithinPeriodOfPerformanceOfPosition(auftrag: AuftragDO) {
        val paymentSchedules = auftrag.paymentSchedulesExcludingDeleted
        val positionsWithDatesNotWithinPop: MutableList<Short> = ArrayList()
        for (pos in auftrag.positionenExcludingDeleted) {
            val periodOfPerformanceBegin =
                if (pos.hasOwnPeriodOfPerformance()) pos.periodOfPerformanceBegin else auftrag.periodOfPerformanceBegin
            var periodOfPerformanceEnd =
                if (pos.hasOwnPeriodOfPerformance()) pos.periodOfPerformanceEnd else auftrag.periodOfPerformanceEnd
            if (periodOfPerformanceEnd != null) {
                // Payments milestones are allowed inside period of performance plus 3 months:
                periodOfPerformanceEnd = periodOfPerformanceEnd.plusMonths(3)
            }
            val lastInvoiceDate = periodOfPerformanceEnd

            /*
            Java:
           final boolean hasDateNotInRange = paymentSchedules.stream()
          .filter(payment -> payment.getPositionNumber() != null && payment.getPositionNumber() == pos.getNumber())
          .map(PaymentScheduleDO::getScheduleDate)
          .filter(Objects::nonNull)
          .anyMatch(date -> (periodOfPerformanceBegin != null && date.isBefore(periodOfPerformanceBegin))
              || (lastInvoiceDate != null && date.isAfter(lastInvoiceDate)));

             */

            val hasDateNotInRange = paymentSchedules.filter { payment ->
                payment.positionNumber != null
                        && payment.positionNumber == pos.number
                        && payment.scheduleDate != null
            }.any { payment ->
                val date = payment.scheduleDate!!
                (periodOfPerformanceBegin != null && date.isBefore(periodOfPerformanceBegin))
                        || (lastInvoiceDate != null && date.isAfter(lastInvoiceDate))
            }

            if (hasDateNotInRange) {
                positionsWithDatesNotWithinPop.add(pos.number)
            }
        }

        if (!positionsWithDatesNotWithinPop.isEmpty()) {
            val positions = positionsWithDatesNotWithinPop.stream()
                .map { obj: Short -> obj.toString() }
                .collect(Collectors.joining(", "))

            throw UserException(
                "fibu.auftrag.error.datesInPaymentScheduleNotWithinPeriodOfPerformanceOfPosition",
                positions
            )
        }
    }

    override fun afterLoad(obj: AuftragDO) {
        auftragsCache.setOrderInfo(obj)
    }

    override fun afterUpdate(obj: AuftragDO, dbObj: AuftragDO?, isModified: Boolean) {
        auftragsCache.setExpired(obj)
    }

    fun validateAmountsInPaymentScheduleNotGreaterThanNetSumOfPosition(auftrag: AuftragDO) {
        val paymentSchedules = auftrag.paymentSchedulesExcludingDeleted

        for (pos in auftrag.positionenExcludingDeleted) {
            /*val sumOfAmountsForCurrentPosition = paymentSchedules.stream()
                .filter { payment: PaymentScheduleDO -> payment.positionNumber == pos.number }
                .map(PaymentScheduleDO::amount)
                .filter { obj: BigDecimal? -> Objects.nonNull(obj) }
                .reduce(BigDecimal.ZERO) { obj: BigDecimal, augend: BigDecimal? -> obj.add(augend) }  // sum
            */
            val sumOfAmountsForCurrentPosition =
                paymentSchedules.filter { payment -> payment.positionNumber == pos.number && payment.amount != null }
                    .sumOf { payment -> payment.amount ?: BigDecimal.ZERO }

            val netSum = pos.nettoSumme
            if (netSum != null && netSum.compareTo(BigDecimal.ZERO) > 0 && sumOfAmountsForCurrentPosition.compareTo(
                    netSum
                ) > 0
            ) {
                // Only for positive netSum's:
                throw UserException(
                    "fibu.auftrag.error.amountsInPaymentScheduleAreGreaterThanNetSumOfPosition",
                    pos.number
                )
            }
        }
    }

    override fun afterInsertOrModify(obj: AuftragDO, operationType: OperationType) {
        taskTree.refreshOrderPositionReferences()
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDao.prepareHibernateSearch
     */
    override fun prepareHibernateSearch(obj: AuftragDO, operationType: OperationType) {
        projektDao.initializeProjektManagerGroup(obj.projekt)
    }

    /**
     * Sends an e-mail to the projekt manager if exists and is not equals to the logged in user.
     *
     * @param auftrag
     * @param operationType
     * @return
     */
    fun sendNotificationIfRequired(
        auftrag: AuftragDO, operationType: OperationType,
        requestUrl: String?
    ): Boolean {
        if (!configurationService.isSendMailConfigured) {
            return false
        }
        val contactPerson = auftrag.contactPerson ?: return false
        if (!hasAccess(contactPerson, auftrag, null, OperationType.SELECT, false)) {
            return false
        }
        val data: MutableMap<String, Any?> = HashMap()
        data["contactPerson"] = contactPerson
        data["auftrag"] = auftrag
        data["requestUrl"] = requestUrl
        data["history"] = flatHistoryFormatService.selectHistoryEntriesAndConvert(this, auftrag).take(10)
        val msg = Mail()
        msg.setTo(contactPerson)
        val subject = if (operationType == OperationType.INSERT) {
            "Auftrag #" + auftrag.nummer + " wurde angelegt."
        } else if (operationType == OperationType.DELETE) {
            "Auftrag #" + auftrag.nummer + " wurde gelöscht."
        } else {
            "Auftrag #" + auftrag.nummer + " wurde geändert."
        }
        msg.setProjectForgeSubject(subject)
        data["subject"] = subject
        val content = sendMail.renderGroovyTemplate(
            msg,
            "mail/orderChangeNotification.html",
            data,
            getLocalizedMessage("fibu.auftrag"),
            contactPerson
        )
        msg.content = content
        msg.contentType = Mail.CONTENTTYPE_HTML
        return sendMail.send(msg, null, null)
    }

    val nextNumber: Int
        /**
         * Gets the highest Auftragsnummer.
         */
        get() = getNextNumber(null)

    /**
     * Gets the highest Auftragsnummer.
     *
     * @param auftrag wird benötigt, damit geschaut werden kann, ob dieser Auftrag ggf. schon existiert. Wenn er schon
     * eine Nummer hatte, so kann verhindert werden, dass er eine nächst höhere Nummer bekommt. Ein solcher
     * Auftrag bekommt die alte Nummer wieder zugeordnet.
     */
    fun getNextNumber(auftrag: AuftragDO?): Int {
        if (auftrag?.id != null) {
            val orig = find(auftrag.id, checkAccess = false)
            if (orig!!.nummer != null) {
                auftrag.nummer = orig.nummer
                return orig.nummer!!
            }
        }
        // val list: List<Int?> = em.createQuery("select max(t.nummer) from AuftragDO t").getResultList()
        return persistenceService.getNextNumber("AuftragDO", "nummer", START_NUMBER)
    }

    override fun mergeHistoryEntries(
        obj: AuftragDO,
        list: MutableList<HistoryEntryDO>,
        context: DisplayHistoryConvertContext<*>
    ) {
        obj.positionenIncludingDeleted?.forEach { position ->
            val entries = historyService.loadHistory(position)
            entries.forEach { entry ->
                HistoryFormatUtils.setPropertyNameForListEntries(entry, prefix = "pos", number = position.number)
            }
            mergeHistoryEntries(list, entries)
        }
        obj.paymentSchedules?.forEach { schedule ->
            val entries = historyService.loadHistory(schedule)
            entries.forEach { entry ->
                HistoryFormatUtils.setPropertyNameForListEntries(
                    entry,
                    prefix = "paymentSchedule",
                    number = schedule.number
                )
            }
            mergeHistoryEntries(list, entries)
        }
    }

    /**
     * Returns also true, if idSet contains the id of any order position.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.contains
     */
    override fun contains(idSet: Set<Long>?, entry: AuftragDO): Boolean {
        idSet ?: return false
        if (super.contains(idSet, entry)) {
            return true
        }
        for (pos in entry.positionenIncludingDeleted!!) {
            if (idSet.contains(pos.id)) {
                return true
            }
        }
        return false
    }

    override fun copyValues(src: AuftragDO, dest: AuftragDO, vararg ignoreFields: String): EntityCopyStatus? {
        return super.copyValues(src, dest, "uiStatus", "fakturiertSum")
    }

    override fun newInstance(): AuftragDO {
        return AuftragDO()
    }

    companion object {
        val USER_RIGHT_ID: UserRightId = UserRightId.PM_ORDER_BOOK

        const val START_NUMBER: Int = 1

        private val log: Logger = LoggerFactory.getLogger(AuftragDao::class.java)

        val ADDITIONAL_HISTORY_SEARCH_DOS: Array<Class<*>> = arrayOf(AuftragsPositionDO::class.java)

        val ADDITIONAL_SEARCH_FIELDS = arrayOf(
            "contactPerson.username",
            "contactPerson.firstname",
            "contactPerson.lastname", "kunde.name", "projekt.name", "projekt.kunde.name", "positionen.position",
            "positionen.art",
            "positionen.status", "positionen.titel", "positionen.bemerkung", "positionen.nettoSumme"
        )
    }
}
