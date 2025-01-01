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

package org.projectforge.web.timesheet;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.task.TaskTreeHelper;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetRecentEntry;
import org.projectforge.business.timesheet.TimesheetRecentService;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.calendar.CalendarPage;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.teamcal.integration.TeamcalTimesheetPluginComponentHook;
import org.projectforge.web.user.UserPrefEditPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.WicketUtils;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.util.*;

@EditPage(defaultReturnPage = TimesheetListPage.class)
public class TimesheetEditPage extends AbstractEditPage<TimesheetDO, TimesheetEditForm, TimesheetDao>
        implements ISelectCallerPage {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TimesheetEditPage.class);

  protected static final String[] BOOKMARKABLE_SELECT_PROPERTIES = new String[]{"p.taskId|task", "p.userId|user",
          "p.kost2Id|kost2"};

  /**
   * Key for preset the start date.
   */
  public static final String PARAMETER_KEY_START_DATE_IN_MILLIS = "startMillis";

  /**
   * Key for preset the stop date.
   */
  public static final String PARAMETER_KEY_STOP_DATE_IN_MILLIS = "stopMillis";

  /**
   * Key for moving start date.
   */
  public static final String PARAMETER_KEY_NEW_START_DATE = "newStartDate";

  /**
   * Key for moving start date.
   */
  public static final String PARAMETER_KEY_NEW_END_DATE = "newEndDate";

  /**
   * Key for preset the description.
   */
  public static final String PARAMETER_KEY_DESCRIPTION = "description";

  /**
   * Key for preset the task id.
   */
  public static final String PARAMETER_KEY_TASK_ID = TimesheetListPage.PARAMETER_KEY_TASK_ID;

  /**
   * Key for preset the user.
   */
  public static final String PARAMETER_KEY_USER = "user";

  /**
   * Max length of combo box entries.
   */
  static final int MAX_LENGTH_OF_RECENT_TASKS = 80;

  /**
   * The first recent block contains entries in chronological order.
   */
  static final int SIZE_OF_FIRST_RECENT_BLOCK = 5;

  private static final long serialVersionUID = -8192471994161712577L;

  private static final TeamcalTimesheetPluginComponentHook[] HOOK_ARRAY = {new TeamcalTimesheetPluginComponentHook()};

  public TimesheetEditPage(final TimesheetDO timesheet) {
    super(new PageParameters(), "timesheet");
    init(timesheet);
  }

  public TimesheetEditPage(final PageParameters parameters) {
    super(parameters, "timesheet");
    init();
  }

  void preInit() {
    if (isNew() == true) {
      final PageParameters parameters = getPageParameters();
      final Long taskId = WicketUtils.getAsLong(parameters, PARAMETER_KEY_TASK_ID);
      if (taskId != null) {
        getBaseDao().setTask(getData(), taskId);
      }
      final Long startTimeInMillis = WicketUtils.getAsLong(parameters, PARAMETER_KEY_START_DATE_IN_MILLIS);
      final Long stopTimeInMillis = WicketUtils.getAsLong(parameters, PARAMETER_KEY_STOP_DATE_IN_MILLIS);
      if (startTimeInMillis != null) {
        getData().setStartDate(startTimeInMillis);
        if (stopTimeInMillis == null) {
          getData().setStopTime(new Timestamp(startTimeInMillis)); // Default is time sheet with zero duration.
        }
      }
      if (stopTimeInMillis != null) {
        getData().setStopTime(new Timestamp(stopTimeInMillis));
        if (startTimeInMillis == null) {
          getData().setStartDate(stopTimeInMillis); // Default is time sheet with zero duration.
        }
      }
      final String description = WicketUtils.getAsString(parameters, PARAMETER_KEY_DESCRIPTION);
      if (description != null) {
        getData().setDescription(description);
      }
      final long userId = WicketUtils.getAsLong(parameters, PARAMETER_KEY_USER, -1L);
      if (userId != -1) {
        WicketSupport.get(TimesheetDao.class).setUser(getData(), userId);
      }
    } else {
      final Long newStartTimeInMillis = WicketUtils.getAsLong(getPageParameters(), PARAMETER_KEY_NEW_START_DATE);
      final Long newStopTimeInMillis = WicketUtils.getAsLong(getPageParameters(), PARAMETER_KEY_NEW_END_DATE);
      if (newStartTimeInMillis != null) {
        getData().setStartDate(newStartTimeInMillis);
      }
      if (newStopTimeInMillis != null) {
        getData().setStopTime(new Timestamp(newStopTimeInMillis));
      }
    }

    if (isNew() == true) {
      final TimesheetRecentEntry recent = WicketSupport.get(TimesheetRecentService.class).getRecentTimesheet();
      if (recent != null) {
        if (getData().getTaskId() == null) {
          getBaseDao().setTask(getData(), recent.getTaskId());
        }
        if (getData().getKost2Id() == null) {
          getBaseDao().setKost2(getData(), recent.getKost2Id());
        }
        if (StringUtils.isBlank(getData().getLocation())) {
          getData().setLocation(recent.getLocation());
        }
        if (StringUtils.isBlank(getData().getDescription())) {
          getData().setDescription(recent.getDescription());
        }
      }
      if (getData().getUserId() == null) {
        getBaseDao().setUser(getData(), getUser().getId());
      }
    }
  }

  @Override
  protected TimesheetDao getBaseDao() {
    return WicketSupport.get(TimesheetDao.class);
  }

  @Override
  public void setResponsePage() {
    super.setResponsePage();
    if (returnToPage instanceof CalendarPage) {
      // Display the date of this time sheet in the CalendarPage (useful if the time sheet was moved).
      ((CalendarPage) returnToPage).setStartDate(getData().getStartTime());
    }
  }

  @Override
  protected TimesheetEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final TimesheetDO data) {
    return new TimesheetEditForm(this, data);
  }

  /**
   * Return list for table with all recent used time sheets.
   *
   * @return
   */
  protected List<TimesheetDO> getRecentTimesheets() {
    final List<TimesheetRecentEntry> recentEntries = WicketSupport.get(TimesheetRecentService.class).getRecentTimesheets();
    final List<TimesheetDO> list = new ArrayList<TimesheetDO>();
    if (CollectionUtils.isNotEmpty(recentEntries)) {
      for (final TimesheetRecentEntry entry : recentEntries) {
        final TimesheetDO sheet = getRecentSheet(entry);
        list.add(sheet);
      }
      Collections.sort(list, new Comparator<TimesheetDO>() {
        @Override
        public int compare(final TimesheetDO t1, final TimesheetDO t2) {
          final Kost2DO kost1 = t1.getKost2();
          final Kost2DO kost2 = t2.getKost2();
          final ProjektDO project1 = kost1 != null ? kost1.getProjekt() : null;
          final ProjektDO project2 = kost2 != null ? kost2.getProjekt() : null;
          final String kunde1 = project1 != null && project1.getKunde() != null ? project1.getKunde().getName() : null;
          final String kunde2 = project2 != null && project2.getKunde() != null ? project2.getKunde().getName() : null;
          return new CompareToBuilder().append(kunde1, kunde2)
                  .append(project1 != null ? project1.getName() : null, project2 != null ? project2.getName() : null)
                  .append(t1.getTask() != null ? t1.getTask().getTitle() : null,
                          t2.getTask() != null ? t2.getTask().getTitle() : null)
                  .toComparison();
        }
      });
      // Don't show recent block for new users if all entries are already displayed.
      if (recentEntries.size() > SIZE_OF_FIRST_RECENT_BLOCK) {
        int i = 0;
        for (final TimesheetRecentEntry entry : recentEntries) {
          final TimesheetDO sheet = getRecentSheet(entry);
          list.add(i, sheet);
          if (i++ >= SIZE_OF_FIRST_RECENT_BLOCK) {
            break;
          }
        }
      }
    }
    return list;
  }

  /**
   * Gets the recent locations.
   */
  public List<String> getRecentLocations() {
    return WicketSupport.get(TimesheetRecentService.class).getRecentLocations();
  }

  private TimesheetDO getRecentSheet(final TimesheetRecentEntry entry) {
    final TimesheetDO sheet = new TimesheetDO();
    final TaskDO task = getTaskTree().getTaskById(entry.getTaskId());
    sheet.setTask(task);
    final Kost2DO kost2 = WicketSupport.getKostCache().getKost2(entry.getKost2Id());
    sheet.setKost2(kost2);
    sheet.setDescription(entry.getDescription());
    sheet.setLocation(entry.getLocation());
    final PFUserDO user = UserGroupCache.getInstance().getUser(entry.getUserId());
    sheet.setUser(user);
    return sheet;
  }

  protected TimesheetRecentEntry getTimesheetRecentEntry() {
    return form.timesheetPageSupport.getTimesheetRecentEntry();
  }

  /**
   * Sets the id of the current time sheet to null and the user to the logged in user and returns to the input page.
   * This results in adding a new time sheet. (Does not clone TimesheetEditAction!)
   *
   * @see org.projectforge.web.wicket.AbstractEditPage#cloneData()
   */
  @Override
  protected void cloneData() {
    super.cloneData();
    final TimesheetDO timesheet = getData();
    getBaseDao().setUser(timesheet, getUser().getId());
    form.userSelectPanel.markTextFieldModelAsChanged();
    setResponsePage(new TimesheetEditPage(form.getData()));
  }

  @Override
  public void select(final String property, final Object selectedValue) {
    if ("taskId".equals(property) == true) {
      final Long id;
      if (selectedValue instanceof String) {
        id = NumberHelper.parseLong((String) selectedValue);
      } else {
        id = (Long) selectedValue;
      }
      getBaseDao().setTask(getData(), id);
      form.refresh();
    } else if ("userId".equals(property) == true) {
      final Long id;
      if (selectedValue instanceof String) {
        id = NumberHelper.parseLong((String) selectedValue);
      } else {
        id = (Long) selectedValue;
      }
      getBaseDao().setUser(getData(), id);
    } else if ("kost2Id".equals(property) == true) {
      final Long id;
      if (selectedValue instanceof String) {
        id = NumberHelper.parseLong((String) selectedValue);
      } else {
        id = (Long) selectedValue;
      }
      getBaseDao().setKost2(getData(), id);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property) {
    if ("taskId".equals(property) == true) {
      getData().setTask(null);
      form.refresh();
    } else if ("userId".equals(property) == true) {
      getData().setUser(null);
      form.refresh();
    } else if ("kost2Id".equals(property) == true) {
      getData().setKost2(null);
      form.refresh();
    } else {
      log.error("Property '" + property + "' not supported for unselection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  @Override
  public void cancelSelection(final String property) {
    // Do nothing.
  }

  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate() {
    // clean ignore location if needed
    if (form != null && form.getFilter() != null && getData() != null) {
      form.getFilter().removeIgnoredLocation(getData().getLocation());
    }
    // Save time sheet as recent time sheet
    final TimesheetDO timesheet = getData();
    WicketSupport.get(TimesheetRecentService.class).addRecentTimesheet(timesheet);
    // Does the user want to store this time sheet as template?
    if (BooleanUtils.isTrue(form.saveAsTemplate) == true) {
      final UserPrefEditPage userPrefEditPage = new UserPrefEditPage(UserPrefArea.TIMESHEET_TEMPLATE, getData());
      userPrefEditPage.setReturnToPage(this.returnToPage);
      return userPrefEditPage;
    }
    return null;
  }

  @Override
  protected String[] getBookmarkableInitialProperties() {
    return BOOKMARKABLE_SELECT_PROPERTIES;
  }

  @Override
  protected void onPreEdit() {
  }

  @Override
  protected Logger getLogger() {
    return log;
  }

  public static List<TimesheetPluginComponentHook> getPluginHooks() {
    return Collections.unmodifiableList(Arrays.asList(HOOK_ARRAY));
  }

  private TaskTree getTaskTree() {
    return TaskTreeHelper.getTaskTree();
  }
}
