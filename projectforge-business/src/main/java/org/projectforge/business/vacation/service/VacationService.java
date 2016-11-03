package org.projectforge.business.vacation.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

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
   * @param year
   * @return List of vacations
   */
  List<VacationDO> getActiveVacationForYear(EmployeeDO employee, int year);

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
   * @param year
   * @param checkLastYear
   * @return number of available vacation
   */
  BigDecimal getAvailableVacationdaysForYear(EmployeeDO employee, int year, boolean checkLastYear);

  /**
   * Returns the number of used vacation days
   *
   * @param employee
   * @param year
   * @return number of used vacation days
   */
  BigDecimal getUsedVacationdaysForYear(EmployeeDO employee, int year);

  /**
   * Returns the number of planed vacation days
   *
   * @param currentEmployee
   * @param year
   * @return number of used vacation days
   */
  BigDecimal getPlanedVacationdaysForYear(EmployeeDO currentEmployee, int year);

  /**
   * Getting the number of used and planned vacation days
   *
   * @param employee
   * @param year
   * @return number of used vacation days
   */
  BigDecimal getUsedAndPlanedVacationdaysForYear(EmployeeDO employee, int year);

  /**
   * Sends an information mail to the vacation data users involved
   *
   * @param vacationData data
   * @param isNew        flag for new vacation request
   * @param isDeleted
   */
  void sendMailToVacationInvolved(VacationDO vacationData, boolean isNew, boolean isDeleted);

  /**
   * Sends an information to employee and HR, that vacation request is approved.
   *
   * @param vacationData data
   * @param approved     is application approved
   */
  void sendMailToEmployeeAndHR(VacationDO vacationData, boolean approved);

  /**
   * Returns the date for ending usage of vacation from last year
   *
   * @return
   */
  Calendar getEndDateVacationFromLastYear();

  /**
   * Updates the used days from last year
   *
   * @param vacationData
   * @return new value for used days
   */
  BigDecimal updateUsedVacationDaysFromLastYear(VacationDO vacationData);

  /**
   * Calculates the vacationsdays from last year and updates it.
   *
   * @param emp
   * @param year
   */
  void updateUsedNewVacationDaysFromLastYear(EmployeeDO emp, int year);

  /**
   * Check, if user is able to use vacation services.
   *
   * @param user
   * @param throwException
   * @return
   */
  boolean couldUserUseVacationService(PFUserDO user, boolean throwException);

  /**
   * Load all active vacations (not marked as deleted)
   *
   * @param employee
   * @return
   */
  List<VacationDO> getAllActiveVacation(EmployeeDO employee);
}