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

package org.projectforge.business.task;

import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TaskKostTest extends AbstractTestBase {
    // private static final Logger log = Logger.getLogger(TaskTest.class);

    @Autowired
    TaskDao taskDao;

    @Autowired
    private TaskTree taskTree;

    @Autowired
    ProjektDao projektDao;

    @Autowired
    Kost2Dao kost2Dao;

    @Test
    public void checkKost2() {
        persistenceService.runInTransaction(context ->
        {
            logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
            Kost2DO kost = new Kost2DO();
            kost.setNummernkreis(1);
            kost.setBereich(137);
            kost.setTeilbereich(05);
            kost.setKost2Art(new Kost2ArtDO().withId(1L));
            final Kost2DO kost2a = kost2Dao
                    .find(kost2Dao.insert(kost)); // Kost2: 1.137.05.01
            kost = new Kost2DO();
            kost.setNummernkreis(1);
            kost.setBereich(137);
            kost.setTeilbereich(05);
            kost.setKost2Art(new Kost2ArtDO().withId(2L));
            final Kost2DO kost2b = kost2Dao
                    .find(kost2Dao.insert(kost)); // Kost2: 1.137.05.02
            kost = new Kost2DO();
            kost.setNummernkreis(2);
            kost.setBereich(423);
            kost.setTeilbereich(12);
            kost.setKost2Art(new Kost2ArtDO().withId(1L));
            final Kost2DO kost2c = kost2Dao
                    .find(kost2Dao.insert(kost)); // Kost2: 2.423.12.01
            final TaskDO task = initTestDB.addTask("kost2test2", "root");
            task.setKost2BlackWhiteList("1.137.05.01, 1.137.05.02, 2.423.12.01");
            taskDao.update(task);
            List<Kost2DO> list = taskTree.getKost2List(task.getId());
            assertEquals(3, list.size());
            assertKost2(kost2a, list.get(0));
            assertKost2(kost2b, list.get(1));
            assertKost2(kost2c, list.get(2));
            task.setKost2BlackWhiteList("1.137.05.01, 1.137.05.02, 2.423.12.01");
            task.setKost2IsBlackList(true);
            taskDao.update(task);
            list = taskTree.getKost2List(task.getId());
            assertNull(list);
            task.setKost2BlackWhiteList("1.137.05.01, 1.137.05.02, 2.423.12.01, jwe9jdkjn");
            task.setKost2IsBlackList(false);
            taskDao.update(task);
            list = taskTree.getKost2List(task.getId());
            assertEquals(3, list.size());
            assertKost2(kost2a, list.get(0));
            assertKost2(kost2b, list.get(1));
            assertKost2(kost2c, list.get(2));
            return null;
        });
    }

    @Test
    public void checkProjektKost2() {
        persistenceService.runInTransaction(context ->
        {
            logon(getUser(AbstractTestBase.TEST_FINANCE_USER));
            final TaskDO task = initTestDB.addTask("kost2test1", "root");
            final ProjektDO find = new ProjektDO();
            find.setName("Kost2 test project");
            find.setInternKost2_4(137);
            find.setNummer(05);
            find.setTask(task);
            final ProjektDO projekt = projektDao
                    .find(projektDao.insert(find)); // Kost2: 4.137.05
            List<Kost2DO> list = taskTree.getKost2List(task.getId());
            assertNull(list);
            Kost2DO kost = new Kost2DO();
            kost.setNummernkreis(4);
            kost.setBereich(137);
            kost.setProjekt(projekt);
            kost.setTeilbereich(05);
            kost.setKost2Art(new Kost2ArtDO().withId(1L));
            final Kost2DO kost2a = kost2Dao
                    .find(kost2Dao.insert(kost)); // Kost2: 4.137.05.01
            kost = new Kost2DO();
            kost.setNummernkreis(4);
            kost.setBereich(137);
            kost.setProjekt(projekt);
            kost.setTeilbereich(05);
            kost.setKost2Art(new Kost2ArtDO().withId(2L));
            final Kost2DO kost2b = kost2Dao
                    .find(kost2Dao.insert(kost)); // Kost2: 4.137.05.02
            list = taskTree.getKost2List(task.getId());
            assertEquals(2, list.size());
            assertKost2(kost2a, list.get(0));
            assertKost2(kost2b, list.get(1));
            kost = new Kost2DO();
            kost.setNummernkreis(4);
            kost.setBereich(137);
            kost.setProjekt(projekt);
            kost.setTeilbereich(05);
            kost.setKost2Art(new Kost2ArtDO().withId(3L));
            final Kost2DO kost2c = kost2Dao
                    .find(kost2Dao.insert(kost)); // Kost2: 4.137.05.03
            kost = new Kost2DO();
            kost.setNummernkreis(4);
            kost.setBereich(137);
            kost.setProjekt(projekt);
            kost.setTeilbereich(05);
            kost.setKost2Art(new Kost2ArtDO().withId(4L));
            final Kost2DO kost2d = kost2Dao
                    .find(kost2Dao.insert(kost)); // Kost2: 4.137.05.04
            list = taskTree.getKost2List(task.getId());
            assertEquals(4, list.size());
            assertKost2(kost2a, list.get(0));
            assertKost2(kost2b, list.get(1));
            assertKost2(kost2c, list.get(2));
            assertKost2(kost2d, list.get(3));
            task.setKost2BlackWhiteList("02,3, 5.123.423.11"); // White list
            // 5.123.423.11 will be ignored.
            taskDao.update(task);
            list = taskTree.getKost2List(task.getId());
            assertEquals(2, list.size());
            assertKost2(kost2b, list.get(0));
            assertKost2(kost2c, list.get(1));
            task.setKost2BlackWhiteList("05.02; 4.137.05.03, 5.123.423.11");
            task.setKost2IsBlackList(true); // Black list
            // 5.123.423.11 will be ignored.
            taskDao.update(task);
            list = taskTree.getKost2List(task.getId());
            assertEquals(2, list.size());
            assertKost2(kost2a, list.get(0));
            assertKost2(kost2d, list.get(1));
            task.setKost2BlackWhiteList("*");
            task.setKost2IsBlackList(true); // Black list (ignore all)
            taskDao.update(task);
            list = taskTree.getKost2List(task.getId());
            assertNull(list);
            task.setKost2BlackWhiteList("-");
            task.setKost2IsBlackList(false); // White list
            taskDao.update(task);
            list = taskTree.getKost2List(task.getId());
            assertNull(list);
            task.setKost2BlackWhiteList("*");
            task.setKost2IsBlackList(false); // White list
            taskDao.update(task);
            list = taskTree.getKost2List(task.getId());
            assertEquals(4, list.size());
            return null;
        });
    }

    private void assertKost2(final Kost2DO expected, final Kost2DO actual) {
        assertArrayEquals(new Integer[]{expected.getNummernkreis(), expected.getBereich(),
                        expected.getTeilbereich(), expected.getKost2Art().getId().intValue()},
                new Integer[]{actual.getNummernkreis(), actual.getBereich(),
                        actual.getTeilbereich(), actual.getKost2Art().getId().intValue()},
                "Kost2DO not expected.");
    }
}
