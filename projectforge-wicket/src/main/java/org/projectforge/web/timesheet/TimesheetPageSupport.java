/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetRecentEntry;
import org.projectforge.business.timesheet.TimesheetRecentService;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.*;

import java.io.Serializable;
import java.util.List;

/**
 * For sharing functionality between mobile and normal edit pages.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
class TimesheetPageSupport implements Serializable
{
  private static final long serialVersionUID = 9008998607656697576L;

  final AbstractSecuredBasePage page;

  private final AbstractGridBuilder< ? > gridBuilder;

  private final TimesheetDao timesheetDao;

  private final TimesheetDO timesheet;

  /**
   * Constructor for edit pages.
   */
  public TimesheetPageSupport(final AbstractSecuredBasePage page, final AbstractGridBuilder< ? > gridBuilder,
      final TimesheetDao timesheetDao, final TimesheetDO timesheet)
  {
    this.page = page;
    this.gridBuilder = gridBuilder;
    this.timesheetDao = timesheetDao;
    this.timesheet = timesheet;
  }

  public TimesheetRecentEntry getTimesheetRecentEntry()
  {
    TimesheetRecentEntry pref = (TimesheetRecentEntry) page.getUserPrefEntry(TimesheetEditPage.class.getName());
    if (pref == null) {
      pref = new TimesheetRecentEntry();
      page.putUserPrefEntry(TimesheetEditPage.class.getName(), pref, true);
    }
    return pref;
  }

  public AbstractFieldsetPanel< ? > addLocation(final TimesheetRecentService timesheetRecentService) {
    return addLocation(timesheetRecentService , null);
  }

  @SuppressWarnings("serial")
  public AbstractFieldsetPanel< ? > addLocation(final TimesheetRecentService timesheetRecentService, final TimesheetEditFilter filter)
  {
    final FieldProperties<String> props = getLocationProperties();
    final AbstractFieldsetPanel< ? > fs = gridBuilder.newFieldset(props);
    final PFAutoCompleteMaxLengthTextField locationTextField = new PFAutoCompleteMaxLengthTextField(InputPanel.WICKET_ID,
        new PropertyModel<String>(timesheet, "location")) {
      @Override
      protected List<String> getChoices(final String input)
      {
        return trimResults(timesheetDao.getLocationAutocompletion(input));
      }

      private List<String> trimResults(final List<String> result)
      {
        if(result != null && result.size() > 0 && filter != null && filter.getIgnoredLocations() != null && filter.getIgnoredLocations().size() > 0) {
          result.removeAll(filter.getIgnoredLocations());
        }
        return result;
      }

      @Override
      protected List<String> getFavorites()
      {
        return trimResults(timesheetRecentService.getRecentLocations());
      }
    };
    locationTextField.withMatchContains(true).withMinChars(2).withFocus(true);
    fs.setStoreObject(locationTextField);
    fs.add(locationTextField);
    if (fs instanceof FieldsetPanel) {
      ((FieldsetPanel) fs).addKeyboardHelpIcon(getString("tooltip.autocomplete.withDblClickFunction"));
      ((FieldsetPanel) fs).addHelpIcon(getString("timesheet.location.tooltip"));
    }
    return fs;
  }

  public FieldProperties<String> getLocationProperties()
  {
    return new FieldProperties<String>("timesheet.location", new PropertyModel<String>(timesheet, "location"));
  }

  public FieldProperties<String> getReferenceProperties()
  {
    return new FieldProperties<String>("timesheet.reference", new PropertyModel<String>(timesheet, "reference"));
  }

  private String getString(final String i18nKey)
  {
    return gridBuilder.getString(i18nKey);
  }
}
