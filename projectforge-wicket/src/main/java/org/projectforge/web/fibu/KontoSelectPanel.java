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
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KontoDao;
import org.projectforge.business.fibu.KontoStatus;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.utils.IntRanges;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.framework.utils.Ranges;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;

/**
 * This panel is a autocompletion text field for selecting an account (DATEV-Konto).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class KontoSelectPanel extends AbstractSelectPanel<KontoDO> implements ComponentWrapperPanel
{
  private static final long serialVersionUID = 5452693296383142460L;

  private PFAutoCompleteTextField<KontoDO> kontoTextField;

  @SpringBean
  private KontoDao kontoDao;

  private IntRanges kontoNumberRanges;

  /**
   * @param id
   * @param model
   * @param kontoText      If no Konto is given then a free text field representing a Konto can be used.
   * @param caller
   * @param selectProperty
   */
  public KontoSelectPanel(final String id, final IModel<KontoDO> model, final ISelectCallerPage caller, final String selectProperty)
  {
    super(id, model, caller, selectProperty);
    kontoTextField = new PFAutoCompleteTextField<KontoDO>("kontoField", getModel())
    {
      @Override
      protected List<KontoDO> getChoices(final String input)
      {
        final BaseSearchFilter filter = new BaseSearchFilter();
        filter.setSearchFields("nummer", "bezeichnung", "description");
        filter.setSearchString(input);
        final List<KontoDO> list = kontoDao.getList(filter);
        if (kontoNumberRanges != null && list != null) {
          final List<KontoDO> result = new ArrayList<KontoDO>();
          for (final KontoDO konto : list) {
            if (konto.getStatus() == KontoStatus.NONACTIVE) {
              continue;
            }
            if (kontoNumberRanges.doesMatch(konto.getNummer()) == true) {
              result.add(konto);
            }
          }
          return result;
        }
        return list;
      }

      @Override
      protected String formatLabel(final KontoDO konto)
      {
        if (konto == null) {
          return "";
        }
        return konto.formatKonto();
      }

      @Override
      protected String formatValue(final KontoDO konto)
      {
        if (konto == null) {
          return "";
        }
        return konto.formatKonto();
      }

      @Override
      public void convertInput()
      {
        final KontoDO konto = (KontoDO) getConverter(getType()).convertToObject(getInput(), getLocale());
        setConvertedInput(konto);
      }

      @Override
      public IConverter getConverter(final Class type)
      {
        return new IConverter()
        {
          @Override
          public Object convertToObject(final String value, final Locale locale)
          {
            if (StringUtils.isEmpty(value) == true) {
              getModel().setObject(null);
              return null;
            }
            final int ind = value.indexOf(" ");
            final String kontonummerString = ind >= 0 ? value.substring(0, ind) : value;
            final Integer kontonummer = NumberHelper.parseInteger(kontonummerString);
            final KontoDO konto;
            if (kontonummer != null) {
              konto = kontoDao.getKonto(kontonummer);
            } else {
              konto = null;
            }
            if (konto == null) {
              error(getString("fibu.konto.error.invalidKonto"));
            }
            getModel().setObject(konto);
            return konto;
          }

          @Override
          public String convertToString(final Object value, final Locale locale)
          {
            if (value == null) {
              return "";
            }
            final KontoDO konto = (KontoDO) value;
            return konto.formatKonto();
          }
        };
      }
    };
    kontoTextField.enableTooltips().withLabelValue(true).withMatchContains(true).withMinChars(2).withAutoSubmit(false).withWidth(400);
    kontoTextField.setLabel(new Model<String>()
    {
      @Override
      public String getObject()
      {
        return getString("fibu.konto");
      }
    });
  }

  /**
   * If set then only accounting numbers are given in auto-completion list, whose number matches the given range(s).
   *
   * @param kontoNumberRanges the kontoNumberRanges to set
   * @return this for chaining.
   * @see Ranges#setRanges(String)
   */
  public KontoSelectPanel setKontoNumberRanges(final IntRanges kontoNumberRanges)
  {
    this.kontoNumberRanges = kontoNumberRanges;
    return this;
  }

  /**
   * Must be called before component is added to a field set. This is different to most other Panels in ProjectForge.
   *
   * @see org.projectforge.web.wicket.AbstractSelectPanel#init()
   */
  @Override
  @SuppressWarnings("serial")
  public KontoSelectPanel init()
  {
    super.init();
    add(kontoTextField);
    return this;
  }

  @Override
  public void convertInput()
  {
    setConvertedInput(getModelObject());
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    kontoTextField.setOutputMarkupId(true);
    return kontoTextField.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return null;
  }
}
