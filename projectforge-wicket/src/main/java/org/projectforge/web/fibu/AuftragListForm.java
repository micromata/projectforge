/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.projectforge.business.fibu.*;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.common.I18nEnumChoiceProvider;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;
import org.wicketstuff.select2.Select2MultiChoice;

import java.math.BigDecimal;

public class AuftragListForm extends AbstractListForm<AuftragFilter, AuftragListPage> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuftragListForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  private AuftragsStatistik auftragsStatistik;

  public AuftragListForm(final AuftragListPage parentPage) {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init() {
    super.init(false);

    final AuftragFilter filter = getSearchFilter();

    // time period for erfassungsdatum
    // TODO: What to do here?
    addTimePeriodPanel("fibu.auftrag.erfassung.datum",
        LambdaModel.of(filter::getStartDate, filter::setStartDate),
        LambdaModel.of(filter::getEndDate, filter::setEndDate)
    );

    // time period for period of performance
    // TODO: What to do here?
    addTimePeriodPanel("fibu.periodOfPerformance",
        LambdaModel.of(filter::getPeriodOfPerformanceStartDate, filter::setPeriodOfPerformanceStartDate),
        LambdaModel.of(filter::getPeriodOfPerformanceEndDate, filter::setPeriodOfPerformanceEndDate)
    );

    // Statistics
    addStatistics();
  }

  private void addStatistics() {
    gridBuilder.newGridPanel();
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("statistics")).suppressLabelForWarning();
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return getStatisticsValue("fibu.common.netto", getAuftragsStatistik().getNettoSum(),
            getAuftragsStatistik().getCounter());
      }
    }));
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return WebConstants.HTML_TEXT_DIVIDER
            + getStatisticsValue("akquise", getAuftragsStatistik().getAkquiseSum(),
            getAuftragsStatistik().getCounterAkquise());
      }

    }) {
      @Override
      public boolean isVisible() {
        return (getAuftragsStatistik().getCounterAkquise() > 0);
      }
    });
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return WebConstants.HTML_TEXT_DIVIDER
            + getStatisticsValue("fibu.auftrag.status.beauftragt", getAuftragsStatistik().getBeauftragtSum(),
            getAuftragsStatistik()
                .getCounterBeauftragt());
      }
    }, TextStyle.BLUE) {
      @Override
      public boolean isVisible() {
        return (getAuftragsStatistik().getCounterBeauftragt() > 0);
      }
    });
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return WebConstants.HTML_TEXT_DIVIDER
            + getStatisticsValue("fibu.fakturiert", getAuftragsStatistik().getInvoicedSum(),
            getAuftragsStatistik().getCounterInvoiced());
      }
    }) {
      @Override
      public boolean isVisible() {
        return (getAuftragsStatistik().getCounterInvoiced() > 0);
      }
    });
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return WebConstants.HTML_TEXT_DIVIDER
            + getStatisticsValue("fibu.notYetInvoiced",
            getAuftragsStatistik().getNotYetInvoicedSum(),
            getAuftragsStatistik().getCounterNotYetInvoiced());
      }
    }) {
      @Override
      public boolean isVisible() {
        return (getAuftragsStatistik().getCounterNotYetInvoiced() > 0);
      }
    });
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject() {
        return WebConstants.HTML_TEXT_DIVIDER
            + getStatisticsValue("fibu.toBeInvoiced",
            getAuftragsStatistik().getToBeInvoiced(),
            getAuftragsStatistik().getCounterToBeInvoiced());
      }
    }, TextStyle.RED) {
      @Override
      public boolean isVisible() {
        return (getAuftragsStatistik().getCounterToBeInvoiced() > 0);
      }
    });
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   * org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @SuppressWarnings("serial")
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel) {
    optionsFieldsetPanel.add(createAuftragsStatusMultiChoice());
    optionsFieldsetPanel.add(createAuftragsPositionsArtMultiChoice());
    optionsFieldsetPanel.add(createAuftragFakturiertDropDown());
    optionsFieldsetPanel.add(createAuftragsPositionsPaymentTypeDropDown());
    optionsFieldsetPanel.add(createUserSelect(optionsFieldsetPanel.newChildId()));
  }

  private Select2MultiChoice<AuftragsStatus> createAuftragsStatusMultiChoice() {
    return new Select2MultiChoice<>(
        Select2MultiChoicePanel.WICKET_ID,
        LambdaModel.of(getSearchFilter()::getAuftragsStatuses),
        new I18nEnumChoiceProvider<>(AuftragsStatus.class)
    );
  }

  private Select2MultiChoice<AuftragsPositionsArt> createAuftragsPositionsArtMultiChoice() {
    return new Select2MultiChoice<>(
        Select2MultiChoicePanel.WICKET_ID,
        LambdaModel.of(getSearchFilter()::getAuftragsPositionsArten),
        new I18nEnumChoiceProvider<>(AuftragsPositionsArt.class)
    );
  }

  private DropDownChoice<AuftragFakturiertFilterStatus> createAuftragFakturiertDropDown() {
    final LabelValueChoiceRenderer<AuftragFakturiertFilterStatus> fakturiertChoiceRenderer = new LabelValueChoiceRenderer<>(this,
        AuftragFakturiertFilterStatus.values());

    final DropDownChoice<AuftragFakturiertFilterStatus> fakturiertChoice = new DropDownChoice<>(
        DropDownChoicePanel.WICKET_ID,
        LambdaModel.of(getSearchFilter()::getAuftragFakturiertFilterStatus, getSearchFilter()::setAuftragFakturiertFilterStatus),
        fakturiertChoiceRenderer.getValues(),
        fakturiertChoiceRenderer
    );
    fakturiertChoice.setNullValid(false);

    return fakturiertChoice;
  }

  private DropDownChoice<Integer> createAuftragsPositionsPaymentTypeDropDown() {
    final LabelValueChoiceRenderer<Integer> auftragsPositionsPaymentTypeChoiceRenderer = new LabelValueChoiceRenderer<>();
    auftragsPositionsPaymentTypeChoiceRenderer.addValue(-1, getString("filter.all"));
    for (final AuftragsPositionsPaymentType paymentType : AuftragsPositionsPaymentType.values()) {
      auftragsPositionsPaymentTypeChoiceRenderer.addValue(paymentType.ordinal(), getString(paymentType.getI18nKey()));
    }

    final DropDownChoice<Integer> auftragsPositionsPaymentTypeChoice = new DropDownChoice<>(
        DropDownChoicePanel.WICKET_ID,
        LambdaModel.of(this::getAuftragsPositionsPaymentType, this::setAuftragsPositionsPaymentType),
        auftragsPositionsPaymentTypeChoiceRenderer.getValues(),
        auftragsPositionsPaymentTypeChoiceRenderer
    );
    auftragsPositionsPaymentTypeChoice.setNullValid(false);

    return auftragsPositionsPaymentTypeChoice;
  }

  private UserSelectPanel createUserSelect(final String id) {
    final UserSelectPanel userSelectPanel = new UserSelectPanel(
        id,
        LambdaModel.of(this::getUser, this::setUser),
        parentPage,
        "user"
    );
    userSelectPanel.init();
    return userSelectPanel;
  }

  protected void refresh() {
    this.auftragsStatistik = null;
  }

  @Override
  public PFUserDO getUser() {
    return getSearchFilter().getUser();
  }

  public void setUser(final PFUserDO user) {
    getSearchFilter().setUser(user);
  }

  private Integer getAuftragsPositionsPaymentType() {
    if (getSearchFilter().getAuftragsPositionsPaymentType() != null) {
      return getSearchFilter().getAuftragsPositionsPaymentType().ordinal();
    } else {
      return -1;
    }
  }

  private void setAuftragsPositionsPaymentType(final Integer auftragsPositionsPaymentType) {
    if (auftragsPositionsPaymentType == null || auftragsPositionsPaymentType == -1) {
      getSearchFilter().setAuftragsPositionsPaymentType(null);
    } else {
      getSearchFilter().setAuftragsPositionsPaymentType(AuftragsPositionsPaymentType.values()[auftragsPositionsPaymentType]);
    }
  }

  private AuftragsStatistik getAuftragsStatistik() {
    if (auftragsStatistik == null) {
      auftragsStatistik = WicketSupport.get(AuftragDao.class).buildStatistik(getParentPage().getList());
    }
    return auftragsStatistik;
  }

  private String getStatisticsValue(final String label, final BigDecimal amount, final int count) {
    return getString(label) + ": " + CurrencyFormatter.format(amount) + " (" + count + ")";
  }

  @Override
  protected AuftragFilter newSearchFilterInstance() {
    return new AuftragFilter();
  }

  @Override
  protected Logger getLogger() {
    return log;
  }
}
