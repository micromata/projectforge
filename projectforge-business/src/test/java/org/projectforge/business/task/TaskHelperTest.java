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
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TaskHelperTest extends AbstractTestBase {
    @Autowired
    private ProjektDao projektDao;

    @Autowired
    private TaskTree taskTree;

    @Test
    public void normalizeKost2BlackWhiteList() {
        final TaskDO task = new TaskDO();
        task.setKost2BlackWhiteList(null);
        assertNull(TaskHelper.normalizeKost2BlackWhiteList(task));
        task.setKost2BlackWhiteList("");
        assertEquals("", TaskHelper.normalizeKost2BlackWhiteList(task));
        task.setKost2BlackWhiteList("1");
        assertEquals("1", TaskHelper.normalizeKost2BlackWhiteList(task));
        task.setKost2BlackWhiteList("5.212.01.12, 45;  .89");
        assertEquals(".89,45,5.212.01.12",
                TaskHelper.normalizeKost2BlackWhiteList(task));
        task.setKost2BlackWhiteList("5.212.01.12,, 45;  .89");
        assertEquals(".89,45,5.212.01.12",
                TaskHelper.normalizeKost2BlackWhiteList(task));
        task.setKost2BlackWhiteList("5.212.01.12, , 45;  .89,45");
        assertEquals(".89,45,5.212.01.12",
                TaskHelper.normalizeKost2BlackWhiteList(task));
    }

    @Test
    public void addKost2() {
        logon(AbstractTestBase.TEST_FINANCE_USER);
        persistenceService.runInTransaction(context -> {
            final TaskDO task1 = initTestDB.addTask("addKost2", "root");
            final ProjektDO projekt = new ProjektDO();
            projekt.setName("addKost2");
            projekt.setInternKost2_4(128);
            projekt.setNummer(5);
            projekt.setTask(task1);
            projektDao.insert(projekt);
            final Kost2ArtDO kost2Art = new Kost2ArtDO().withId(42L);
            final Kost2DO kost = new Kost2DO();
            kost.setNummernkreis(4);
            kost.setBereich(128);
            kost.setTeilbereich(5);
            kost.setKost2Art(kost2Art);
            assertEquals("42", TaskHelper.addKost2(taskTree, task1, kost));
            task1.setKost2BlackWhiteList("12,6.001.02.89,12");
            assertEquals("12,42,6.001.02.89",
                    TaskHelper.addKost2(taskTree, task1, kost));
            final TaskDO task2 = new TaskDO();
            assertEquals("4.128.05.42", TaskHelper.addKost2(taskTree, task2, kost));
            task2.setKost2BlackWhiteList("12,6.001.02.89");
            assertEquals("12,4.128.05.42,6.001.02.89", TaskHelper.addKost2(taskTree, task2, kost));
            return null;
        });
    }
}
