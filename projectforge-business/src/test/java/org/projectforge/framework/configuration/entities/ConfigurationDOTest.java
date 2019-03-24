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

package org.projectforge.framework.configuration.entities;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

import java.util.List;

import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.ConfigurationType;
import org.projectforge.test.AbstractTestNGBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class ConfigurationDOTest extends AbstractTestNGBase
{

  @Autowired
  private ConfigurationDao configurationDao;

  @Test
  public void testSingleEntry()
  {
    final ConfigurationDO conf = new ConfigurationDO().setType(ConfigurationType.STRING);
    conf.setStringValue("Hurzel");
    assertEquals("Hurzel", conf.getStringValue());
    conf.setStringValue("");
    assertEquals("", conf.getStringValue());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConfiguration()
  {
    ConfigurationDO config = configurationDao.getEntry(ConfigurationParam.MESSAGE_OF_THE_DAY);
    config = configurationDao.getEntry(ConfigurationParam.MESSAGE_OF_THE_DAY);
    assertNotNull(config);
    config = new ConfigurationDO().setType(ConfigurationType.STRING);
    config.setParameter("unknown");
    config.setValue("Hurzel");
    assertNotNull(config);
    configurationDao.internalSave(config);
    List<ConfigurationDO> list = (List<ConfigurationDO>) hibernateTemplate
        .find("from ConfigurationDO c where c.parameter = 'unknown'");
    config = list.get(0);
    assertEquals("Hurzel", config.getStringValue());
    configurationDao.checkAndUpdateDatabaseEntries();
    list = (List<ConfigurationDO>) hibernateTemplate.find("from ConfigurationDO c where c.parameter = 'unknown'");
    config = list.get(0);
    assertEquals("Entry should be deleted.", true, config.isDeleted());

    config = configurationDao.getEntry(ConfigurationParam.MESSAGE_OF_THE_DAY);
    configurationDao.internalMarkAsDeleted(config);
    configurationDao.checkAndUpdateDatabaseEntries();
    config = configurationDao.getEntry(ConfigurationParam.MESSAGE_OF_THE_DAY);
    assertEquals("Object should be restored.", false, config.isDeleted());
  }

  @Test
  public void checkTypes()
  {
    final ConfigurationDO config = new ConfigurationDO().setType(ConfigurationType.STRING);
    config.setStringValue("Hurzel");
    config.setType(ConfigurationType.STRING);
    try {
      config.setType(ConfigurationType.INTEGER);
      fail("Exception should be thrown.");
    } catch (final RuntimeException ex) {

    }
  }
}
