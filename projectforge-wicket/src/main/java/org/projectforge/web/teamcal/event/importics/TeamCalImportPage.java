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

package org.projectforge.web.teamcal.event.importics;

import de.micromata.merlin.excel.importer.ImportStorage;
import de.micromata.merlin.excel.importer.ImportedSheet;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.core.importstorage.AbstractImportPage;
import org.projectforge.web.wicket.WicketUtils;

import java.io.InputStream;
import java.util.List;

public class TeamCalImportPage extends AbstractImportPage<TeamCalImportForm>
{
  private static final long serialVersionUID = 4717760936874814502L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamCalImportPage.class);

  public static String PARAM_KEY_TEAM_CAL_ID = "teamCalId";

  public TeamCalImportPage(final PageParameters parameters)
  {
    super(parameters);
    form = new TeamCalImportForm(this);
    body.add(form);
    final Integer calId = WicketUtils.getAsInteger(parameters, PARAM_KEY_TEAM_CAL_ID);
    if (calId != null) {
      form.calendar = WicketSupport.get(TeamCalDao.class).find(calId);
    }
    form.init();
  }

  public void setEventsToImport(final List<VEvent> events)
  {
    checkAccess();
    final ImportStorage<TeamEventDO> storage = WicketSupport.get(TeamCalImportDao.class).importEvents(events);
    setStorage(storage);
  }

  protected void importEvents()
  {
    checkAccess();
    final FileUpload fileUpload = form.fileUploadField.getFileUpload();
    if (fileUpload != null) {
      try {
        final InputStream is = fileUpload.getInputStream();
        final String clientFilename = fileUpload.getClientFileName();
        final ImportStorage<TeamEventDO> storage = WicketSupport.get(TeamCalImportDao.class).importEvents(is, clientFilename);
        setStorage(storage);
      } catch (final Exception ex) {
        log.error(ex.getMessage(), ex);
        error("An error occurred (see log files for details): " + ex.getMessage());
        clear();
      } finally {
        fileUpload.closeStreams();
      }
    }
  }

  void reconcile()
  {
    final String name = WicketSupport.get(TeamCalImportDao.class).getSheetName();
    if (getStorage() == null) {
      return;
    }
    @SuppressWarnings("unchecked")
    final ImportedSheet<TeamEventDO> sheet = (ImportedSheet<TeamEventDO>) getStorage().getNamedSheet(name);
    if (sheet == null || !sheet.isReconciled()) {
      return;
    }
    reconcile(WicketSupport.get(TeamCalImportDao.class).getSheetName());
  }

  @Override
  protected ImportedSheet<?> reconcile(final String sheetName)
  {
    checkAccess();
    final ImportedSheet<?> sheet = super.reconcile(sheetName);
    WicketSupport.get(TeamCalImportDao.class).reconcile(getStorage(), sheet, form.getCalendarId());
    return sheet;
  }

  @Override
  protected ImportedSheet<?> commit(final String sheetName)
  {
    checkAccess();
    final ImportedSheet<?> sheet = super.commit(sheetName);
    WicketSupport.get(TeamCalImportDao.class).commit(getStorage(), sheet, form.getCalendarId());
    return sheet;
  }

  private void checkAccess()
  {
    getAccessChecker().checkRestrictedOrDemoUser();
  }

  @Override
  protected String getTitle()
  {
    return getString("plugins.teamcal.import.ics.title");
  }
}
