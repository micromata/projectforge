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

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.ical.ICalParser;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.utils.ImportStatus;
import org.projectforge.framework.persistence.utils.ImportStorage;
import org.projectforge.framework.persistence.utils.ImportedElement;
import org.projectforge.framework.persistence.utils.ImportedSheet;
import org.projectforge.framework.utils.ActionLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
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

  public ImportStorage<TeamEventDO> importEvents(final Calendar calendar, final String filename, final ActionLog actionLog) {
    ICalParser parser = ICalParser.parseAllFields();
    parser.parse(calendar);
    final List<TeamEventDO> events = parser.getExtractedEvents();
    events.forEach(teamEventDO -> eventService.fixAttendees(teamEventDO));

    return importEvents(events, filename, actionLog);
  }

  public ImportStorage<TeamEventDO> importEvents(final List<VEvent> vEvents, final ActionLog actionLog) {
    final Calendar calendar = new Calendar();
    vEvents.forEach(event -> calendar.getComponents().add(event));

    ICalParser parser = ICalParser.parseAllFields();
    parser.parse(calendar);
    final List<TeamEventDO> events = parser.getExtractedEvents();
    events.forEach(teamEventDO -> eventService.fixAttendees(teamEventDO));

    return importEvents(events, "none", actionLog);
  }

  private ImportStorage<TeamEventDO> importEvents(final List<TeamEventDO> events, final String filename,
                                                  final ActionLog actionLog) {
    log.info("Uploading ics file: '" + filename + "'...");
    final ImportStorage<TeamEventDO> storage = new ImportStorage<TeamEventDO>();
    storage.setFilename(filename);

    final ImportedSheet<TeamEventDO> importedSheet = new ImportedSheet<TeamEventDO>();
    importedSheet.setName(getSheetName());
    storage.addSheet(importedSheet);

    for (final TeamEventDO event : events) {
      actionLog.incrementCounterSuccess();
      final ImportedElement<TeamEventDO> element = new ImportedElement<TeamEventDO>(storage.nextVal(),
              TeamEventDO.class, DIFF_PROPERTIES);
      element.setValue(event);
      importedSheet.addElement(element);
    }
    log.info("Uploading of ics file '" + filename + "' done. " + actionLog.getCounterSuccess() + " events read.");
    return storage;
  }

  @SuppressWarnings("unchecked")
  public void reconcile(final ImportStorage<?> storage, final ImportedSheet<?> sheet, final Integer teamCalId) {
    Validate.notNull(storage.getSheets());
    Validate.notNull(sheet);
    reconcile((ImportedSheet<TeamEventDO>) sheet, teamCalId);
    sheet.setNumberOfCommittedElements(-1);
  }

  @SuppressWarnings("unchecked")
  public void commit(final ImportStorage<?> storage, final ImportedSheet<?> sheet, final Integer teamCalId) {
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

  private void reconcile(final ImportedSheet<TeamEventDO> sheet, final Integer teamCalId) {
    for (final ImportedElement<TeamEventDO> el : sheet.getElements()) {
      final TeamEventDO event = el.getValue();
      teamEventDao.setCalendar(event, teamCalId);
      final TeamEventDO dbEvent = teamEventDao.getByUid(teamCalId, event.getUid());
      el.setOldValue(dbEvent);
    }
    sheet.setStatus(ImportStatus.RECONCILED);
    sheet.calculateStatistics();
  }

  private int commit(final ImportedSheet<TeamEventDO> sheet, final Integer teamCalId) {
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
        event.setTenant(dbEvent.getTenant());
        if (el.isSelected() == true) {
          col.add(event);
        }
      } else if (el.isSelected() == true) {
        col.add(event);
      }
    }
    teamEventDao.internalSaveOrUpdate(teamEventDao, col, INSERT_BLOCK_SIZE);
    return col.size();
  }
}
