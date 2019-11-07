/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.liquidityplanning;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.fibu.AmountType;
import org.projectforge.business.fibu.PaymentStatus;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;

/**
 * The list formular for the list view (this example has no filter settings). See ToDoListPage for seeing how to use
 * filter settings.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LiquidityEntryListForm extends AbstractListForm<LiquidityFilter, LiquidityEntryListPage>
{
  private static final long serialVersionUID = 2040255193023406307L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LiquidityEntryListForm.class);

  public LiquidityEntryListForm(final LiquidityEntryListPage parentPage)
  {
    super(parentPage);
  }

  private LiquidityEntriesStatistics getStats()
  {
    return parentPage.getStatistics();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#init()
   */
  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newGridPanel();
    {
      // Statistics
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("statistics")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return getString("fibu.rechnung.status.bezahlt")
              + ": "
              + CurrencyFormatter.format(getStats().getPaid())
              + WebConstants.HTML_TEXT_DIVIDER;
        }
      }));
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return getString("totalSum") + ": " + CurrencyFormatter.format(getStats().getTotal())
              + WebConstants.HTML_TEXT_DIVIDER;
        }
      }));
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return getString("fibu.rechnung.offen") + ": " + CurrencyFormatter.format(getStats().getOpen())
              + WebConstants.HTML_TEXT_DIVIDER;
        }
      }, TextStyle.BLUE));
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return getString("fibu.rechnung.filter.ueberfaellig") + ": "
              + CurrencyFormatter.format(getStats().getOverdue());
        }
      }, TextStyle.RED));
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   *      org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @SuppressWarnings("serial")
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    // DropDownChoice next days
    final LabelValueChoiceRenderer<Integer> nextDaysRenderer = new LabelValueChoiceRenderer<>();
    nextDaysRenderer.addValue(0, getString("filter.all"));
    nextDaysRenderer.addValue(7, getLocalizedMessage("search.nextDays", 7));
    nextDaysRenderer.addValue(10, getLocalizedMessage("search.nextDays", 10));
    nextDaysRenderer.addValue(14, getLocalizedMessage("search.nextDays", 14));
    nextDaysRenderer.addValue(30, getLocalizedMessage("search.nextDays", 30));
    nextDaysRenderer.addValue(60, getLocalizedMessage("search.nextDays", 60));
    nextDaysRenderer.addValue(90, getLocalizedMessage("search.nextDays", 90));
    final DropDownChoice<Integer> nextDaysChoice = new DropDownChoice<>(
        optionsFieldsetPanel.getDropDownChoiceId(),
        new PropertyModel<>(getSearchFilter(), "nextDays"), nextDaysRenderer.getValues(), nextDaysRenderer);
    nextDaysChoice.setNullValid(false);
    optionsFieldsetPanel.add(nextDaysChoice, true);
    {
      final DivPanel radioGroupPanel = optionsFieldsetPanel.addNewRadioBoxButtonDiv();
      final RadioGroupPanel<PaymentStatus> radioGroup = new RadioGroupPanel<PaymentStatus>(radioGroupPanel.newChildId(),
          "paymentStatus",
          new PropertyModel<>(getSearchFilter(), "paymentStatus"))
      {
        /**
         * @see org.projectforge.web.wicket.flowlayout.RadioGroupPanel#wantOnSelectionChangedNotifications()
         */
        @Override
        protected boolean wantOnSelectionChangedNotifications()
        {
          return true;
        }

        /**
         * @see org.projectforge.web.wicket.flowlayout.RadioGroupPanel#onSelectionChanged(java.lang.Object)
         */
        @Override
        protected void onSelectionChanged(final Object newSelection)
        {
          parentPage.refresh();
        }
      };
      radioGroupPanel.add(radioGroup);
      radioGroup.add(new Model<>(PaymentStatus.ALL), getString(PaymentStatus.ALL.getI18nKey()));
      radioGroup.add(new Model<>(PaymentStatus.UNPAID), getString(PaymentStatus.UNPAID.getI18nKey()));
      radioGroup.add(new Model<>(PaymentStatus.PAID), getString(PaymentStatus.PAID.getI18nKey()));
    }
    {
      final DivPanel radioGroupPanel = optionsFieldsetPanel.addNewRadioBoxButtonDiv();
      final RadioGroupPanel<AmountType> radioGroup = new RadioGroupPanel<AmountType>(radioGroupPanel.newChildId(),
          "amountType",
          new PropertyModel<>(getSearchFilter(), "amountType"))
      {
        /**
         * @see org.projectforge.web.wicket.flowlayout.RadioGroupPanel#wantOnSelectionChangedNotifications()
         */
        @Override
        protected boolean wantOnSelectionChangedNotifications()
        {
          return true;
        }

        /**
         * @see org.projectforge.web.wicket.flowlayout.RadioGroupPanel#onSelectionChanged(java.lang.Object)
         */
        @Override
        protected void onSelectionChanged(final Object newSelection)
        {
          parentPage.refresh();
        }
      };
      radioGroupPanel.add(radioGroup);
      radioGroup.add(new Model<>(AmountType.ALL), getString(AmountType.ALL.getI18nKey()));
      radioGroup.add(new Model<>(AmountType.CREDIT), getString(AmountType.CREDIT.getI18nKey()));
      radioGroup.add(new Model<>(AmountType.DEBIT), getString(AmountType.DEBIT.getI18nKey()));
    }
  }

  @Override
  protected LiquidityFilter newSearchFilterInstance()
  {
    return new LiquidityFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
