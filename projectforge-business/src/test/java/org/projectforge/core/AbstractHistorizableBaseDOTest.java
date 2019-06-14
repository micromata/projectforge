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

package org.projectforge.core;

import java.util.Set;

import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.micromata.genome.db.jpa.history.api.HistoryServiceManager;

public class AbstractHistorizableBaseDOTest extends AbstractTestBase
{
  @Autowired
  private PfEmgrFactory emf;

  @Test
  public void testNonHistorizableProperties()
  {
    final TaskDO task = new TaskDO();
    Set<String> set = HistoryServiceManager.get().getHistoryService().getNoHistoryProperties(emf,
        task.getClass());
    Assertions.assertEquals(2, set.size());
    Assertions.assertTrue(set.contains("lastUpdate"));
    Assertions.assertTrue(set.contains("created"));
    Assertions.assertTrue(set.contains("lastUpdate"));
    Assertions.assertTrue(set.contains("created"));
    Assertions.assertFalse(set.contains("title"));

    final AuftragDO order = new AuftragDO();
    set = HistoryServiceManager.get().getHistoryService().getNoHistoryProperties(emf, order.getClass());
    Assertions.assertEquals(3, set.size());
    //    not, because ransient Assertions.assertTrue(set.contains("uiStatus"));
    Assertions.assertTrue(set.contains("uiStatusAsXml"));
    Assertions.assertFalse(set.contains("subject"));
  }
}
