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

package org.projectforge.web.humanresources;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.humanresources.HRPlanningDO;
import org.projectforge.business.humanresources.HRPlanningDao;
import org.projectforge.business.humanresources.HRPlanningEntryDO;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.*;
import org.slf4j.Logger;

import java.time.LocalDate;

/**
 * @author Mario Groß (m.gross@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@EditPage(defaultReturnPage = HRPlanningListPage.class)
public class HRPlanningEditPage extends AbstractEditPage<HRPlanningDO, HRPlanningEditForm, HRPlanningDao> implements ISelectCallerPage {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRPlanningEditPage.class);

  private static final long serialVersionUID = -8192471994161712577L;

  private static final String SESSION_KEY_RECENT_WEEK = "HRPlanningEditPage.recentWeek";

  public HRPlanningEditPage(final PageParameters parameters) {
    super(parameters, "hr.planning");
    final Integer userId = WicketUtils.getAsInteger(parameters, WebConstants.PARAMETER_USER_ID);
    Long millis = WicketUtils.getAsLong(parameters, WebConstants.PARAMETER_DATE);
    final LocalDate week;
    if (millis == null) {
      final Object obj = getUserPrefEntry(SESSION_KEY_RECENT_WEEK);
      if (obj instanceof Long) {
        millis = (Long) obj;
      }
    }
    if (millis != null) {
      week = PFDateTime.from(millis, null, null, PFDateTime.NumberFormat.EPOCH_MILLIS).getLocalDate();
    } else {
      week = null;
    }
    HRPlanningDO planning = null;
    if (userId != null && week != null) {
      // Check if there exists already an entry (deleted or not):
      planning = getBaseDao().getEntry(userId, week);
    }
    if (planning != null) {
      super.init(planning);
    } else {
      super.init();
    }
    if (userId != null) {
      getBaseDao().setUser(getData(), userId);
    }
    if (getData().getId() == null) {
      // New entry:
      final PFDay day = PFDay.fromOrNow(week).getBeginOfWeek();
      getData().setWeek(day.getLocalDate());
    }
  }

  @Override
  protected HRPlanningDao getBaseDao() {
    return WicketSupport.get(HRPlanningDao.class);
  }

  @Override
  protected HRPlanningEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final HRPlanningDO data) {
    return new HRPlanningEditForm(this, data);
  }

  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate() {
    putUserPrefEntry(SESSION_KEY_RECENT_WEEK, getData().getWeek(), true); // Store as recent date.
    return null;
  }

  @Override
  public void select(final String property, final Object selectedValue) {
    if (property.startsWith("projektId:")) {
      try {
        final Integer idx = NumberHelper.parseInteger(property.split(":")[1]);
        final Integer uiId = NumberHelper.parseInteger(property.split(":")[2]);
        final HRPlanningEntryDO entry = getData().getEntry(idx);
        getBaseDao().setProjekt(entry, (Integer) selectedValue);
        form.projektSelectPanels.get(uiId).getTextField().modelChanged();
      } catch (final IndexOutOfBoundsException ex) {
        log.error("Oups, idx not supported: " + ex.getMessage(), ex);
      }
    } else if ("userId".equals(property)) {
      getBaseDao().setUser(getData(), (Integer) selectedValue);
      form.refresh();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property) {
    if (property.startsWith("projektId:")) {
      final Integer idx = NumberHelper.parseInteger(property.split(":")[1]);
      final HRPlanningEntryDO entry = getData().getEntry(idx);
      entry.setProjekt(null);
      form.projektSelectPanels.get(idx).getTextField().modelChanged();
      // form.refresh();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
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
  protected Logger getLogger() {
    return log;
  }
}
