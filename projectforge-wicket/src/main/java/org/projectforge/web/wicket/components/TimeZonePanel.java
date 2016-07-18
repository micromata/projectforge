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

package org.projectforge.web.wicket.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.converter.TimeZoneConverter;

/**
 * Panel contains a ajax autocompletion text field for choosing and displaying a time zone. The time zones of all users
 * will be shown as favorite list.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class TimeZonePanel extends Panel
{
  private PFAutoCompleteTextField<TimeZone> textField;

  private static final IConverter converter = new TimeZoneConverter();

  public TimeZonePanel(final String id, final IModel<TimeZone> model)
  {
    super(id);
    final String[] availableTimeZones = TimeZone.getAvailableIDs();
    Arrays.sort(availableTimeZones);
    final List<TimeZone> list = getAsTimeZoneObjects(availableTimeZones);
    final String[] favoriteIds = TimeZone.getAvailableIDs();
    final List<TimeZone> favoriteTimeZones = getAsTimeZoneObjects(favoriteIds);
    textField = new PFAutoCompleteTextField<TimeZone>("input", model)
    {
      @Override
      protected List<TimeZone> getChoices(final String input)
      {
        final List<TimeZone> result = new ArrayList<TimeZone>();
        for (final TimeZone timeZone : list) {
          final String str = converter.convertToString(timeZone, getLocale()).toLowerCase();
          if (str.contains(input.toLowerCase()) == true) {
            result.add(timeZone);
          }
        }
        return result;
      }

      /**
       * @see org.apache.wicket.Component#getConverter(java.lang.Class)
       */
      @Override
      public <C> IConverter<C> getConverter(final Class<C> type)
      {
        return converter;
      }

      @Override
      protected List<TimeZone> getFavorites()
      {
        return favoriteTimeZones;
      }

      @Override
      protected String formatValue(final TimeZone value)
      {
        return converter.convertToString(value, getLocale());
      }
    };
    textField.withMatchContains(true).withMinChars(2);
    // Cant't use getString(i18nKey) because we're in the constructor and this would result in a Wicket warning.
    final String tooltip = ThreadLocalUserContext.getLocalizedString("tooltip.autocomplete.timeZone");
    WicketUtils.addTooltip(textField, tooltip);
    add(textField);
    add(new TooltipImage("autocompleteDblClickHelpImage", WebConstants.IMAGE_HELP_KEYBOARD, tooltip));
    setRenderBodyOnly(true);
  }

  /**
   * @return the textField
   */
  public PFAutoCompleteTextField<TimeZone> getTextField()
  {
    return textField;
  }

  private List<TimeZone> getAsTimeZoneObjects(final String[] timeZoneIds)
  {
    final List<TimeZone> list = new ArrayList<TimeZone>();
    for (final String timeZoneId : timeZoneIds) {
      list.add(TimeZone.getTimeZone(timeZoneId));
    }
    return list;
  }
}
