/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.user;

import jakarta.annotation.PostConstruct;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.projectforge.business.address.AddressbookRight;
import org.projectforge.business.fibu.AuftragRight;
import org.projectforge.business.fibu.ProjektRight;
import org.projectforge.business.gantt.GanttChartRight;
import org.projectforge.business.humanresources.HRPlanningRight;
import org.projectforge.business.teamcal.admin.right.TeamCalRight;
import org.projectforge.business.teamcal.event.right.TeamEventRight;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.RightRightIdProviderService;
import org.projectforge.framework.persistence.api.UserRightService;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;

@Service
public class UserRightServiceImpl implements UserRightService, Serializable {
    private static final long serialVersionUID = 7745893362798312310L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserRightServiceImpl.class);

    private final Map<IUserRightId, UserRight> rights = new HashMap<>();

    private final Map<String, IUserRightId> rightIds = new HashMap<>();

    private final List<UserRight> orderedRights = new ArrayList<>();

    @Override
    public UserRight getRight(final IUserRightId id) {
        return rights.get(id);
    }

    @Override
    public UserRight getRight(String userRightId) {
        IUserRightId uid = rightIds.get(userRightId);
        if (uid == null) {
            return null;
        }
        return rights.get(uid);
    }

    @Override
    public IUserRightId getRightId(final String userRightId) throws IllegalArgumentException {
        final IUserRightId rightId = rightIds.get(userRightId);
        if (rightId == null) {
            throw new IllegalArgumentException(
                    "UserRightId with id '" + userRightId + "' not found (may-be not yet initialized).");
        }
        return rightId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<UserRight> getOrderedRights() {
        return new UnmodifiableList(orderedRights);
    }

    @PostConstruct
    public void init() {
        initUserRightIds();
        addRight(UserRightCategory.HR, UserRightId.HR_EMPLOYEE, FALSE_READONLY_READWRITE, ProjectForgeGroup.HR_GROUP);
        addRight(UserRightCategory.HR, UserRightId.HR_EMPLOYEE_SALARY, FALSE_READONLY_READWRITE,
                ProjectForgeGroup.HR_GROUP);
        addRight(UserRightCategory.HR, UserRightId.HR_VACATION, FALSE_READONLY_READWRITE,
                ProjectForgeGroup.HR_GROUP);
        addRight(UserRightCategory.FIBU, UserRightId.FIBU_AUSGANGSRECHNUNGEN, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
                .setReadOnlyForControlling();
        addRight(UserRightCategory.FIBU, UserRightId.FIBU_EINGANGSRECHNUNGEN, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
                .setReadOnlyForControlling();
        addRight(UserRightCategory.FIBU, UserRightId.FIBU_DATEV_IMPORT, FALSE_TRUE, FIBU_GROUPS);
        addRight(UserRightCategory.FIBU, UserRightId.FIBU_COST_UNIT, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
                .setReadOnlyForControlling();
        addRight(UserRightCategory.FIBU, UserRightId.FIBU_ACCOUNTS, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
                .setReadOnlyForControlling();
        addRight(UserRightCategory.FIBU, UserRightId.FIBU_CURRENCY_CONVERSION, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
                .setReadOnlyForControlling();
        addRight(UserRightCategory.ORGA, UserRightId.ORGA_CONTRACTS, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
                .setReadOnlyForControlling();
        addRight(UserRightCategory.ORGA, UserRightId.ORGA_INCOMING_MAIL, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
                .setReadOnlyForControlling();
        addRight(UserRightCategory.ORGA, UserRightId.ORGA_OUTGOING_MAIL, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
                .setReadOnlyForControlling();
        addRight(UserRightCategory.ORGA, UserRightId.ORGA_VISITORBOOK, FALSE_READONLY_READWRITE, ProjectForgeGroup.ORGA_TEAM);
        addRight(new ProjektRight());
        addRight(new AuftragRight());
        addRight(new GanttChartRight());
        addRight(new HRPlanningRight());
        addRight(new TeamCalRight());
        addRight(new TeamEventRight());
        addRight(new AddressbookRight());

        addRight(UserRightCategory.ADMIN, UserRightId.ADMIN_CORE, FALSE_READONLY_READWRITE, ProjectForgeGroup.ADMIN_GROUP);
    }

    private void initUserRightIds() {

        ServiceLoader<RightRightIdProviderService> serviceLoader = ServiceLoader
                .load(RightRightIdProviderService.class);
        for (RightRightIdProviderService service : serviceLoader) {
            String cname = service.getClass().getName();
            for (IUserRightId uid : service.getUserRightIds()) {
                if (rightIds.containsKey(uid.getId())) {
                    log.error("Duplicated UserId: " + uid.getId());
                }
                rightIds.put(uid.getId(), uid);
            }
        }

    }

    public UserGroupsRight addRight(final UserRightCategory category, final UserRightId id,
                                    final UserRightValue[] rightValues,
                                    final ProjectForgeGroup... fibuGroups) {
        final UserGroupsRight right = new UserGroupsRight(id, category, rightValues, fibuGroups);
        addRight(right);
        return right;
    }

    @Override
    public void addRight(final UserRight right) {
        final IUserRightId userRightId = right.getId();
        rights.put(right.getId(), right);
        rightIds.put(userRightId.getId(), userRightId);
        orderedRights.add(right);
    }
}
