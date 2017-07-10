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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.fibu.AbstractRechnungsPositionDO;
import org.projectforge.business.fibu.EingangsrechnungsPositionDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.RechnungDO;
import org.projectforge.business.fibu.RechnungsPositionDO;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.business.fibu.kost.KostZuweisungenCopyHelper;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.framework.utils.Constants;
import org.projectforge.framework.utils.NumberFormatter;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.wicket.CsrfTokenHandler;
import org.projectforge.web.wicket.WicketAjaxUtils;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.converter.CurrencyConverter;
import org.projectforge.web.wicket.flowlayout.ButtonPanel;
import org.projectforge.web.wicket.flowlayout.IconButtonPanel;
import org.projectforge.web.wicket.flowlayout.IconType;
import org.projectforge.web.wicket.flowlayout.MyAjaxComponentHolder;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class RechnungCostEditTablePanel extends Panel
{
  private static final long serialVersionUID = -5732520730823126042L;

  private final RepeatingView rows;

  private final Form<AbstractRechnungsPositionDO> form;

  private final FeedbackPanel feedbackPanel;

  @SpringBean
  private Kost2Dao kost2Dao;

  private AbstractRechnungsPositionDO position;

  MyAjaxComponentHolder ajaxComponents = new MyAjaxComponentHolder();

  /**
   * Cross site request forgery token.
   */
  private final CsrfTokenHandler csrfTokenHandler;

  /**
   * @param id
   */
  @SuppressWarnings("serial")
  public RechnungCostEditTablePanel(final String id)
  {
    super(id);
    feedbackPanel = new FeedbackPanel("feedback");
    ajaxComponents.register(feedbackPanel);
    add(feedbackPanel);
    this.form = new Form<AbstractRechnungsPositionDO>("form")
    {
      @Override
      protected void onSubmit()
      {
        super.onSubmit();
        csrfTokenHandler.onSubmit();
      }
    };
    add(form);
    csrfTokenHandler = new CsrfTokenHandler(form);
    rows = new RepeatingView("rows");
    form.add(rows);
  }

  /**
   * @return the position
   */
  public AbstractRechnungsPositionDO getPosition()
  {
    return position;
  }

  @SuppressWarnings("serial")
  public RechnungCostEditTablePanel add(final AbstractRechnungsPositionDO origPosition)
  {
    if (origPosition instanceof RechnungsPositionDO) {
      position = new RechnungsPositionDO();
    } else {
      position = new EingangsrechnungsPositionDO();
    }
    position.copyValuesFrom(origPosition, "kostZuweisungen");
    KostZuweisungenCopyHelper.copy(origPosition.getKostZuweisungen(), position);
    List<KostZuweisungDO> kostzuweisungen = position.getKostZuweisungen();
    if (CollectionUtils.isEmpty(kostzuweisungen) == true) {
      addZuweisung(position);
      kostzuweisungen = position.getKostZuweisungen();
    }
    for (final KostZuweisungDO zuweisung : kostzuweisungen) {
      final WebMarkupContainer row = createRow(rows.newChildId(), position, zuweisung);
      rows.add(row);
    }
    final Label restLabel = new Label("restValue", new Model<String>()
    {
      /**
       * @see org.apache.wicket.model.Model#getObject()
       */
      @Override
      public String getObject()
      {
        return CurrencyFormatter.format(position.getKostZuweisungNetFehlbetrag());
      }
    });
    form.add(restLabel);
    ajaxComponents.register(restLabel);
    final AjaxButton addRowButton = new AjaxButton(ButtonPanel.BUTTON_ID, form)
    {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form)
      {
        final KostZuweisungDO zuweisung = addZuweisung(position);
        final WebMarkupContainer newRow = createRow(rows.newChildId(), position, zuweisung);
        newRow.setOutputMarkupId(true);
        final String prependJavascript = WicketAjaxUtils.appendChild("costAssignmentBody", "tr", newRow.getMarkupId());
        rows.add(newRow);
        target.add(newRow);
        ajaxComponents.addTargetComponents(target);
        target.prependJavaScript(prependJavascript);
      }

      @Override
      protected void onError(final AjaxRequestTarget target, final Form<?> form)
      {
        target.add(feedbackPanel);
      }
    };
    // addRowButton.setDefaultFormProcessing(false);
    final SingleButtonPanel addPositionButtonPanel = new SingleButtonPanel("addRowButton", addRowButton, getString("add"));
    form.add(addPositionButtonPanel);

    final AjaxButton recalculateButton = new AjaxButton(ButtonPanel.BUTTON_ID, form)
    {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form)
      {
        ajaxComponents.addTargetComponents(target);
      }

      @Override
      protected void onError(final AjaxRequestTarget target, final Form<?> form)
      {
        target.add(feedbackPanel);
      }
    };
    // recalculateButton.setDefaultFormProcessing(false);
    final SingleButtonPanel recalculateButtonPanel = new SingleButtonPanel("recalculateButton", recalculateButton, getString("recalculate"));
    form.add(recalculateButtonPanel);

    return this;
  }

  @SuppressWarnings("serial")
  private WebMarkupContainer createRow(final String id, final AbstractRechnungsPositionDO position, final KostZuweisungDO zuweisung)
  {
    final WebMarkupContainer row = new WebMarkupContainer(id);
    row.setOutputMarkupId(true);
    final Kost1FormComponent kost1 = new Kost1FormComponent("kost1", new PropertyModel<Kost1DO>(zuweisung, "kost1"), true, true);
    kost1.setLabel(new Model<String>(getString("fibu.kost1")));
    row.add(kost1);
    ajaxComponents.register(kost1);
    final Kost2FormComponent kost2 = new Kost2FormComponent("kost2", new PropertyModel<Kost2DO>(zuweisung, "kost2"), true, true);
    kost2.setLabel(new Model<String>(getString("fibu.kost2")));
    row.add(kost2);
    ajaxComponents.register(kost2);

    final MinMaxNumberField<BigDecimal> netto = new MinMaxNumberField<BigDecimal>("netto",
        new PropertyModel<BigDecimal>(zuweisung, "netto"), Constants.TEN_BILLION_NEGATIVE, Constants.TEN_BILLION)
    {
      @SuppressWarnings({ "rawtypes", "unchecked" })
      @Override
      public IConverter getConverter(final Class type)
      {
        return new CurrencyConverter(position.getNetSum());
      }
    };
    netto.setLabel(new Model<String>(getString("fibu.common.netto")));
    WicketUtils.addTooltip(netto, getString("currencyConverter.percentage.help"));
    row.add(netto);
    ajaxComponents.register(netto); // Should be updated if e. g. percentage value is given.
    final Label pLabel = new Label("percentage", new Model<String>()
    {
      /**
       * @see org.apache.wicket.model.Model#getObject()
       */
      @Override
      public String getObject()
      {
        final BigDecimal percentage;
        if (NumberHelper.isZeroOrNull(position.getNetSum()) == true || NumberHelper.isZeroOrNull(zuweisung.getNetto()) == true) {
          percentage = BigDecimal.ZERO;
        } else {
          percentage = zuweisung.getNetto().divide(position.getNetSum(), RoundingMode.HALF_UP);
        }
        final boolean percentageVisible = NumberHelper.isNotZero(percentage);
        if (percentageVisible == true) {
          return NumberFormatter.formatPercent(percentage);
        } else {
          return " ";
        }
      }
    });
    ajaxComponents.register(pLabel);
    row.add(pLabel);

    if (position.isKostZuweisungDeletable(zuweisung) == true) {
      final AjaxButton deleteRowButton = new AjaxButton(ButtonPanel.BUTTON_ID, form)
      {
        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form)
        {
          position.deleteKostZuweisung(zuweisung.getIndex());
          final StringBuffer prependJavascriptBuf = new StringBuffer();
          prependJavascriptBuf.append(WicketAjaxUtils.removeChild("costAssignmentBody", row.getMarkupId()));
          ajaxComponents.remove(row);
          rows.remove(row);
          target.prependJavaScript(prependJavascriptBuf.toString());
        }

        @Override
        protected void onError(final AjaxRequestTarget target, final Form<?> form)
        {
          target.add(feedbackPanel.setVisible(true));
        }
      };
      deleteRowButton.setDefaultFormProcessing(false);
      row.add(new IconButtonPanel("deleteEntry", deleteRowButton, IconType.TRASH, null).setLight());
    } else {
      // Don't show a delete button.
      row.add(new Label("deleteEntry", "&nbsp;").setEscapeModelStrings(false).setRenderBodyOnly(true));
    }
    return row;
  }

  private KostZuweisungDO addZuweisung(final AbstractRechnungsPositionDO position)
  {
    final KostZuweisungDO kostZuweisung = new KostZuweisungDO();
    position.addKostZuweisung(kostZuweisung);
    if (kostZuweisung.getIndex() > 0) {
      final KostZuweisungDO predecessor = position.getKostZuweisung(kostZuweisung.getIndex() - 1);
      if (predecessor != null) {
        kostZuweisung.setKost1(predecessor.getKost1()); // Preset kost1 from the predecessor position.
        kostZuweisung.setKost2(predecessor.getKost2()); // Preset kost2 from the predecessor position.
      }
    }
    if (RechnungsPositionDO.class.isAssignableFrom(position.getClass()) == true && kostZuweisung.getKost2() == null) {
      // Preset kost2 with first kost2 found for the projekt.
      final RechnungsPositionDO rechnungsPosition = (RechnungsPositionDO) position;
      final RechnungDO rechnung = rechnungsPosition.getRechnung();
      if (rechnung != null) {
        final ProjektDO project = rechnung.getProjekt();
        if (project != null) {
          final List<Kost2DO> kost2List = kost2Dao.getActiveKost2(project);
          if (CollectionUtils.isNotEmpty(kost2List) == true) {
            kostZuweisung.setKost2(kost2List.get(0));
          }
        }
      }
    }
    kostZuweisung.setNetto(position.getKostZuweisungNetFehlbetrag().negate());
    return kostZuweisung;
  }

  /**
   * @return the form
   */
  public Form<AbstractRechnungsPositionDO> getForm()
  {
    return form;
  }
}
