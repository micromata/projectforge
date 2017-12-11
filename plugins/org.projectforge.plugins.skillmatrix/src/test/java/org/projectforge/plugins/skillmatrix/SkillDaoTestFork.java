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

package org.projectforge.plugins.skillmatrix;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.test.AbstractTestBase;
import org.projectforge.web.registry.WebRegistry;
import org.projectforge.web.wicket.WicketApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SkillDaoTestFork extends AbstractTestBase
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SkillDaoTestFork.class);

  @Autowired
  private SkillDao skillDao;

  @Autowired
  private PluginAdminService pluginAdminService;

  private Integer skillId;

  @Override
  @BeforeClass
  public void setUp()
  {
    super.setUp();
    I18nHelper.addBundleName(WicketApplication.RESOURCE_BUNDLE_NAME);
    WebRegistry.getInstance().init();
    pluginAdminService.initializeAllPluginsForUnittest();
  }

  @Test
  public void accessTest()
  {
    final SkillTestHelper testHelper = new SkillTestHelper(initTestDB);
    final SkillDO skill = testHelper.prepareUsersAndGroups("skill", this, skillDao);
    skillId = skill.getId();
    logon(testHelper.getOwner());
    assertEquals("skill.title", skillDao.getById(skillId).getTitle());
    checkSelectAccess(true, testHelper.getOwner(), testHelper.getFullUser1(), testHelper.getReadonlyUser1());
    checkSelectAccess(false, testHelper.getNoAccessUser(), testHelper.getFullUser2(), testHelper.getReadonlyUser2());
  }

  private void checkSelectAccess(final boolean access, final PFUserDO... users)
  {
    for (final PFUserDO user : users) {
      logon(user);
      try {
        assertEquals("skill.title", skillDao.getById(skillId).getTitle());
        if (access == false) {
          fail("Select-AccessException expected for user: " + user.getUsername());
        }
      } catch (final AccessException ex) {
        if (access == true) {
          fail("Unexpected Selected-AccessException for user: " + user.getUsername());
        } else {
          log.info("Last AccessException was expected (OK).");
        }
      }
    }
  }

}
