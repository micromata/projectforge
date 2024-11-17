/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.excel.importer.ImportStatus;
import de.micromata.merlin.excel.importer.ImportStorage;
import de.micromata.merlin.excel.importer.ImportedElement;
import de.micromata.merlin.excel.importer.ImportedSheet;
import net.fortuna.ical4j.model.component.VEvent;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.ical.ICalParser;
import org.projectforge.business.teamcal.ical.VEventUtils;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.utils.MyImportedElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Service
public class TeamCalImportDao {
  @Autowired
  private TeamEventService eventService;

  /**
   * Size of bulk inserts. If this value is too large, exceptions are expected and as more small the value is so as more
   * slowly is the insert process.
   */
  private static final int INSERT_BLOCK_SIZE = 50;

  private static final String[] DIFF_PROPERTIES = {"subject", "location", "allDay", "startDate", "endDate", "note",
          "recurrenceRule",
          "recurrenceUntil"};

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamCalImportDao.class);

  @Autowired
  private TeamEventDao teamEventDao;

  public ImportStorage<TeamEventDO> importEvents(final InputStream is, final String filename) {
    ICalParser parser = new ICalParser();

    final List<TeamEventDO> events = parser.parse(is);
    events.forEach(teamEventDO -> eventService.fixAttendees(teamEventDO));

    return importEvents(events, filename);
  }

  public ImportStorage<TeamEventDO> importEvents(final List<VEvent> vEvents) {
    final List<TeamEventDO> events = new LinkedList<>();
    for (final VEvent vEvent : vEvents) {
      events.add(VEventUtils.convertToEventDO(vEvent));
    }
    events.forEach(teamEventDO -> eventService.fixAttendees(teamEventDO));

    return importEvents(events, "none");
  }

  private ImportStorage<TeamEventDO> importEvents(final List<TeamEventDO> events, final String filename) {
    log.info("Uploading ics file: '" + filename + "'...");
    final ImportStorage<TeamEventDO> storage = new ImportStorage<>();
    storage.setFilename(filename);

    final ImportedSheet<TeamEventDO> importedSheet = new ImportedSheet<>(storage);
    importedSheet.setName(getSheetName());
    storage.addSheet(importedSheet);

    int row = 0;
    for (final TeamEventDO event : events) {
      importedSheet.getLogger().incrementSuccesscounter();
      final MyImportedElement<TeamEventDO> element = new MyImportedElement<>(importedSheet, row++,
              TeamEventDO.class, DIFF_PROPERTIES);
      element.setValue(event);
      importedSheet.addElement(element);
    }
    log.info("Uploading of ics file '" + filename + "' done. " + importedSheet.getLogger().getSuccessCounter() + " events read.");
    return storage;
  }

  @SuppressWarnings("unchecked")
  public void reconcile(final ImportStorage<?> storage, final ImportedSheet<?> sheet, final Long teamCalId) {
    Validate.notNull(storage.getSheets());
    Validate.notNull(sheet);
    reconcile((ImportedSheet<TeamEventDO>) sheet, teamCalId);
    sheet.setNumberOfCommittedElements(-1);
  }

  @SuppressWarnings("unchecked")
  public void commit(final ImportStorage<?> storage, final ImportedSheet<?> sheet, final Long teamCalId) {
    Validate.notNull(storage.getSheets());
    Validate.notNull(sheet);
    Validate.isTrue(sheet.getStatus() == ImportStatus.RECONCILED);
    final int no = commit((ImportedSheet<TeamEventDO>) sheet, teamCalId);
    sheet.setNumberOfCommittedElements(no);
    sheet.setStatus(ImportStatus.IMPORTED);
  }

  String getSheetName() {
    return ThreadLocalUserContext.getLocalizedString("plugins.teamcal.events");
  }

  private void reconcile(final ImportedSheet<TeamEventDO> sheet, final Long teamCalId) {
    for (final ImportedElement<TeamEventDO> el : sheet.getElements()) {
      final TeamEventDO event = el.getValue();
      teamEventDao.setCalendar(event, teamCalId);
      final TeamEventDO dbEvent = teamEventDao.getByUid(teamCalId, event.getUid());
      el.setOldValue(dbEvent);
    }
    sheet.setStatus(ImportStatus.RECONCILED);
    sheet.calculateStatistics();
  }

  private int commit(final ImportedSheet<TeamEventDO> sheet, final Long teamCalId) {
    log.info("Commit team events called");
    final Collection<TeamEventDO> col = new ArrayList<TeamEventDO>();
    for (final ImportedElement<TeamEventDO> el : sheet.getElements()) {
      final TeamEventDO event = el.getValue();
      if (HibernateUtils.shortenProperties(TeamEventDO.class, event, "note", "location", "subject", "organizer") == true) {
        log.info("Properties of the event were shortened: " + event);
      }
      final TeamEventDO dbEvent = teamEventDao.getByUid(teamCalId, event.getUid());
      if (dbEvent != null) {
        event.setId(dbEvent.getId());
        if (el.getSelected() == true) {
          col.add(event);
        }
      } else if (el.getSelected() == true) {
        col.add(event);
      }
    }
    teamEventDao.insertOrUpdate(col, INSERT_BLOCK_SIZE, false);
    return col.size();
  }
}
