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

package org.projectforge.business.vacation.service;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
  List<VacationDO> getVacationForDate(EmployeeDO employee, Date startDate, Date endDate, boolean withSpecial);

  /**
   * Getting all not deleted vacations for given employee of the current year.
   *
   * @param employee
   * @param year
   * @param withSpecial
   * @return List of vacations
   */
  List<VacationDO> getActiveVacationForYear(EmployeeDO employee, int year, boolean withSpecial);

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
   * Returns the number of available vacation for user object. If user has no employee, it returns 0.
   *
   * @param user
   * @param year
   * @param checkLastYear
   * @return number of available vacation
   */
  BigDecimal getAvailableVacationdaysForYear(PFUserDO user, int year, boolean checkLastYear);

  /**
   * Returns the number of available vacation days for the given employee at the given date.
   * For example: If date is 2017-04-30, then the approved vacation between 2017-01-01 and 2017-04-30 is regarded.
   * Also the (used) vacation from the previous year is regarded depending on the given date.
   *
   * @param employee
   * @param queryDate
   * @return
   */
  BigDecimal getAvailableVacationDaysForYearAtDate(final EmployeeDO employee, final Date queryDate);

  /**
   * Returns the number of pland vacation days for the given employee at the given date.
   * For example: If date is 2017-04-30, then the vacation between 2017-04-30 and 2017-12-31 is regarded.
   *
   * @param employee
   * @param queryDate
   * @return
   */
  BigDecimal getPlandVacationDaysForYearAtDate(final EmployeeDO employee, final Date queryDate);

  /**
   * Returns the number of approved vacation days
   *
   * @param employee
   * @param year
   * @return
   */
  BigDecimal getApprovedVacationdaysForYear(EmployeeDO employee, int year);

  /**
   * Returns the number of planed vacation days
   *
   * @param employee
   * @param year
   * @return
   */
  BigDecimal getPlannedVacationdaysForYear(EmployeeDO employee, int year);

  /**
   * Getting the number of used and planned vacation days
   *
   * @param employee
   * @param year
   * @return number of used vacation days
   */
  BigDecimal getApprovedAndPlanedVacationdaysForYear(EmployeeDO employee, int year);

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
   * Delete the used days from vacation to last year
   *
   * @param vacationData
   * @return new value for used days
   */
  BigDecimal deleteUsedVacationDaysFromLastYear(VacationDO vacationData);

  /**
   * Calculates the vacationsdays from last year and updates it.
   *
   * @param emp
   * @param year
   */
  void updateVacationDaysFromLastYearForNewYear(EmployeeDO emp, int year);

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
   * @param withSpecial
   * @return
   */
  List<VacationDO> getAllActiveVacation(EmployeeDO employee, boolean withSpecial);

  /**
   * Returns number of open applications for leave for users employee
   *
   * @param user
   * @return
   */
  BigDecimal getOpenLeaveApplicationsForUser(PFUserDO user);

  /**
   * Returns number of special vacations for an employee
   *
   * @param employee
   * @param year
   * @param status
   * @return
   */
  BigDecimal getSpecialVacationCount(EmployeeDO employee, int year, VacationStatus status);

  /**
   * Returns the calendars for apllication for leave
   *
   * @param vacation
   * @return
   */
  List<TeamCalDO> getCalendarsForVacation(VacationDO vacation);

  /**
   * Save the calendars for apllication for leave
   *
   * @param calendars
   * @param vacation
   * @return
   */
  void saveOrUpdateVacationCalendars(VacationDO vacation, Collection<TeamCalDO> calendars);

  /**
   * Delete CalenderEvents for apllication for leave
   *
   * @param vacation
   * @return
   */
  void markTeamEventsOfVacationAsDeleted(VacationDO vacation, boolean deleteIncludingVacationCalendarDO);

  /**
   * UnDelete CalenderEvents for apllication for leave
   *
   * @param vacation
   * @return
   */
  void markAsUnDeleteEventsForVacationCalendars(VacationDO vacation);

  /**
   * Create CalenderEvents for apllication for leave
   *
   * @param vacation
   * @return
   */
  void createEventsForVacationCalendars(VacationDO vacation);

  /**
   * Returns the number of vacation days for the given period.
   *
   * @param from
   * @param to
   * @param isHalfDayVacation
   * @return
   */
  BigDecimal getVacationDays(final Date from, final Date to, final Boolean isHalfDayVacation);

  /**
   * UnDelete Calender for apllication for leave
   *
   * @param vacation
   * @return
   */
  void undeleteTeamEventsOfVacation(VacationDO vacation);


  /**
   * Get VacationCount for PFUser
   *
   * @param fromYear
   * @param fromMonth
   * @param toYear
   * @param toMonth
   * @param user
   * @return
   */
  String getVacationCount(int fromYear, int fromMonth, int toYear, int toMonth, PFUserDO user);

  /**
   * Checks, if logged in User has HR vacation access.
   *
   * @return
   */
  boolean hasLoggedInUserHRVacationAccess();
}
