/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.MessageParam;
import org.projectforge.framework.i18n.MessageParamType;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.api.impl.DBPredicate;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.framework.xstream.XmlObjectWriter;
import org.projectforge.mail.Mail;
import org.projectforge.mail.SendMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.Tuple;
import javax.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class AuftragDao extends BaseDao<AuftragDO> {
  public static final UserRightId USER_RIGHT_ID = UserRightId.PM_ORDER_BOOK;

  public final static int START_NUMBER = 1;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuftragDao.class);

  private static final Class<?>[] ADDITIONAL_HISTORY_SEARCH_DOS = new Class[]{AuftragsPositionDO.class};

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"contactPerson.username",
          "contactPerson.firstname",
          "contactPerson.lastname", "kunde.name", "projekt.name", "projekt.kunde.name", "positionen.position",
          "positionen.art",
          "positionen.status", "positionen.titel", "positionen.bemerkung", "positionen.nettoSumme"};

  @Autowired
  private UserDao userDao;

  @Autowired
  private KundeDao kundeDao;

  @Autowired
  private ProjektDao projektDao;

  @Autowired
  private SendMail sendMail;

  private Integer abgeschlossenNichtFakturiert;

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private RechnungCache rechnungCache;

  @Autowired
  private ConfigurationService configurationService;

  private TaskTree taskTree;

  public AuftragDao() {
    super(AuftragDO.class);
    userRightId = USER_RIGHT_ID;
  }

  /**
   * Could not use injection by spring, because TaskTree is already injected in AuftragDao.
   *
   * @param taskTree
   */
  public void registerTaskTree(final TaskTree taskTree) {
    this.taskTree = taskTree;
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * List of all years with invoices: select min(datum), max(datum) from t_fibu_rechnung.
   *
   * @return
   */
  public int[] getYears() {
    final Tuple minMaxDate = SQLHelper.ensureUniqueResult(em.createNamedQuery(AuftragDO.SELECT_MIN_MAX_DATE, Tuple.class));
    return SQLHelper.getYears((java.sql.Date) minMaxDate.get(0), (java.sql.Date) minMaxDate.get(1));
  }

  /**
   * @return Map with all order positions referencing a task. The key of the map is the task id.
   */
  public Map<Integer, Set<AuftragsPositionVO>> getTaskReferences() {
    final Map<Integer, Set<AuftragsPositionVO>> result = new HashMap<>();
    final List<AuftragsPositionDO> list = em.createQuery(
            "from AuftragsPositionDO a where a.task.id is not null and a.deleted = false",
            AuftragsPositionDO.class).getResultList();
    if (list == null) {
      return result;
    }
    for (final AuftragsPositionDO pos : list) {
      if (pos.getTaskId() == null) {
        log.error(
                "Oups, should not occur, that in getTaskReference a order position without a task reference is found.");
        continue;
      }
      final AuftragsPositionVO vo = new AuftragsPositionVO(pos);
      Set<AuftragsPositionVO> set = result.get(pos.getTaskId());
      if (set == null) {
        set = new TreeSet<>();
        result.put(pos.getTaskId(), set);
      }
      set.add(vo);
    }
    return result;
  }

  public AuftragsStatistik buildStatistik(final List<AuftragDO> list) {
    final AuftragsStatistik stats = new AuftragsStatistik();
    if (list == null) {
      return stats;
    }
    for (final AuftragDO auftrag : list) {
      calculateInvoicedSum(auftrag);
      stats.add(auftrag);
    }
    return stats;
  }

  /**
   * Get all invoices and set the field fakturiertSum for every order of the given col.
   *
   * @param col
   * @see RechnungCache#getRechnungsPositionVOSetByAuftragsPositionId(Integer)
   */
  public void calculateInvoicedSum(final Collection<AuftragDO> col) {
    if (col == null) {
      return;
    }
    for (final AuftragDO auftrag : col) {
      calculateInvoicedSum(auftrag);
    }
  }

  /**
   * Get all invoices and set the field fakturiertSum for the given order.
   *
   * @param order
   * @see RechnungCache#getRechnungsPositionVOSetByAuftragsPositionId(Integer)
   */
  public void calculateInvoicedSum(final AuftragDO order) {
    if (order == null) {
      return;
    }
    if (order.getPositionenExcludingDeleted() != null) {
      for (final AuftragsPositionDO pos : order.getPositionenExcludingDeleted()) {
        final Set<RechnungsPositionVO> set = rechnungCache
                .getRechnungsPositionVOSetByAuftragsPositionId(pos.getId());
        if (set != null) {
          pos.setFakturiertSum(RechnungDao.getNettoSumme(set));
        }
      }
    }
  }

  /**
   * @param auftrag
   * @param contactPersonId If null, then contact person will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setContactPerson(final AuftragDO auftrag, final Integer contactPersonId) {
    if (contactPersonId == null) {
      auftrag.setContactPerson(null);
    } else {
      final PFUserDO contactPerson = userDao.getOrLoad(contactPersonId);
      auftrag.setContactPerson(contactPerson);
    }
  }

  /**
   * @param position
   * @param taskId
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setTask(final AuftragsPositionDO position, final Integer taskId) {
    final TaskDO task = taskDao.getOrLoad(taskId);
    position.setTask(task);
  }

  /**
   * @param auftrag
   * @param kundeId If null, then kunde will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setKunde(final AuftragDO auftrag, final Integer kundeId) {
    final KundeDO kunde = kundeDao.getOrLoad(kundeId);
    auftrag.setKunde(kunde);
  }

  /**
   * @param auftrag
   * @param projektId If null, then projekt will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setProjekt(final AuftragDO auftrag, final Integer projektId) {
    final ProjektDO projekt = projektDao.getOrLoad(projektId);
    auftrag.setProjekt(projekt);
  }

  /**
   * @param posString Format ###.## (&lt;order number&gt;.&lt;position number&gt;).
   */
  public AuftragsPositionDO getAuftragsPosition(final String posString) {
    Integer auftragsNummer;
    Short positionNummer;
    if (posString == null) {
      return null;
    }
    final int sep = posString.indexOf('.');
    if (sep <= 0 || sep + 1 >= posString.length()) {
      return null;
    }
    auftragsNummer = NumberHelper.parseInteger(posString.substring(0, posString.indexOf('.')));
    positionNummer = NumberHelper.parseShort(posString.substring(posString.indexOf('.') + 1));
    if (auftragsNummer == null || positionNummer == null) {
      log.info("Cannot parse order number (format ###.## expected: " + posString);
      return null;
    }
    final AuftragDO auftrag = SQLHelper.ensureUniqueResult(
            em
                    .createNamedQuery(AuftragDO.FIND_BY_NUMMER, AuftragDO.class)
                    .setParameter("nummer", auftragsNummer));
    return auftrag != null ? auftrag.getPosition(positionNummer) : null;
  }

  public synchronized int getAbgeschlossenNichtFakturiertAnzahl() {
    if (abgeschlossenNichtFakturiert != null) {
      return abgeschlossenNichtFakturiert;
    }
    final AuftragFilter filter = new AuftragFilter();
    filter.getAuftragsStatuses().add(AuftragsStatus.ABGESCHLOSSEN);
    filter.setAuftragFakturiertFilterStatus(AuftragFakturiertFilterStatus.NICHT_FAKTURIERT);
    try {
      final List<AuftragDO> list = getList(filter, false);
      abgeschlossenNichtFakturiert = list != null ? list.size() : 0;
      return abgeschlossenNichtFakturiert;
    } catch (final Exception ex) {
      log.error("Exception ocurred while getting number of closed and not invoiced orders: " + ex.getMessage(), ex);
      // Exception e. g. if data-base update is needed.
      return 0;
    }
  }

  @Override
  public List<AuftragDO> getList(final BaseSearchFilter filter) {
    return getList(filter, true);
  }

  private List<AuftragDO> getList(final BaseSearchFilter filter, final boolean checkAccess) {
    final AuftragFilter myFilter;
    if (filter instanceof AuftragFilter) {
      myFilter = (AuftragFilter) filter;
    } else {
      myFilter = new AuftragFilter(filter);
    }

    final QueryFilter queryFilter = new QueryFilter(myFilter);

    addCriterionForAuftragsStatuses(myFilter, queryFilter);

    if (myFilter.getUser() != null) {
      queryFilter.add(
              QueryFilter.or(
                      QueryFilter.eq("contactPerson", myFilter.getUser()),
                      QueryFilter.eq("projectManager", myFilter.getUser()),
                      QueryFilter.eq("headOfBusinessManager", myFilter.getUser()),
                      QueryFilter.eq("salesManager", myFilter.getUser())
              )
      );
    }

    createCriterionForErfassungsDatum(myFilter).ifPresent(queryFilter::add);

    AuftragAndRechnungDaoHelper.createCriterionForPeriodOfPerformance(myFilter).ifPresent(queryFilter::add);

    queryFilter.addOrder(SortProperty.desc("nummer"));

    List<AuftragDO> list;
    if (checkAccess) {
      list = getList(queryFilter);
    } else {
      list = internalGetList(queryFilter);
    }

    list = myFilter.filterFakturiert(list);

    filterPositionsArten(myFilter, list);

    if (myFilter.getAuftragsPositionsPaymentType() != null) {
      CollectionUtils.filter(list, object -> {
        final AuftragDO auftrag = (AuftragDO) object;
        boolean match = false;
        if (myFilter.getAuftragsPositionsPaymentType() != null) {
          if (CollectionUtils.isNotEmpty(auftrag.getPositionenExcludingDeleted())) {
            for (final AuftragsPositionDO position : auftrag.getPositionenExcludingDeleted()) {
              if (myFilter.getAuftragsPositionsPaymentType() == position.getPaymentType()) {
                match = true;
                break;
              }
            }
          }
        }
        return match;
      });
    }

    return list;
  }

  private void addCriterionForAuftragsStatuses(final AuftragFilter myFilter, final QueryFilter queryFilter) {
    final Collection<AuftragsStatus> auftragsStatuses = myFilter.getAuftragsStatuses();
    if (CollectionUtils.isEmpty(auftragsStatuses)) {
      // nothing to do
      return;
    }

    final List<DBPredicate> orCriterions = new ArrayList<>();
    orCriterions.add(QueryFilter.isIn("auftragsStatus", auftragsStatuses));

    queryFilter.createJoin("positionen")
            .createJoin("paymentSchedules", JoinType.LEFT);

    orCriterions.add(QueryFilter.isIn("positionen.status", myFilter.getAuftragsPositionStatuses()));

    // special case
    if (auftragsStatuses.contains(AuftragsStatus.ABGESCHLOSSEN)) {
      orCriterions.add(QueryFilter.eq("paymentSchedules.reached", true));
    }

    queryFilter.add(QueryFilter.or(orCriterions.toArray(new DBPredicate[orCriterions.size()])));

    // check deleted
    if (!myFilter.isIgnoreDeleted()) {
      queryFilter.add(QueryFilter.eq("positionen.deleted", myFilter.isDeleted()));
    }
  }

  private Optional<DBPredicate> createCriterionForErfassungsDatum(final AuftragFilter myFilter) {
    final java.sql.Date startDate = DateHelper.convertDateToSqlDateInTheUsersTimeZone(myFilter.getStartDate());
    final java.sql.Date endDate = DateHelper.convertDateToSqlDateInTheUsersTimeZone(myFilter.getEndDate());

    if (startDate != null && endDate != null) {
      return Optional.of(
              QueryFilter.between("erfassungsDatum", startDate, endDate)
      );
    }

    if (startDate != null) {
      return Optional.of(
              QueryFilter.ge("erfassungsDatum", startDate)
      );
    }

    if (endDate != null) {
      return Optional.of(
              QueryFilter.le("erfassungsDatum", endDate)
      );
    }

    return Optional.empty();
  }

  private void filterPositionsArten(final AuftragFilter myFilter, final List<AuftragDO> list) {
    final Collection<AuftragsPositionsArt> auftragsPositionsArten = myFilter.getAuftragsPositionsArten();

    if (CollectionUtils.isNotEmpty(auftragsPositionsArten)) {
      CollectionUtils.filter(list, object -> {
        final List<AuftragsPositionDO> positionen = ((AuftragDO) object).getPositionenExcludingDeleted();

        // check if any of the current positions contains at least one AuftragsPositionsArt of the auftragsPositionsArten of the filter
        return CollectionUtils.isNotEmpty(positionen) && positionen.stream()
                .map(AuftragsPositionDO::getArt)
                .anyMatch(positionsArt -> auftragsPositionsArten.stream().anyMatch(art -> art == positionsArt));
      });
    }
  }

  @Override
  protected void onSaveOrModify(final AuftragDO obj) {
    if (obj.getNummer() == null) {
      throw new UserException("validation.required.valueNotPresent",
              new MessageParam("fibu.auftrag.nummer", MessageParamType.I18N_KEY));
    }
    if (obj.getId() == null) {
      // Neuer Auftrag/Angebot
      final Integer next = getNextNumber(obj);
      if (next.intValue() != obj.getNummer().intValue()) {
        throw new UserException("fibu.auftrag.error.nummerIstNichtFortlaufend");
      }
    } else {
      final AuftragDO other = SQLHelper.ensureUniqueResult(em.createNamedQuery(AuftragDO.FIND_OTHER_BY_NUMMER, AuftragDO.class)
              .setParameter("nummer", obj.getNummer())
              .setParameter("id", obj.getId()));
      if (other != null) {
        throw new UserException("fibu.auftrag.error.nummerBereitsVergeben");
      }
    }
    if (CollectionUtils.isEmpty(obj.getPositionenIncludingDeleted())) {
      throw new UserException("fibu.auftrag.error.auftragHatKeinePositionen");
    }
    final int size = obj.getPositionenIncludingDeleted().size();
    for (int i = size - 1; i > 0; i--) {
      // Don't remove first position, remove only the last empty positions.
      final AuftragsPositionDO position = obj.getPositionenIncludingDeleted().get(i);
      if (position.getId() == null && position.isEmpty()) {
        obj.getPositionenIncludingDeleted().remove(i);
      } else {
        break;
      }
    }
    if (CollectionUtils.isNotEmpty(obj.getPositionenIncludingDeleted())) {
      for (final AuftragsPositionDO position : obj.getPositionenIncludingDeleted()) {
        position.checkVollstaendigFakturiert();
      }
    }
    abgeschlossenNichtFakturiert = null;
    final String uiStatusAsXml = XmlObjectWriter.writeAsXml(obj.getUiStatus());
    obj.setUiStatusAsXml(uiStatusAsXml);
    final List<PaymentScheduleDO> paymentSchedules = obj.getPaymentSchedules();
    final int pmSize = paymentSchedules != null ? paymentSchedules.size() : -1;
    if (pmSize > 1) {
      for (int i = pmSize - 1; i > 0; i--) {
        // Don't remove first payment schedule, remove only the last empty payment schedules.
        final PaymentScheduleDO schedule = obj.getPaymentSchedules().get(i);
        if (schedule.getId() == null && schedule.isEmpty()) {
          obj.getPaymentSchedules().remove(i);
        } else {
          break;
        }
      }
    }
    validateDatesInPaymentScheduleWithinPeriodOfPerformanceOfPosition(obj);
    validateAmountsInPaymentScheduleNotGreaterThanNetSumOfPosition(obj);
  }

  void validateDatesInPaymentScheduleWithinPeriodOfPerformanceOfPosition(final AuftragDO auftrag) {
    final List<PaymentScheduleDO> paymentSchedules = auftrag.getPaymentSchedules();
    if (paymentSchedules == null) {
      // if there are no payment schedules, there are no dates which are not within the period of performance
      return;
    }

    final List<Short> positionsWithDatesNotWithinPop = new ArrayList<>();
    for (final AuftragsPositionDO pos : auftrag.getPositionenExcludingDeleted()) {
      final Date periodOfPerformanceBegin = pos.hasOwnPeriodOfPerformance() ? pos.getPeriodOfPerformanceBegin() : auftrag.getPeriodOfPerformanceBegin();
      final Date periodOfPerformanceEnd = pos.hasOwnPeriodOfPerformance() ? pos.getPeriodOfPerformanceEnd() : auftrag.getPeriodOfPerformanceEnd();

      final boolean hasDateNotInRange = paymentSchedules.stream()
              .filter(payment -> payment.getPositionNumber() == pos.getNumber())
              .map(PaymentScheduleDO::getScheduleDate)
              .filter(Objects::nonNull)
              .anyMatch(date -> date.before(periodOfPerformanceBegin) || date.after(periodOfPerformanceEnd));

      if (hasDateNotInRange) {
        positionsWithDatesNotWithinPop.add(pos.getNumber());
      }
    }

    if (!positionsWithDatesNotWithinPop.isEmpty()) {
      final String positions = positionsWithDatesNotWithinPop.stream()
              .map(Object::toString)
              .collect(Collectors.joining(", "));

      throw new UserException("fibu.auftrag.error.datesInPaymentScheduleNotWithinPeriodOfPerformanceOfPosition", positions);
    }
  }

  void validateAmountsInPaymentScheduleNotGreaterThanNetSumOfPosition(final AuftragDO auftrag) {
    final List<PaymentScheduleDO> paymentSchedules = auftrag.getPaymentSchedules();
    if (paymentSchedules == null) {
      // if there are no payment schedules, there are no amounts which can be greater -> validation OK
      return;
    }

    for (final AuftragsPositionDO pos : auftrag.getPositionenExcludingDeleted()) {
      final BigDecimal sumOfAmountsForCurrentPosition = paymentSchedules.stream()
              .filter(payment -> payment.getPositionNumber() == pos.getNumber())
              .map(PaymentScheduleDO::getAmount)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add); // sum

      final BigDecimal netSum = pos.getNettoSumme();
      if (netSum != null && sumOfAmountsForCurrentPosition.compareTo(netSum) > 0) {
        throw new UserException("fibu.auftrag.error.amountsInPaymentScheduleAreGreaterThanNetSumOfPosition", pos.getNumber());
      }
    }
  }

  @Override
  protected void afterSaveOrModify(final AuftragDO obj) {
    super.afterSaveOrModify(obj);
    if (taskTree != null) {
      taskTree.refreshOrderPositionReferences();
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#prepareHibernateSearch(ExtendedBaseDO, OperationType)
   */
  @Override
  protected void prepareHibernateSearch(final AuftragDO obj, final OperationType operationType) {
    projektDao.initializeProjektManagerGroup(obj.getProjekt());
  }

  /**
   * Sends an e-mail to the projekt manager if exists and is not equals to the logged in user.
   *
   * @param auftrag
   * @param operationType
   * @return
   */

  public boolean sendNotificationIfRequired(final AuftragDO auftrag, final OperationType operationType,
                                            final String requestUrl) {
    if (!configurationService.isSendMailConfigured()) {
      return false;
    }
    final PFUserDO contactPerson = auftrag.getContactPerson();
    if (contactPerson == null) {
      return false;
    }
    if (!hasAccess(contactPerson, auftrag, null, OperationType.SELECT, false)) {
      return false;
    }
    final Map<String, Object> data = new HashMap<>();
    data.put("contactPerson", contactPerson);
    data.put("auftrag", auftrag);
    data.put("requestUrl", requestUrl);
    final List<DisplayHistoryEntry> history = getDisplayHistoryEntries(auftrag);
    final List<DisplayHistoryEntry> list = new ArrayList<>();
    int i = 0;
    for (final DisplayHistoryEntry entry : history) {
      list.add(entry);
      if (++i >= 10) {
        break;
      }
    }
    data.put("history", list);
    final Mail msg = new Mail();
    msg.setTo(contactPerson);
    final String subject;
    if (operationType == OperationType.INSERT) {
      subject = "Auftrag #" + auftrag.getNummer() + " wurde angelegt.";
    } else if (operationType == OperationType.DELETE) {
      subject = "Auftrag #" + auftrag.getNummer() + " wurde gelöscht.";
    } else {
      subject = "Auftrag #" + auftrag.getNummer() + " wurde geändert.";
    }
    msg.setProjectForgeSubject(subject);
    data.put("subject", subject);
    final String content = sendMail.renderGroovyTemplate(msg, "mail/orderChangeNotification.html", data, contactPerson);
    msg.setContent(content);
    msg.setContentType(Mail.CONTENTTYPE_HTML);
    return sendMail.send(msg, null, null);
  }

  /**
   * Gets the highest Auftragsnummer.
   */
  @SuppressWarnings("unchecked")
  public Integer getNextNumber() {
    return getNextNumber(null);
  }

  /**
   * Gets the highest Auftragsnummer.
   *
   * @param auftrag wird benötigt, damit geschaut werden kann, ob dieser Auftrag ggf. schon existiert. Wenn er schon
   *                eine Nummer hatte, so kann verhindert werden, dass er eine nächst höhere Nummer bekommt. Ein solcher
   *                Auftrag bekommt die alte Nummer wieder zugeordnet.
   */
  @SuppressWarnings("unchecked")
  public Integer getNextNumber(final AuftragDO auftrag) {
    if (auftrag != null && auftrag.getId() != null) {
      final AuftragDO orig = internalGetById(auftrag.getId());
      if (orig.getNummer() != null) {
        auftrag.setNummer(orig.getNummer());
        return orig.getNummer();
      }
    }
    final List<Integer> list = em.createQuery("select max(t.nummer) from AuftragDO t").getResultList();
    Validate.notNull(list);
    if (list.size() == 0 || list.get(0) == null) {
      log.info("First entry of AuftragDO");
      return START_NUMBER;
    }
    Integer number = list.get(0);
    return ++number;
  }

  /**
   * Gets history entries of super and adds all history entries of the AuftragsPositionDO children.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#getDisplayHistoryEntries(ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final AuftragDO obj) {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (!hasLoggedInUserHistoryAccess(obj, false)) {
      return list;
    }
    if (CollectionUtils.isNotEmpty(obj.getPositionenIncludingDeleted())) {
      for (final AuftragsPositionDO position : obj.getPositionenIncludingDeleted()) {
        final List<DisplayHistoryEntry> entries = internalGetDisplayHistoryEntries(position);
        for (final DisplayHistoryEntry entry : entries) {
          final String propertyName = entry.getPropertyName();
          if (propertyName != null) {
            entry.setPropertyName("Pos#" + position.getNumber() + ":" + entry.getPropertyName()); // Prepend number of positon.
          } else {
            entry.setPropertyName("Pos#" + position.getNumber());
          }
        }
        list.addAll(entries);
      }
    }
    if (CollectionUtils.isNotEmpty(obj.getPaymentSchedules())) {
      for (final PaymentScheduleDO schedule : obj.getPaymentSchedules()) {
        final List<DisplayHistoryEntry> entries = internalGetDisplayHistoryEntries(schedule);
        for (final DisplayHistoryEntry entry : entries) {
          final String propertyName = entry.getPropertyName();
          if (propertyName != null) {
            entry.setPropertyName("PaymentSchedule#" + schedule.getNumber() + ":" + entry.getPropertyName()); // Prepend number of positon.
          } else {
            entry.setPropertyName("PaymentSchedule#" + schedule.getNumber());
          }
        }
        list.addAll(entries);
      }
    }
    list.sort((o1, o2) -> (o2.getTimestamp().compareTo(o1.getTimestamp())));
    return list;
  }

  @Override
  protected Class<?>[] getAdditionalHistorySearchDOs() {
    return ADDITIONAL_HISTORY_SEARCH_DOS;
  }

  /**
   * Returns also true, if idSet contains the id of any order position.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#contains(Set, ExtendedBaseDO)
   */
  @Override
  public boolean contains(final Set<Integer> idSet, final AuftragDO entry) {
    if (super.contains(idSet, entry)) {
      return true;
    }
    for (final AuftragsPositionDO pos : entry.getPositionenIncludingDeleted()) {
      if (idSet.contains(pos.getId())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public AuftragDO newInstance() {
    return new AuftragDO();
  }
}
