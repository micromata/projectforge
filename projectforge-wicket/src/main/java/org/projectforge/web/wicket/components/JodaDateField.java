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

import org.apache.wicket.markup.html.form.AbstractTextComponent.ITextFormatProvider;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.joda.time.DateMidnight;
import org.projectforge.web.wicket.converter.JodaDateConverter;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class JodaDateField extends TextField<DateMidnight> implements ITextFormatProvider
{
  private static final long serialVersionUID = 6795639659992455936L;

  JodaDateConverter converter;

  public JodaDateField(final String id, final IModel<DateMidnight> model)
  {
    super(id, model);
    setType(DateMidnight.class);
  }

  /**
   * @see org.apache.wicket.Component#getConverter(java.lang.Class)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <C> IConverter<C> getConverter(final Class<C> type)
  {
    if (converter == null) {
      converter = new JodaDateConverter();
    }
    return (IConverter<C>)converter;
  }

  /**
   * @see org.apache.wicket.markup.html.form.AbstractTextComponent.ITextFormatProvider#getTextFormat()
   */
  @Override
  public String getTextFormat()
  {
    return converter.getPattern();
  }
}

