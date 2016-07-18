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

package org.projectforge.web.wicket.autocompletion;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.common.i18n.I18nEnum;
import org.projectforge.web.wicket.converter.I18nEnumConverter;

/**
 * Autocompletion field that represents localized enum values.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class I18nEnumAutoCompleteTextField<T extends I18nEnum> extends PFAutoCompleteTextField<T>
{
  private static final long serialVersionUID = 8174480171305510104L;

  private final T[] supportedValues;

  /**
   * @param id
   * @param label Label used for validation, calls setLabel(label).
   * @param model
   * @param type
   * @param settings
   */
  public I18nEnumAutoCompleteTextField(final String id, final IModel<T> model, final T[] supportedValues)
  {
    super(id,model);
    this.supportedValues = supportedValues;
  }

  public I18nEnumAutoCompleteTextField(final String id,final IModel<T> model, final IAutoCompleteRenderer<String> renderer,
      final PFAutoCompleteSettings settings, final T[] supportedValues)
  {
    super(id,  model, renderer, settings);
    this.supportedValues = supportedValues;
  }

  @Override
  protected List<String> getRecentUserInputs()
  {
    final List<String> list = new ArrayList<String>();
    for (final T i18nEnum : supportedValues) {
      list.add(getString(i18nEnum.getI18nKey()));
    }
    return list;
  }


  @Override
  protected List<T> getChoices(final String input)
  {
    final List<T> list = new ArrayList<T>();
    for (final T i18nEnum : supportedValues) {
      list.add(i18nEnum);
    }
    return list;
  }

  @Override
  protected String formatLabel(final T value)
  {
    if (value == null) {
      return "";
    }
    return getString(((I18nEnum)value).getI18nKey());
  }

  @Override
  protected String formatValue(final T value)
  {
    if (value == null) {
      return "";
    }
    return getString(((I18nEnum)value).getI18nKey());
  }

  /**
   * @see org.apache.wicket.Component#getConverter(java.lang.Class)
   */
  @Override
  public <C> IConverter<C> getConverter(final Class<C> type)
  {
    return new I18nEnumConverter(this, supportedValues);
  }
}
