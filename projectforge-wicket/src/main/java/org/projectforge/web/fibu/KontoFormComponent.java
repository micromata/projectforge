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
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KontoDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;

public class KontoFormComponent extends PFAutoCompleteTextField<KontoDO>
{
  private static final long serialVersionUID = -9086404806066376969L;

  @SpringBean
  private KontoDao kontoDao;

  class KontoConverter implements IConverter
  {
    private static final long serialVersionUID = -6179453515097650206L;

    public KontoDO convertToObject(final String value, final Locale locale)
    {
      if (StringUtils.isBlank(value) == true) {
        return null;
      }
      final Integer number;
      try {
        number = Integer.valueOf(value);
      } catch (final NumberFormatException ex) {
        return null;
      }
      return kontoDao.getKonto(number);
    }

    public String convertToString(final Object value, final Locale locale)
    {
      if (value == null) {
        return "";
      }
      return String.valueOf(((KontoDO) value).getNummer());
    }
  }

  @SuppressWarnings("serial")
  public KontoFormComponent(final String id, final IModel<KontoDO> model, final boolean required)
  {
    super(id, model);
    if (required == true) {
      setRequired(true);
      add(new AbstractValidator<KontoDO>() {
        @Override
        protected void onValidate(final IValidatable<KontoDO> validatable)
        {
          final KontoDO value = validatable.getValue();
          if (value == null) {
            error(validatable);
          }
        }

        @Override
        public boolean validateOnNullValue()
        {
          return true;
        }

        @Override
        protected String resourceKey()
        {
          return "fibu.konto.error.invalidKonto";
        }
      });
    }
    this.withLabelValue(true).withMatchContains(true).withMinChars(2).withWidth(500);
    enableTooltips();
  }

  @Override
  protected String getTooltip()
  {
    final KontoDO konto = getModelObject();
    if (konto == null) {
      return "";
    }
    return String.valueOf(konto.getNummer()) + " - " + konto.getBezeichnung();
  }

  @Override
  protected List<KontoDO> getChoices(final String input)
  {
    final BaseSearchFilter filter = new BaseSearchFilter();
    filter.setSearchString(input);
    final List<KontoDO> list = kontoDao.getList(filter);
    Collections.sort(list, new Comparator<KontoDO>() {
      public int compare(final KontoDO o1, final KontoDO o2)
      {
        return (o1.getNummer().compareTo(o2.getNummer()));
      }
    });
    return list;
  }

  @Override
  protected String formatValue(final KontoDO value)
  {
    if (value == null) {
      return "";
    }
    return String.valueOf(value.getNummer());
  }

  @Override
  protected String formatLabel(final KontoDO value)
  {
    if (value == null) {
      return "";
    }
    return String.valueOf(value.getNummer()) + " - " + value.getBezeichnung();
  }

  @Override
  public IConverter getConverter(final Class type)
  {
    return new KontoConverter();
  }
}
