/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.fibu.kost;

import java.util.List;

import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.EmployeeSalaryDO;
import org.projectforge.business.fibu.EmployeeSalaryDao;
import org.projectforge.business.fibu.MonthlyEmployeeReportDao;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

public class NewKostZuweisungDao extends BaseDao<KostZuweisungDO>
{
	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(NewKostZuweisungDao.class);

	private EmployeeDao employeeDao;

	private Kost1Dao kost1Dao;

	private Kost2Dao kost2Dao;

	private EmployeeSalaryDao employeeSalaryDao;

	private NewKostZuweisungDao kostZuweisungDao;

	private MonthlyEmployeeReportDao monthlyEmployeeReportDao;

	//  public ResultHolder checkMonth(final int year, final int month)
	//  {
	//    final EmployeeSalaryFilter filter = new EmployeeSalaryFilter();
	//    filter.setYear(year);
	//    filter.setMonth(month);
	//    // Get all salaries of the given month
	//    final List<EmployeeSalaryDO> list = employeeSalaryDao.getList(filter);
	//    for (final EmployeeSalaryDO salary : list) {
	//      // For every employee (user)
	//      final PFUserDO user = userGroupCache.getUser(salary.getEmployee().getUserId());
	//      if (year != salary.getYear()) {
	//        log.warn("Salary to check is not of the same year " + year + " as expected: " + salary);
	//        continue;
	//      }
	//      if (month != salary.getMonth()) {
	//        log.warn("Salary to check is not of the same month " + month + " as expected: " + salary);
	//        continue;
	//      }
	//      final MonthlyEmployeeReport report = monthlyEmployeeReportDao.getReport(year, month, user);
	//      final Map<String, Kost2Row> rows = report.getKost2Rows();
	//      final Map<Integer, MonthlyEmployeeReportEntry> employeeReportMap = report.getKost2Durations();
	//      final Kost1DO kost1 = salary.getEmployee().getKost1();
	//      final long totalMillis = report.getTotalDuration();
	//      for (final Kost2Row row : rows.values()) {
	//        final Kost2DO kost2 = row.getKost2();
	//        kostZuweisungDao.getKostZuweisungen(salary);
	//        final MonthlyEmployeeReportEntry employeeReportEntry = employeeReportMap.get(kost2.getId());
	//        final long durationMillis = employeeReportEntry.getMillis();
	//        final KostZuweisungDO kostZuweisung = new KostZuweisungDO();
	//        kostZuweisung.setKost1(kost1);
	//        kostZuweisung.setKost2(kost2);
	//        //kostZuweisung.setNetto(netto);
	//        kostZuweisung.setEmployeeSalary(salary);
	//      }
	//    }
	//  }

	public NewKostZuweisungDao()
	{
		super(KostZuweisungDO.class);
	}

	public List<KostZuweisungDO> getKostZuweisungen(final EmployeeSalaryDO salary)
	{
		@SuppressWarnings("unchecked")
		final List<KostZuweisungDO> list = (List<KostZuweisungDO>) getHibernateTemplate().find("from KostZuweisungDO k where k.employeeSalary.id = ? and u.password = ?",
				salary.getId());
		return list;
	}

	/**
	 * User must member of group finance or controlling.
	 * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess()
	 */
	@Override
	public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
	{
		return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP);
	}

	/**
	 * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess(PFUserDO, org.projectforge.core.ExtendedBaseDO, boolean)
	 * @see #hasSelectAccess(PFUserDO, boolean)
	 */
	@Override
	public boolean hasSelectAccess(final PFUserDO user, final KostZuweisungDO obj, final boolean throwException)
	{
		return hasSelectAccess(user, throwException);
	}

	/**
	 * User must member of group finance.
	 * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
	 */
	@Override
	public boolean hasAccess(final PFUserDO user, final KostZuweisungDO obj, final KostZuweisungDO oldObj, final OperationType operationType,
			final boolean throwException)
	{
		return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.FINANCE_GROUP);
	}

	/**
	 * @param kostZuweisung
	 * @param kost1Id If null, then kost1 will be set to null;
	 * @see BaseDao#getOrLoad(Integer)
	 */
	public void setKost1(final KostZuweisungDO kostZuweisung, final Integer kost1Id)
	{
		final Kost1DO kost1 = kost1Dao.getOrLoad(kost1Id);
		kostZuweisung.setKost1(kost1);
	}

	/**
	 * @param kostZuweisung
	 * @param kost1Id If null, then kost2 will be set to null;
	 * @see BaseDao#getOrLoad(Integer)
	 */
	public void setKost2(final KostZuweisungDO kostZuweisung, final Integer kost2Id)
	{
		final Kost2DO kost2 = kost2Dao.getOrLoad(kost2Id);
		kostZuweisung.setKost2(kost2);
	}

	public void setEmployeeDao(final EmployeeDao employeeDao)
	{
		this.employeeDao = employeeDao;
	}

	public void setEmployeeSalaryDao(final EmployeeSalaryDao employeeSalaryDao)
	{
		this.employeeSalaryDao = employeeSalaryDao;
	}

	public void setKost1Dao(final Kost1Dao kost1Dao)
	{
		this.kost1Dao = kost1Dao;
	}

	public void setKost2Dao(final Kost2Dao kost2Dao)
	{
		this.kost2Dao = kost2Dao;
	}

	public void setKostZuweisungDao(final NewKostZuweisungDao kostZuweisungDao)
	{
		this.kostZuweisungDao = kostZuweisungDao;
	}

	public void setMonthlyEmployeeReportDao(final MonthlyEmployeeReportDao monthlyEmployeeReportDao)
	{
		this.monthlyEmployeeReportDao = monthlyEmployeeReportDao;
	}

	@Override
	public KostZuweisungDO newInstance()
	{
		return new KostZuweisungDO();
	}
}
