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
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost1Dao;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.attr.impl.InternalAttrSchemaConstants;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Ein Mitarbeiter ist einem ProjectForge-Benutzer zugeordnet und trägt einige buchhalterische Angaben.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class EmployeeDao extends BaseDao<EmployeeDO> {
  public static final UserRightId USER_RIGHT_ID = UserRightId.HR_EMPLOYEE;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeDao.class);

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"user.firstname", "user.lastname",
          "user.description",
          "user.organization"};

  private final static String META_SQL = " AND e.deleted = :deleted AND e.tenant = :tenant";

  @Autowired
  private UserDao userDao;

  @Autowired
  private Kost1Dao kost1Dao;

  @Autowired
  private PfEmgrFactory emgrFactory;

  public EmployeeDao() {
    super(EmployeeDO.class);
    userRightId = USER_RIGHT_ID;
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  public EmployeeDO findByUserId(final Integer userId) {
    return SQLHelper.ensureUniqueResult(em
            .createNamedQuery(EmployeeDO.FIND_BY_USER_ID, EmployeeDO.class)
            .setParameter("userId", userId)
            .setParameter("tenantId", TenantRegistryMap.getInstance().getTenantRegistry().getTenantId()));
  }

  /**
   * If more than one employee is found, null will be returned.
   *
   * @param fullname Format: &lt;last name&gt;, &lt;first name&gt;
   */
  public EmployeeDO findByName(final String fullname) {
    final StringTokenizer tokenizer = new StringTokenizer(fullname, ",");
    if (tokenizer.countTokens() != 2) {
      log.error("EmployeeDao.getByName: Token '" + fullname + "' not supported.");
    }
    Validate.isTrue(tokenizer.countTokens() == 2);
    final String lastname = tokenizer.nextToken().trim();
    final String firstname = tokenizer.nextToken().trim();
    return SQLHelper.ensureUniqueResult(em
            .createNamedQuery(EmployeeDO.FIND_BY_LASTNAME_AND_FIRST_NAME, EmployeeDO.class)
            .setParameter("firstname", firstname)
            .setParameter("lastname", lastname));
  }

  /**
   * @param employee
   * @param userId   If null, then user will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  @Deprecated
  public void setUser(final EmployeeDO employee, final Integer userId) {
    final PFUserDO user = userDao.getOrLoad(userId);
    employee.setUser(user);
  }

  /**
   * @param employee
   * @param kost1Id  If null, then kost1 will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  @Deprecated
  public void setKost1(final EmployeeDO employee, final Integer kost1Id) {
    final Kost1DO kost1 = kost1Dao.getOrLoad(kost1Id);
    employee.setKost1(kost1);
  }

  @Override
  public List<EmployeeDO> getList(final BaseSearchFilter filter) {
    final EmployeeFilter myFilter;
    if (filter instanceof EmployeeFilter) {
      myFilter = (EmployeeFilter) filter;
    } else {
      myFilter = new EmployeeFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    final List<EmployeeDO> list = getList(queryFilter);
    final Date now = new Date();
    if (myFilter.isShowOnlyActiveEntries()) {
      CollectionUtils.filter(list, new Predicate() {
        @Override
        public boolean evaluate(final Object object) {
          final EmployeeDO employee = (EmployeeDO) object;
          if (employee.getEintrittsDatum() != null && now.before(employee.getEintrittsDatum())) {
            return false;
          } else return employee.getAustrittsDatum() == null || !now.after(employee.getAustrittsDatum());
        }
      });
    }
    for (EmployeeDO employeeDO : list) {
      for (EmployeeTimedDO employeeTimedDO : employeeDO.getTimeableAttributes()) {
        if (employeeTimedDO.getGroupName().equals(InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME)) {
          try {
            employeeDO.setStatus(EmployeeStatus.findByi18nKey((String) employeeTimedDO.getAttribute("status")));
          } catch (Exception e) {
            log.error("Exception while setting timeable status to deprecated status employee field. Message: " + e.getMessage());
          }
        }
      }
    }
    return list;
  }

  @Override
  public EmployeeDO newInstance() {
    return new EmployeeDO();
  }

  public EmployeeTimedDO newEmployeeTimeAttrRow(final EmployeeDO employee) {
    final EmployeeTimedDO nw = new EmployeeTimedDO();
    nw.setEmployee(employee);
    employee.getTimeableAttributes().add(nw);
    return nw;
  }

  public EmployeeDO getEmployeeByStaffnumber(String staffnumber) {
    EmployeeDO result = null;
    try {
      result = emgrFactory.runRoTrans(emgr -> {
        String baseSQL = "SELECT e FROM EmployeeDO e WHERE e.staffNumber = :staffNumber";
        TenantDO tenant;
        if (ThreadLocalUserContext.getUser() == null || ThreadLocalUserContext.getUser().getTenant() == null) {
          tenant = tenantService.getDefaultTenant();
        } else {
          tenant = ThreadLocalUserContext.getUser().getTenant();
        }
        return emgr.selectSingleDetached(EmployeeDO.class, baseSQL + META_SQL, "staffNumber", staffnumber, "deleted", false, "tenant",
                tenant);
      });
    } catch (NoResultException ex) {
      log.warn("No employee found for staffnumber: " + staffnumber);
    }
    return result;
  }
}
