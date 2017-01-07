package org.projectforge.plugins.ffp.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.impl.CorePersistenceServiceImpl;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.model.FFPAccountingDO;
import org.projectforge.plugins.ffp.model.FFPDebtDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Standard implementation of the ffp event service interface.
 *
 * @author Florian Blumenstein
 */
@Service
public class FFPEventServiceImpl extends CorePersistenceServiceImpl<Integer, FFPEventDO>
    implements FFPEventService
{
  @Autowired
  private FFPEventDao eventDao;

  @Autowired
  private FFPDebtDao debtDao;

  @Autowired
  private EmployeeDao employeeDao;

  @Override
  public boolean hasLoggedInUserInsertAccess()
  {
    return eventDao.hasLoggedInUserInsertAccess();
  }

  @Override
  public boolean hasLoggedInUserInsertAccess(FFPEventDO obj, boolean throwException)
  {
    return eventDao.hasLoggedInUserInsertAccess(obj, throwException);
  }

  @Override
  public boolean hasLoggedInUserUpdateAccess(FFPEventDO obj, FFPEventDO dbObj, boolean throwException)
  {
    return eventDao.hasLoggedInUserUpdateAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasLoggedInUserDeleteAccess(FFPEventDO obj, FFPEventDO dbObj, boolean throwException)
  {
    return eventDao.hasLoggedInUserDeleteAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasDeleteAccess(PFUserDO user, FFPEventDO obj, FFPEventDO dbObj, boolean throwException)
  {
    return eventDao.hasDeleteAccess(user, obj, dbObj, throwException);
  }

  @Override
  public List<String> getAutocompletion(String property, String searchString)
  {
    return eventDao.getAutocompletion(property, searchString);
  }

  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(FFPEventDO obj)
  {
    return eventDao.getDisplayHistoryEntries(obj);
  }

  @Override
  public void rebuildDatabaseIndex4NewestEntries()
  {
    eventDao.rebuildDatabaseIndex4NewestEntries();
  }

  @Override
  public void rebuildDatabaseIndex()
  {
    eventDao.rebuildDatabaseIndex();
  }

  @Override
  public FFPEventDao getEventDao()
  {
    return eventDao;
  }

  public List<FFPDebtDO> calculateDebt(FFPEventDO event)
  {
    List<FFPAccountingDO> accountingDOs = event.getAccountingList();
    final BigDecimal averageCosts = calculateAverage(accountingDOs);

    Map<FFPAccountingDO, BigDecimal> paidToMuch = new HashMap<>();
    Map<FFPAccountingDO, BigDecimal> paidToLess = new HashMap<>();
    for (FFPAccountingDO accountingDO : accountingDOs) {
      BigDecimal hasToPayValue = averageCosts.multiply(accountingDO.getWeighting());

      if (accountingDO.getValue().compareTo(hasToPayValue) > 0) {
        paidToMuch.put(accountingDO, accountingDO.getValue().subtract(hasToPayValue));
      } else if (accountingDO.getValue().compareTo(hasToPayValue) == 0) {
        // attendee paid as much the average was. So we dont need to
        // mind this one
      } else {
        paidToLess.put(accountingDO, hasToPayValue.subtract(accountingDO.getValue()));
      }
    }
    List<FFPDebtDO> result = new ArrayList<>();
    for (Entry<FFPAccountingDO, BigDecimal> deptorEntry : paidToLess.entrySet()) {
      BigDecimal deptorValue = deptorEntry.getValue();
      for (Entry<FFPAccountingDO, BigDecimal> creditorEntry : paidToMuch.entrySet()) {
        BigDecimal creditorValue = creditorEntry.getValue();
        if (creditorValue.compareTo(BigDecimal.ZERO) == 0) {
          // This creditor has all his money back
          continue;
        }
        FFPDebtDO ffpDebtDO = new FFPDebtDO();
        ffpDebtDO.setFrom(deptorEntry.getKey().getAttendee());
        ffpDebtDO.setTo(creditorEntry.getKey().getAttendee());
        ffpDebtDO.setEvent(event);
        if (creditorValue.compareTo(deptorValue) > 0) {
          // creditor paid more than this debtor needs to pay
          creditorValue = creditorValue.subtract(deptorValue);
          ffpDebtDO.setValue(deptorValue.setScale(2, BigDecimal.ROUND_HALF_UP));
          paidToMuch.put(creditorEntry.getKey(), creditorValue);
          result.add(ffpDebtDO);
          break;
        } else if (creditorValue.compareTo(deptorValue) == 0) {
          // creditor paid the same amount as this debtor needs to pay
          creditorValue = BigDecimal.ZERO;
          ffpDebtDO.setValue(deptorValue.setScale(2, BigDecimal.ROUND_HALF_UP));
          paidToMuch.put(creditorEntry.getKey(), creditorValue);
          result.add(ffpDebtDO);
          break;
        } else {
          // creditor paid less than this debtor needs to pay
          // we need to get the next creditor
          deptorValue = deptorValue.subtract(creditorValue);
          ffpDebtDO.setValue(creditorValue.setScale(2, BigDecimal.ROUND_HALF_UP));
          // clean
          creditorValue = BigDecimal.ZERO;
          paidToMuch.put(creditorEntry.getKey(), creditorValue);
          result.add(ffpDebtDO);
        }
      }
    }
    return result;
  }

  @Override
  public List<FFPDebtDO> getDeptList(EmployeeDO employee)
  {
    return debtDao.getDebtList(employee);
  }

  @Override
  public void updateDept(FFPEventDO event)
  {
    List<FFPDebtDO> debtList = calculateDebt(event);
    //Check if new or update
    //TODO: Has to be done!
    //New
    debtDao.internalSaveOrUpdate(debtList);
    //Update
    //TODO: Has to be done!
  }

  @Override
  public void updateDebtFrom(FFPDebtDO debt)
  {
    debt.setApprovedByFrom(true);
    debtDao.internalUpdate(debt);
  }

  @Override
  public void updateDebtTo(FFPDebtDO debt)
  {
    debt.setApprovedByTo(true);
    debtDao.internalUpdate(debt);
  }

  @Override
  public Integer getOpenFromDebts(PFUserDO user)
  {
    return debtDao.getOpenFromDebts(employeeDao.findByUserId(user.getId()));
  }

  BigDecimal calculateAverage(List<FFPAccountingDO> accountingDOs)
  {
    BigDecimal sumValue = BigDecimal.ZERO;
    BigDecimal sumWighting = BigDecimal.ZERO;
    for (FFPAccountingDO attendee : accountingDOs) {
      sumValue = sumValue.add(attendee.getValue());
      sumWighting = sumWighting.add(attendee.getWeighting());
    }
    return sumValue.divide(sumWighting, 10, BigDecimal.ROUND_HALF_UP);
  }
}
