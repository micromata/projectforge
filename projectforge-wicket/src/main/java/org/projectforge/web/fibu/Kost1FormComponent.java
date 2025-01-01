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

package org.projectforge.web.fibu;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.ValidationError;
import org.projectforge.business.fibu.OldKostFormatter;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost1Dao;
import org.projectforge.business.fibu.kost.KostFilter;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Kost1FormComponent extends PFAutoCompleteTextField<Kost1DO>
{
  private static final long serialVersionUID = -5900055958356749220L;

  class Kost1Converter implements IConverter<Kost1DO>
  {
    private static final long serialVersionUID = 5770334618044073827L;

    @Override
    public Kost1DO convertToObject(String value, final Locale locale)
    {
      value = StringUtils.trimToEmpty(value);
      return WicketSupport.get(Kost1Dao.class).getKost1(value);
    }

    @Override
    public String convertToString(final Kost1DO value, final Locale locale)
    {
      if (value == null) {
        return "";
      }
      return value.getFormattedNumber();
    }
  }

  public Kost1FormComponent(final String id, final IModel<Kost1DO> model, final boolean required)
  {
    this(id, model, required, false);
  }

  @SuppressWarnings("serial")
  public Kost1FormComponent(final String id, final IModel<Kost1DO> model, final boolean required, final boolean tooltipRightAlignment)
  {
    super(id, model, tooltipRightAlignment);
    if (required == true) {
      setRequired(true);
      add((INullAcceptingValidator<Kost1DO>) validatable -> {
        final Kost1DO value = validatable.getValue();
        if (value == null) {
          error(new ValidationError().addKey("fibu.kost.error.invalidKost"));
        }
      });
    }
    this.withLabelValue(true).withMatchContains(true).withMinChars(2).withWidth(200);
    enableTooltips();
  }

  @Override
  protected String getTooltip()
  {
    final Kost1DO kost1 = getModelObject();
    if (kost1 == null) {
      return "";
    }
    return OldKostFormatter.format(kost1) + " - " + OldKostFormatter.formatToolTip(kost1);
  }

  @Override
  protected List<Kost1DO> getChoices(final String input)
  {
    final KostFilter filter = new KostFilter();
    if (input.indexOf('*') >= 0) {
      filter.setSearchString(input);
    } else {
      filter.setSearchString(input + "*");
    }
    filter.setListType(KostFilter.FILTER_NOT_ENDED);
    final List<Kost1DO> list = WicketSupport.get(Kost1Dao.class).select(filter);
    Collections.sort(list, new Comparator<Kost1DO>()
    {
      @Override
      public int compare(final Kost1DO o1, final Kost1DO o2)
      {
        return Integer.compare(o1.getNummer(), o2.getNummer());
      }
    });
    return list;
  }

  @Override
  protected String formatValue(final Kost1DO value)
  {
    if (value == null) {
      return "";
    }
    return value.getFormattedNumber();
  }

  @Override
  protected String formatLabel(final Kost1DO value)
  {
    if (value == null) {
      return "";
    }
    return value.getFormattedNumber() + " " + value.getDescription();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public IConverter getConverter(final Class type)
  {
    return new Kost1Converter();
  }
}
