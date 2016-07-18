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

package org.projectforge.web.admin;

import java.util.Date;
import java.util.List;

import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.login.Login;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.excel.ExportSheet;
import org.projectforge.excel.ExportWorkbook;
import org.projectforge.framework.access.AccessCheckerImpl;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.database.DatabaseUpdateDO;
import org.projectforge.framework.persistence.database.MyDatabaseUpdateService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

public class SystemUpdatePage extends AbstractSecuredPage
{
  private static final long serialVersionUID = -7624191773850329338L;

  @SpringBean
  protected MyDatabaseUpdateService myDatabaseUpdater;

  private final SystemUpdateForm form;

  @SuppressWarnings("serial")
  public SystemUpdatePage(final PageParameters parameters)
  {
    super(parameters);
    myDatabaseUpdater.getSystemUpdater().runAllPreChecks();
    form = new SystemUpdateForm(this);
    body.add(form);
    form.init();
    final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
        new Link<Void>(ContentMenuEntryPanel.LINK_ID)
        {
          @Override
          public void onClick()
          {
            checkAdminUser();
            final MyDatabaseUpdateService databaseUpdateDao = myDatabaseUpdater.getDatabaseUpdateService();
            final List<DatabaseUpdateDO> updateEntries = databaseUpdateDao.getUpdateHistory();
            final ExportWorkbook workbook = new ExportWorkbook();
            final ExportSheet sheet = workbook.addSheet("Update history");
            sheet.getContentProvider().setColWidths(new int[] { 20, 10, 20, 15, 50, 20 });
            sheet.getContentProvider().putFormat(java.sql.Timestamp.class, "YYYY-MM-DD hh:mm:ss");
            sheet.setPropertyNames(
                new String[] { "regionId", "versionString", "updateDate", "executedBy.username", "description",
                    "executionResult" });
            sheet.addRow().setValues("region id", "version", "update date", "executed by", "description",
                "execution result");
            sheet.addRows(updateEntries);
            final String filename = "ProjectForge-UpdateHistory_" + DateHelper.getDateAsFilenameSuffix(new Date())
                + ".xls";
            final byte[] xls = workbook.getAsByteArray();
            DownloadUtils.setDownloadTarget(xls, filename);
          };
        }, getString("system.update.downloadUpdateHistoryAsXls"));
    addContentMenuEntry(menu);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#onBeforeRender()
   */
  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    refresh();
  }

  protected void update(final UpdateEntry updateEntry)
  {
    checkAdminUser();
    accessChecker.checkRestrictedOrDemoUser();
    myDatabaseUpdater.getSystemUpdater().update(updateEntry);
    refresh();
  }

  protected void refresh()
  {
    checkAdminUser();
    myDatabaseUpdater.getSystemUpdater().runAllPreChecks();
    form.updateEntryRows();
  }

  private void checkAdminUser()
  {
    if (Login.getInstance().isAdminUser(ThreadLocalUserContext.getUser()) == false) {
      throw new AccessException(AccessCheckerImpl.I18N_KEY_VIOLATION_USER_NOT_MEMBER_OF,
          ProjectForgeGroup.ADMIN_GROUP.getKey());
    }
  }

  @Override
  protected String getTitle()
  {
    return getString("system.update.title");
  }
}
