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

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.framework.utils.Constants;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.RequiredMinMaxNumberField;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.converter.CurrencyConverter;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import java.math.BigDecimal;

public class LiquidityForecastForm extends AbstractStandardForm<Object, LiquidityForecastPage>
{
  private static final long serialVersionUID = -4518924991100703065L;

  private static final String USER_PREF_KEY_SETTINGS = LiquidityForecastSettings.class.getName();

  LiquidityForecastSettings settings;

  public LiquidityForecastForm(final LiquidityForecastPage parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.liquidityplanning.forecast.startAmount"));
      final RequiredMinMaxNumberField<BigDecimal> amount = new RequiredMinMaxNumberField<BigDecimal>(
          fs.getTextFieldId(),
          new PropertyModel<>(getSettings(), "startAmount"), Constants.TEN_BILLION_NEGATIVE,
          Constants.TEN_BILLION)
      {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public IConverter getConverter(final Class type)
        {
          return new CurrencyConverter();
        }
      };
      WicketUtils.setSize(amount, 8);
      fs.add(amount);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.liquidityplanning.forecast"))
          .setUnit(getString("days"));
      final RequiredMinMaxNumberField<Integer> nextDays = new RequiredMinMaxNumberField<>(fs.getTextFieldId(),
          new PropertyModel<>(getSettings(), "nextDays"), 3, LiquidityForecastSettings.MAX_FORECAST_DAYS);
      WicketUtils.setSize(nextDays, 4);
      fs.add(nextDays);
    }
    {
      final Button callButton = new Button(SingleButtonPanel.WICKET_ID, new Model<>("execute"))
      {
        @Override
        public final void onSubmit()
        {
          // parentPage.call();
        }
      };
      final SingleButtonPanel callButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), callButton,
          getString("execute"),
          SingleButtonPanel.DEFAULT_SUBMIT);
      actionButtons.add(callButtonPanel);
      setDefaultButton(callButton);
    }
  }

  protected LiquidityForecastSettings getSettings()
  {
    if (settings == null) {
      settings = (LiquidityForecastSettings) parentPage.getUserPrefEntry(USER_PREF_KEY_SETTINGS);
    }
    if (settings == null) {
      settings = new LiquidityForecastSettings();
      parentPage.putUserPrefEntry(USER_PREF_KEY_SETTINGS, settings, true);
    }
    return settings;
  }
}
