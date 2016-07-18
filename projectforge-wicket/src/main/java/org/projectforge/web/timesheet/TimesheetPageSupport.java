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

package org.projectforge.web.timesheet;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetPrefData;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel;
import org.projectforge.web.wicket.flowlayout.AbstractGridBuilder;
import org.projectforge.web.wicket.flowlayout.FieldProperties;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;

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
   * @param form
   * @param gridBuilder
   * @param addressDao
   * @param personalAddressDao
   * @param address
   */
  public TimesheetPageSupport(final AbstractSecuredBasePage page, final AbstractGridBuilder< ? > gridBuilder,
      final TimesheetDao timesheetDao, final TimesheetDO timesheet)
  {
    this.page = page;
    this.gridBuilder = gridBuilder;
    this.timesheetDao = timesheetDao;
    this.timesheet = timesheet;
  }

  public TimesheetPrefData getTimesheetPrefData()
  {
    TimesheetPrefData pref = (TimesheetPrefData) page.getUserPrefEntry(TimesheetEditPage.class.getName());
    if (pref == null) {
      pref = new TimesheetPrefData();
      page.putUserPrefEntry(TimesheetEditPage.class.getName(), pref, true);
    }
    return pref;
  }

  public AbstractFieldsetPanel< ? > addLocation() {
    return addLocation(null);
  }

  @SuppressWarnings("serial")
  public AbstractFieldsetPanel< ? > addLocation(final TimesheetEditFilter filter)
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
        return trimResults(getTimesheetPrefData().getRecentLocations());
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

  private String getString(final String i18nKey)
  {
    return gridBuilder.getString(i18nKey);
  }
}
