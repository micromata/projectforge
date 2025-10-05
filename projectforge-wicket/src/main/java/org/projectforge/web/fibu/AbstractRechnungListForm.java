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

import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.fibu.AbstractRechnungsStatistik;
import org.projectforge.business.fibu.RechnungFilter;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;

public abstract class AbstractRechnungListForm<F extends RechnungFilter, P extends AbstractListPage<?, ?, ?>> extends
    AbstractListForm<F, P> {
  private static final long serialVersionUID = 2678813484329104564L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractRechnungListForm.class);

  protected int[] years;

  @Override
  protected void init() {
    super.init(false);

    // time period for Rechnungsdatum
    final F filter = getSearchFilter();
    addTimePeriodPanel("fibu.rechnung.datum",
        LambdaModel.of(filter::getFromDate, filter::setFromDate),
        LambdaModel.of(filter::getToDate, filter::setToDate)
    );

    onBeforeAddStatistics();

    addStatistics();
  }

  private void addStatistics() {
    gridBuilder.newGridPanel();

    // Show currency conversion warnings if any - use lazy evaluation
    final FieldsetPanel warningFs = gridBuilder.newFieldset("")
        .setLabelSide(false)
        .suppressLabelForWarning();

    warningFs.add(new org.apache.wicket.markup.html.basic.Label(warningFs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        try {
          if (getStats().getHasCurrencyConversionWarnings()) {
            StringBuilder sb = new StringBuilder();
            sb.append("<span style='color: red; font-weight: bold;'>");
            sb.append(getString("fibu.rechnung.currencyConversion.warnings.header"));
            sb.append("</span><br/>");
            for (String warning : getStats().getCurrencyConversionWarningsList()) {
              sb.append("<span style='color: red;'>• ").append(warning).append("</span><br/>");
            }
            sb.append("<br/>");
            return sb.toString();
          }
        } catch (Exception e) {
          // List not yet available during init, will be shown on next render
        }
        return "";
      }
    }).setEscapeModelStrings(false));

    final FieldsetPanel fs = gridBuilder.newFieldset(getString("statistics")).suppressLabelForWarning();
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return getString("fibu.common.brutto") + ": " + CurrencyFormatter.format(getStats().getBrutto()) + WebConstants.HTML_TEXT_DIVIDER;
      }
    }));
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return getString("fibu.common.netto") + ": " + CurrencyFormatter.format(getStats().getNetto()) + WebConstants.HTML_TEXT_DIVIDER;
      }
    }));
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return getString("fibu.rechnung.offen") + ": " + CurrencyFormatter.format(getStats().getOffen()) + WebConstants.HTML_TEXT_DIVIDER;
      }
    }, TextStyle.BLUE));
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return getString("fibu.rechnung.filter.ueberfaellig") + ": " + CurrencyFormatter.format(getStats().getUeberfaellig());
      }
    }, TextStyle.RED));
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return WebConstants.HTML_TEXT_DIVIDER
            + getString("fibu.rechnung.skonto")
            + ": "
            + CurrencyFormatter.format(getStats().getSkonto())
            + WebConstants.HTML_TEXT_DIVIDER;
      }
    }));
    // fieldset.add(new HtmlCodePanel(fieldset.newChildId(), "<br/>"));
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return getString("fibu.rechnung.zahlungsZiel")
            + ": Ø "
            + String.valueOf(getStats().getZahlungszielAverage())
            + WebConstants.HTML_TEXT_DIVIDER;
      }
    }));
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return getString("fibu.rechnung.zahlungsZiel.actual") + ": Ø " + String.valueOf(getStats().getTatsaechlichesZahlungzielAverage());
      }
    }));
  }

  protected void onBeforeAddStatistics() {
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   * org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @SuppressWarnings("serial")
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel) {
    final DivPanel radioGroupPanel = optionsFieldsetPanel.addNewRadioBoxButtonDiv();
    final RadioGroupPanel<String> radioGroup = new RadioGroupPanel<>(radioGroupPanel.newChildId(), "listtype",
        new PropertyModel<>(getSearchFilter(), "listType"));
    radioGroupPanel.add(radioGroup);
    radioGroup.add(new Model<String>("all"), getString("filter.all"));
    radioGroup.add(new Model<String>("unbezahlt"), getString("fibu.rechnung.filter.unbezahlt"));
    radioGroup.add(new Model<String>("ueberfaellig"), getString("fibu.rechnung.filter.ueberfaellig"));

    if (Configuration.getInstance().isCostConfigured() == true) {
      optionsCheckBoxesPanel.add(new CheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<>(getSearchFilter(),
          "showKostZuweisungStatus"), getString("fibu.rechnung.showKostZuweisungstatus")));
    }
  }

  protected abstract AbstractRechnungsStatistik<?> getStats();

  public AbstractRechnungListForm(final P parentPage) {
    super(parentPage);
  }

  @Override
  protected Logger getLogger() {
    return log;
  }
}
