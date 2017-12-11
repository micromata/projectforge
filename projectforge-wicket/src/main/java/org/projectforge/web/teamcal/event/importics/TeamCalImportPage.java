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

package org.projectforge.web.teamcal.event.importics;

import java.io.InputStream;
import java.util.List;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.persistence.utils.ImportStorage;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.web.core.importstorage.AbstractImportPage;
import org.projectforge.web.wicket.WicketUtils;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;

public class TeamCalImportPage extends AbstractImportPage<TeamCalImportForm>
{
  private static final long serialVersionUID = 4717760936874814502L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamCalImportPage.class);

  public static String PARAM_KEY_TEAM_CAL_ID = "teamCalId";

  @SpringBean
  private TeamCalDao teamCalDao;

  @SpringBean
  private TeamCalImportDao teamCalImportDao;

  public TeamCalImportPage(final PageParameters parameters)
  {
    super(parameters);
    form = new TeamCalImportForm(this);
    body.add(form);
    final Integer calId = WicketUtils.getAsInteger(parameters, PARAM_KEY_TEAM_CAL_ID);
    if (calId != null) {
      form.calendar = teamCalDao.getById(calId);
    }
    form.init();
  }

  public void setEventsToImport(final List<VEvent> events)
  {
    checkAccess();
    final ImportStorage<TeamEventDO> storage = teamCalImportDao.importEvents(events, actionLog);
    setStorage(storage);
  }

  protected void importEvents()
  {
    checkAccess();
    final FileUpload fileUpload = form.fileUploadField.getFileUpload();
    if (fileUpload != null) {
      try {
        final InputStream is = fileUpload.getInputStream();
        actionLog.reset();
        final String clientFilename = fileUpload.getClientFileName();
        final CalendarBuilder builder = new CalendarBuilder();
        final Calendar calendar = builder.build(is);
        final ImportStorage<TeamEventDO> storage = teamCalImportDao.importEvents(calendar, clientFilename, actionLog);
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
    final String name = teamCalImportDao.getSheetName();
    if (getStorage() == null) {
      return;
    }
    @SuppressWarnings("unchecked")
    final ImportedSheet<TeamEventDO> sheet = (ImportedSheet<TeamEventDO>) getStorage().getNamedSheet(name);
    if (sheet == null || sheet.isReconciled() == false) {
      return;
    }
    reconcile(teamCalImportDao.getSheetName());
  }

  @Override
  protected ImportedSheet<?> reconcile(final String sheetName)
  {
    checkAccess();
    final ImportedSheet<?> sheet = super.reconcile(sheetName);
    teamCalImportDao.reconcile(getStorage(), sheet, form.getCalendarId());
    return sheet;
  }

  @Override
  protected ImportedSheet<?> commit(final String sheetName)
  {
    checkAccess();
    final ImportedSheet<?> sheet = super.commit(sheetName);
    teamCalImportDao.commit(getStorage(), sheet, form.getCalendarId());
    return sheet;
  }

  private void checkAccess()
  {
    accessChecker.checkRestrictedOrDemoUser();
  }

  @Override
  protected String getTitle()
  {
    return getString("plugins.teamcal.import.ics.title");
  }
}
