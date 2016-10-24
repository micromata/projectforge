package org.projectforge.business.vacation.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;

/**
 * Access to vacation.
 *
 * @author Florian Blumenstein
 */
public interface VacationService extends IPersistenceService<VacationDO>, IDao<VacationDO>
{

  /**
   * Getting all vacations for given employee and a time period.
   *
   * @param employee
   * @param startDate
   * @param endDate
   * @return List of vacations
   */
  List<VacationDO> getVacationForDate(EmployeeDO employee, Date startDate, Date endDate);

  /**
   * Getting all not deleted vacations for given employee of the current year.
   *
   * @param employee
   * @return List of vacations
   */
  List<VacationDO> getActiveVacationForCurrentYear(EmployeeDO employee);

  /**
   * Getting vacation for given ids.
   *
   * @param idList
   * @return List of vacations
   */
  List<VacationDO> getVacation(List<Serializable> idList);

  /**
   * Returns the number of available vacation
   *
   * @param employee
   * @return number of available vacation
   */
  BigDecimal getAvailableVacationdays(EmployeeDO employee);

  /**
   * Returns the number of used vacation days
   *
   * @param employee
   * @return number of used vacation days
   */
  BigDecimal getUsedVacationdays(EmployeeDO employee);

  /**
   * Returns the number of planed vacation days
   *
   * @param currentEmployee
   * @return number of used vacation days
   */
  BigDecimal getPlanedVacationdays(EmployeeDO currentEmployee);

  /**
   * Getting the number of used and planned vacation days
   *
   * @param employee
   * @return number of used vacation days
   */
  BigDecimal getUsedAndPlanedVacationdays(EmployeeDO employee);

  /**
   * Sends an information mail to the vacation data users involved
   *
   * @param vacationData data
   * @param isNew        flag for new vacation request
   */
  void sendMailToVacationInvolved(VacationDO vacationData, boolean isNew);

  /**
   * Sends an information to employee and HR, that vacation request is approved.
   *
   * @param vacationData data
   * @param approved     is application approved
   */
  void sendMailToEmployeeAndHR(VacationDO vacationData, boolean approved);

}