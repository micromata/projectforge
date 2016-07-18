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

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.converter.TimeZoneConverter;

/**
 * Text field contains a ajax autocompletion text field for choosing and displaying a time zone. The time zones of all
 * users will be shown as favorite list.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TimeZoneField extends PFAutoCompleteTextField<TimeZone>
{
  private static final long serialVersionUID = 6795639659992455936L;

  @SuppressWarnings("serial")
  private final IConverter converter = new TimeZoneConverter()
  {
    @Override
    protected void error()
    {
      TimeZoneField.this.error(getString("error.timezone.unsupported"));
    };
  };

  private final List<TimeZone> favoriteTimeZones;

  private final List<TimeZone> timeZones;

  public TimeZoneField(final String id, final IModel<TimeZone> model)
  {
    super(id, model);
    final String[] availableTimeZones = TimeZone.getAvailableIDs();
    Arrays.sort(availableTimeZones);
    timeZones = getAsTimeZoneObjects(availableTimeZones);
    final List<String> favoritesIds = new ArrayList<String>();
    for (final PFUserDO user : TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache().getAllUsers()) {
      final String timeZone = user.getTimeZone();
      if (timeZone == null) {
        continue;
      }
      if (favoritesIds.contains(timeZone) == false) {
        favoritesIds.add(timeZone);
      }
    }
    final String[] favoriteIds = favoritesIds.toArray(new String[favoritesIds.size()]);
    favoriteTimeZones = getAsTimeZoneObjects(favoriteIds);
    withMatchContains(true).withMinChars(2);
    // Cant't use getString(i18nKey) because we're in the constructor and this would result in a Wicket warning.
    final String tooltip = ThreadLocalUserContext.getLocalizedString("tooltip.autocomplete.timeZone");
    WicketUtils.addTooltip(this, tooltip);
  }

  @Override
  protected List<TimeZone> getChoices(final String input)
  {
    final List<TimeZone> result = new ArrayList<TimeZone>();
    for (final TimeZone timeZone : timeZones) {
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

  private List<TimeZone> getAsTimeZoneObjects(final String[] timeZoneIds)
  {
    final List<TimeZone> list = new ArrayList<TimeZone>();
    for (final String timeZoneId : timeZoneIds) {
      list.add(TimeZone.getTimeZone(timeZoneId));
    }
    return list;
  }
}
