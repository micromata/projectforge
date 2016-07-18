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

package org.projectforge.continuousdb;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

public class SystemUpdaterTest
{
  @Test
  public void updateTest()
  {
    UpdaterConfiguration config = new UpdaterConfiguration();
    config.setDatabaseUpdateDao(new DatabaseUpdateService());
    SystemUpdater updater = new SystemUpdater(config);
    updater.testRegister(createEntry("a", "1.0.0", "2011-02-01", UpdatePreCheckStatus.READY_FOR_UPDATE));
    assertFalse("Version is expected to be ready for update.", updater.isUpdated());
    updater.testRegister(createEntry("a", "1.1.0", "2011-02-05", UpdatePreCheckStatus.ALREADY_UPDATED));
    assertTrue("Older version shouldn't be tested.", updater.isUpdated());
    updater.testRegister(createEntry("b", "1.0.0", "2011-01-01", UpdatePreCheckStatus.ALREADY_UPDATED));
    assertTrue("Older version shouldn't be tested.", updater.isUpdated());
    UpdateEntry b = createEntry("b", "1.1.0", "2011-01-02", UpdatePreCheckStatus.READY_FOR_UPDATE);
    updater.testRegister(b);
    assertFalse("b 1.1.0 should be detected as ready for update.", updater.isUpdated());

    updater.update(b);
    Assert.assertFalse(updater.isUpdating());
    Assert.assertFalse(updater.isUpdated());
    updater.runAllPreChecks();

    updater = new SystemUpdater(config)
    {
      @Override
      public void runAllPreChecks()
      {
        throw new Error();
      }
    };
    updater.testRegister(b);
    updater.update(b);

  }

  @SuppressWarnings("serial")
  private UpdateEntry createEntry(final String region, final String version, final String isoDate,
      final UpdatePreCheckStatus status)
  {
    return new UpdateEntryImpl(region, version, isoDate, "...")
    {

      @Override
      public UpdateRunningStatus runUpdate()
      {
        return null;
      }

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        return status;
      }
    };
  }
}
