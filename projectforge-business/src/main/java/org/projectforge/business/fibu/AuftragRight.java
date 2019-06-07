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

package org.projectforge.business.fibu;

import java.util.Date;

import org.apache.commons.collections.CollectionUtils;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightServiceImpl;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;

/**
 * User {@link UserRightValue#PARTLYREADWRITE} for users who are members of FIBU_ORGA_GROUPS <b>and</b> of
 * PROJECT_MANAGER/PROJECT_ASSISTANT: If set, then such users have only access to their projects (assigned by the
 * project manager groups). If you choose {@link UserRightValue#READWRITE} for such users, they'll have full read/write
 * access to all orders.
 *
 * @author Kai Reinhard (k.reinhard@me.de)
 */
public class AuftragRight extends UserRightAccessCheck<AuftragDO>
{
  private static final long serialVersionUID = 8639987084144268831L;

  public AuftragRight(AccessChecker accessChecker)
  {
    super(accessChecker, UserRightId.PM_ORDER_BOOK, UserRightCategory.PM,
        UserRightServiceImpl.FALSE_READONLY_PARTLYREADWRITE_READWRITE);
    initializeUserGroupsRight(UserRightServiceImpl.FALSE_READONLY_PARTLYREADWRITE_READWRITE,
        UserRightServiceImpl.FIBU_ORGA_PM_GROUPS)
        // All project managers have read-write access:
        .setAvailableGroupRightValues(ProjectForgeGroup.PROJECT_MANAGER, UserRightValue.PARTLYREADWRITE)
        // All project assistants have no, read or read-write access:
        .setAvailableGroupRightValues(ProjectForgeGroup.PROJECT_ASSISTANT, UserRightValue.FALSE,
            UserRightValue.PARTLYREADWRITE)
        // Read only access for controlling users:
        .setReadOnlyForControlling();
  }

  /**
   * @return True, if {@link UserRightId#PM_PROJECT} is potentially available for the user (independent from the
   * configured value).
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(org.projectforge.framework.access.AccessChecker,
   * org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user)
  {
    return accessChecker.hasRight(user, getId(), UserRightValue.READONLY,
        UserRightValue.PARTLYREADWRITE,
        UserRightValue.READWRITE);
  }

  /**
   * Contact persons sehen Aufträge, die ihnen zugeordnet sind und die nicht vollständig fakturiert sind, sonst wie
   * hasSelectAccess(boolean). Vollständig fakturierte Aufträge sehen die contact persons nur, wenn das Angebotsdatum
   * nicht älter ca. 5 Jahre (ca. 1800 Tage) ist. <br/>
   * Ebenso sehen Projektmanager und Projektassistenten einen Auftrag analog zu einer Kontaktperson, sofern sie Mitglied
   * der ProjektManagerGroup des zugordneten Projekts sind. <br/>
   * Nur Mitglieder der FINANCE_GROUP dürfen für Aufträge das Flag "vollständig fakturiert" ändern.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final AuftragDO obj, final AuftragDO oldObj,
      final OperationType operationType)
  {
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    if (operationType == OperationType.SELECT) {
      if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.CONTROLLING_GROUP) == true) {
        return true;
      }
      if (accessChecker.hasRight(user, getId(), UserRightValue.READONLY, UserRightValue.PARTLYREADWRITE,
          UserRightValue.READWRITE) == false) {
        return false;
      }
    } else {
      if (accessChecker.hasRight(user, getId(), UserRightValue.PARTLYREADWRITE, UserRightValue.READWRITE) == false) {
        return false;
      }
    }
    if (obj != null
        && accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP) == false
        && CollectionUtils.isNotEmpty(obj.getPositionenIncludingDeleted()) == true) {
      // Special field check for non finance administrative staff members:
      if (operationType == OperationType.INSERT) {
        for (final AuftragsPositionDO position : obj.getPositionenExcludingDeleted()) {
          if (position.getVollstaendigFakturiert() == true) {
            throw new AccessException("fibu.auftrag.error.vollstaendigFakturiertProtection");
          }
        }
      } else if (oldObj != null) {
        for (short number = 1; number <= obj.getPositionenIncludingDeleted().size(); number++) {
          final AuftragsPositionDO position = obj.getPosition(number);
          final AuftragsPositionDO dbPosition = oldObj.getPosition(number);

          // check if deleted
          if (position.isDeleted())
            continue;

          if (dbPosition == null) {
            if (position.getVollstaendigFakturiert() == true) {
              throw new AccessException("fibu.auftrag.error.vollstaendigFakturiertProtection");
            }
          } else if (position.getVollstaendigFakturiert() != dbPosition.getVollstaendigFakturiert()) {
            throw new AccessException("fibu.auftrag.error.vollstaendigFakturiertProtection");
          }
        }
      }
    }
    if (accessChecker.isUserMemberOfGroup(user, UserRightServiceImpl.FIBU_ORGA_GROUPS) == true
        && accessChecker.hasRight(user, getId(), UserRightValue.READONLY, UserRightValue.READWRITE)) {
      // No further access checking (but not for users with right PARTLY_READWRITE.
    } else if (obj != null) {
      // User should be a PROJECT_MANAGER or PROJECT_ASSISTANT or user has PARTLYREADWRITE access:
      boolean hasAccess = false;
      if (accessChecker.userEquals(user, obj.getContactPerson()) == true) {
        hasAccess = true;
      }
      if (obj.getProjekt() != null
          && userGroupCache.isUserMemberOfGroup(user.getId(), obj.getProjekt().getProjektManagerGroupId())) {
        hasAccess = true;
      }
      if (hasAccess == true) {
        if (obj.isVollstaendigFakturiert() == false) {
          return true;
        } else if (obj.getAngebotsDatum() != null) {
          final long millis = (new Date()).getTime() - obj.getAngebotsDatum().getTime();
          if (millis / DateHelper.MILLIS_DAY <= 1800) {
            return true;
          }
        }
      }
      return false;
    }
    return true;
  }
}
