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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.ValidationError;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragDao;
import org.projectforge.business.fibu.AuftragFilter;
import org.projectforge.business.fibu.AuftragsPositionDO;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;

/**
 * For displaying and selecting an order position.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class AuftragsPositionFormComponent extends PFAutoCompleteTextField<AuftragsPositionDO>
{
  private static final long serialVersionUID = -4741993589740783229L;

  @SpringBean
  private AuftragDao auftragDao;

  @SuppressWarnings("rawtypes")
  class AuftragsPositionConverter implements IConverter
  {
    private static final long serialVersionUID = -8117783418789940569L;

    @Override
    public AuftragsPositionDO convertToObject(String value, final Locale locale)
    {
      value = StringUtils.trimToEmpty(value);
      if (StringUtils.isEmpty(value) == true) {
        return null;
      }
      final AuftragsPositionDO auftragsPosition = auftragDao.getAuftragsPosition(value);
      if (auftragsPosition == null) {
        throw new ConversionException("Parse error").setResourceKey("fibu.auftrag.position.error.notFound");
      }
      return auftragsPosition;
    }

    @Override
    public String convertToString(final Object value, final Locale locale)
    {
      if (value == null) {
        return "";
      }
      return ((AuftragsPositionDO) value).getFormattedNumber();
    }
  }

  @SuppressWarnings("serial")
  public AuftragsPositionFormComponent(final String id, final IModel<AuftragsPositionDO> model, final boolean required)
  {
    super(id, model);
    if (required == true) {
      setRequired(true);
      add((INullAcceptingValidator<AuftragsPositionDO>) validatable -> {
        final AuftragsPositionDO value = validatable.getValue();
        if (value == null) {
          error(new ValidationError().addKey("fibu.auftrag.error.invalidPosition"));
        }
      });
    }
    this.withLabelValue(true).withMatchContains(true).withMinChars(2).withWidth(800);
    add(new AttributeModifier("title", new Model<String>()
    {
      @Override
      public String getObject()
      {
        final AuftragsPositionDO pos = getModelObject();
        if (pos == null) {
          return "";
        }
        return getTooltip(pos);
      }
    }));
  }

  @Override
  protected List<AuftragsPositionDO> getChoices(final String input)
  {
    final AuftragFilter filter = new AuftragFilter();
    filter.setSearchString(input);
    final List<AuftragDO> list = auftragDao.getList(filter);
    Collections.sort(list, new Comparator<AuftragDO>()
    {
      @Override
      public int compare(final AuftragDO o1, final AuftragDO o2)
      {
        return (o1.getNummer().compareTo(o2.getNummer()));
      }
    });
    final List<AuftragsPositionDO> result = new ArrayList<AuftragsPositionDO>();
    for (final AuftragDO auftrag : list) {
      if (auftrag.getPositionenExcludingDeleted() != null) {
        for (final AuftragsPositionDO pos : auftrag.getPositionenExcludingDeleted()) {
          result.add(pos);
        }
      }
    }
    return result;
  }

  @Override
  protected String formatValue(final AuftragsPositionDO value)
  {
    if (value == null) {
      return "";
    }
    return value.getFormattedNumber();
  }

  @Override
  protected String formatLabel(final AuftragsPositionDO value)
  {
    if (value == null) {
      return "";
    }
    return getTooltip(value);
  }

  private static String getTooltip(final AuftragsPositionDO pos)
  {
    final AuftragDO auftrag = pos.getAuftrag();
    final StringBuffer buf = new StringBuffer();
    buf.append(auftrag.getNummer()).append(".").append(pos.getNumber()).append(": ");
    if (auftrag.getKunde() != null) {
      buf.append(auftrag.getKundeAsString());
      if (auftrag.getProjekt() != null) {
        buf.append(" - ").append(auftrag.getProjekt().getName());
      }
      buf.append(": ");
    } else if (auftrag.getProjekt() != null) {
      buf.append(auftrag.getProjekt().getName());
      buf.append(": ");
    }
    buf.append(auftrag.getTitel()).append(" / ").append(pos.getNumber());
    if (StringUtils.isNotBlank(pos.getTitel()) == true) {
      buf.append(": ").append(pos.getTitel());
    }
    return buf.toString();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public IConverter getConverter(final Class type)
  {
    return new AuftragsPositionConverter();
  }
}
