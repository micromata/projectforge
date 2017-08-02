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

package org.projectforge.web.fibu;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.ValidationError;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.fibu.kost.KostFilter;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;

public class Kost2FormComponent extends PFAutoCompleteTextField<Kost2DO>
{
  private static final long serialVersionUID = 8411456751099783863L;

  @SpringBean
  private Kost2Dao kost2Dao;

  class Kost2Converter implements IConverter
  {
    private static final long serialVersionUID = 5770334618044073827L;

    @Override
    public Kost2DO convertToObject(String value, final Locale locale)
    {
      value = StringUtils.trimToEmpty(value);
      return kost2Dao.getKost2(value);
    }

    @Override
    public String convertToString(final Object value, final Locale locale)
    {
      if (value == null) {
        return "";
      }
      return ((Kost2DO) value).getFormattedNumber();
    }
  }

  public Kost2FormComponent(final String id, final IModel<Kost2DO> model, final boolean required)
  {
    this(id, model, required, false);
  }

  @SuppressWarnings("serial")
  public Kost2FormComponent(final String id, final IModel<Kost2DO> model, final boolean required, final boolean tooltipRightAlignment)
  {
    super(id, model, tooltipRightAlignment);
    if (required == true) {
      setRequired(true);
      add((INullAcceptingValidator<Kost2DO>) validatable -> {
        final Kost2DO value = validatable.getValue();
        if (value == null) {
          error(new ValidationError().addKey("fibu.kost.error.invalidKost"));
        }
      });
    }
    this.withLabelValue(true).withMatchContains(true).withMinChars(2).withWidth(500);
    enableTooltips();
  }

  @Override
  protected String getTooltip()
  {
    final Kost2DO kost2 = getModelObject();
    if (kost2 == null) {
      return "";
    }
    return KostFormatter.format(kost2) + " - " + KostFormatter.formatToolTip(kost2);
  }

  @Override
  protected List<Kost2DO> getChoices(final String input)
  {
    final KostFilter filter = new KostFilter();
    filter.setSearchString(input);
    filter.setListType(KostFilter.FILTER_NOT_ENDED);
    final List<Kost2DO> list = kost2Dao.getList(filter);
    Collections.sort(list, new Comparator<Kost2DO>()
    {
      @Override
      public int compare(final Kost2DO o1, final Kost2DO o2)
      {
        return (o1.getNummer().compareTo(o2.getNummer()));
      }
    });
    return list;
  }

  @Override
  protected String formatValue(final Kost2DO value)
  {
    if (value == null) {
      return "";
    }
    return value.getFormattedNumber();
  }

  @Override
  protected String formatLabel(final Kost2DO value)
  {
    if (value == null) {
      return "";
    }
    return KostFormatter.format(value) + " - " + KostFormatter.formatToolTip(value);
  }

  @Override
  public IConverter getConverter(final Class type)
  {
    return new Kost2Converter();
  }
}
