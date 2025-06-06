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

package org.projectforge.business.fibu;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

public class ProjektDaoTest extends AbstractTestBase {
    @Autowired
    private ProjektDao projektDao;

    @Test
    public void checkAccess() {
        persistenceService.runInTransaction(context ->
        {
            logon(AbstractTestBase.TEST_FINANCE_USER);
            final GroupDO group = initTestDB.addGroup("ProjektDaoTest.ProjectManagers",
                    AbstractTestBase.TEST_PROJECT_ASSISTANT_USER);
            ProjektDO projekt = new ProjektDO();
            projekt.setName("ACME - Webportal");
            projekt.setProjektManagerGroup(group);
            Serializable id = projektDao.insert(projekt);
            projekt = projektDao.find(id);
            projekt.setDescription("Test");
            projektDao.update(projekt);

            logon(AbstractTestBase.TEST_CONTROLLING_USER);
            checkNoWriteAccess(id, projekt, "Controlling");

            logon(AbstractTestBase.TEST_USER);
            checkNoAccess(id, "Other");
            checkNoAccess(id, projekt, "Other");

            logon(AbstractTestBase.TEST_PROJECT_MANAGER_USER);
            projektDao.select(new ProjektFilter());
            checkNoAccess(id, projekt, "Project manager");

            logon(AbstractTestBase.TEST_PROJECT_ASSISTANT_USER);
            projektDao.select(new ProjektFilter());
            checkNoWriteAccess(id, projekt, "Project assistant");
            checkNoHistoryAccess(id, projekt, "Project assistant");

            logon(AbstractTestBase.TEST_ADMIN_USER);
            checkNoAccess(id, projekt, "Admin ");
            checkNoAccess(id, projekt, "Project manager");
            return null;
        });
    }

    private void checkNoAccess(Serializable id, String who) {
        try {
            ProjektFilter filter = new ProjektFilter();
            projektDao.select(filter);
            fail("AccessException expected: " + who + " users should not have select list access to projects.");
        } catch (AccessException ex) {
            // OK
        }
    }

    private void checkNoAccess(Serializable id, ProjektDO projekt, String who) {
        try {
            projektDao.find(id);
            fail("AccessException expected: " + who + " users should not have select access to projects.");
        } catch (AccessException ex) {
            // OK
        }
        checkNoHistoryAccess(id, projekt, who);
        checkNoWriteAccess(id, projekt, who);
    }

    private void checkNoHistoryAccess(Serializable id, ProjektDO projekt, String who) {
        assertFalse(projektDao.hasLoggedInUserHistoryAccess(false), who + " users should not have select access to history of projects.");
        try {
            projektDao.hasLoggedInUserHistoryAccess(true);
            fail("AccessException expected: " + who + " users should not have select access to history of projects.");
        } catch (AccessException ex) {
            // OK
        }
        assertFalse(projektDao.hasLoggedInUserHistoryAccess(projekt, false), who + " users should not have select access to history of projects.");
        try {
            projektDao.hasLoggedInUserHistoryAccess(projekt, true);
            fail("AccessException expected: " + who + " users should not have select access to history of invoices.");
        } catch (AccessException ex) {
            // OK
        }
    }

    private void checkNoWriteAccess(Serializable id, ProjektDO projekt, String who) {
        try {
            ProjektDO ku = new ProjektDO();
            projekt.setName("ACME - Webportal 2");
            projektDao.insert(ku);
            fail("AccessException expected: " + who + " users should not have save access to projects.");
        } catch (AccessException ex) {
            // OK
        }
        try {
            projekt.setDescription(who);
            projektDao.update(projekt);
            fail("AccessException expected: " + who + " users should not have update access to projects.");
        } catch (AccessException ex) {
            // OK
        }
    }

}
