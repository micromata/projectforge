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

import java.io.Serializable;
import java.math.BigDecimal;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.projectforge.business.fibu.kost.BusinessAssessment;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;
import org.projectforge.web.wicket.flowlayout.TextStyle;

public abstract class BusinessAssessment4Fieldset implements Serializable
{
  private static final long serialVersionUID = -1729601074426329958L;

  /**
   * @param id
   */
  @SuppressWarnings("serial")
  public BusinessAssessment4Fieldset(final GridBuilder gridBuilder)
  {
    final FieldsetPanel fs = new FieldsetPanel(gridBuilder.getPanel(), gridBuilder.getString("fibu.businessAssessment")) {
      @Override
      public boolean isVisible() {
        return BusinessAssessment4Fieldset.this.isVisible();
      };
    }.suppressLabelForWarning();
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject()
      {
        final BusinessAssessment bwa = getBusinessAssessment();
        return fs.getString("fibu.businessAssessment.overallPerformance")
            + ": "
            + CurrencyFormatter.format(bwa != null ? bwa.getOverallPerformanceRowAmount() : BigDecimal.ZERO)
            + WebConstants.HTML_TEXT_DIVIDER;
      }
    }, TextStyle.BLUE));
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject()
      {
        final BusinessAssessment bwa = getBusinessAssessment();
        return fs.getString("fibu.businessAssessment.merchandisePurchase")
            + ": "
            + CurrencyFormatter.format(bwa != null ? bwa.getMerchandisePurchaseRowAmount() : BigDecimal.ZERO)
            + WebConstants.HTML_TEXT_DIVIDER;
      }
    }));
    fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
      @Override
      public String getObject()
      {
        final BusinessAssessment bwa = getBusinessAssessment();
        return fs.getString("fibu.businessAssessment.preliminaryResult")
            + ": "
            + CurrencyFormatter.format(bwa != null ? bwa.getPreliminaryResultRowAmount() : BigDecimal.ZERO);
      }
    }));

    final RepeatingView repeater = new RepeatingView(FieldsetPanel.DESCRIPTION_SUFFIX_ID) {
      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        return getBusinessAssessment() != null;
      }
    };
    fs.setDescriptionSuffix(repeater);
    IconPanel icon = new IconPanel(repeater.newChildId(), IconType.PLUS_SIGN).setOnClick("javascript:showBusinessAssessment();");
    icon.setMarkupId("showBusinessAssessment").setOutputMarkupId(true);
    repeater.add(icon);
    icon = new IconPanel(repeater.newChildId(), IconType.MINUS_SIGN).setOnClick("javascript:hideBusinessAssessment();").appendAttribute(
        "style", "display: none;");
    icon.setMarkupId("hideBusinessAssessment").setOutputMarkupId(true);
    repeater.add(icon);

    gridBuilder.newGridPanel();
    final DivPanel businessAssessmentPanel = gridBuilder.getPanel();
    businessAssessmentPanel.setMarkupId("businessAssessment");
    businessAssessmentPanel.add(AttributeModifier.append("style", "display: none;"));
    final FieldsetPanel fieldset = new FieldsetPanel(businessAssessmentPanel, "").suppressLabelForWarning();
    final Label label = new Label(DivTextPanel.WICKET_ID, new Model<String>() {
      /**
       * @see org.apache.wicket.model.Model#getObject()
       */
      @Override
      public String getObject()
      {
        final BusinessAssessment businessAssessment = getBusinessAssessment();
        if (businessAssessment == null) {
          return "";
        }
        return businessAssessment.asHtml();
      }
    });
    label.setEscapeModelStrings(false);
    fieldset.add(new DivTextPanel(fieldset.newChildId(), label).setMarkupId("businessAssessment"));
  }

  protected boolean isVisible()
  {
    return true;
  }

  protected abstract BusinessAssessment getBusinessAssessment();
}
