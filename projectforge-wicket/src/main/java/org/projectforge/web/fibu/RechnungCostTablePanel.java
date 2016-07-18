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
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.fibu.AbstractRechnungsPositionDO;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.kost.KostZuweisungDO;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.framework.utils.NumberFormatter;
import org.projectforge.framework.utils.NumberHelper;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class RechnungCostTablePanel extends Panel
{
  private static final long serialVersionUID = -5732520730823126042L;

  private final WebMarkupContainer table;

  private final ListView<KostZuweisungDO> listView;

  private final List<KostZuweisungDO> model;

  private final AbstractRechnungsPositionDO position;

  /**
   * @param id
   */
  @SuppressWarnings("serial")
  public RechnungCostTablePanel(final String id, final AbstractRechnungsPositionDO position)
  {
    super(id);
    this.position = position;
    table = new WebMarkupContainer("costtable");
    add(table.setOutputMarkupId(true));
    this.model = new ArrayList<KostZuweisungDO>();
    listView = new ListView<KostZuweisungDO>("listview", model) {

      @Override
      protected void populateItem(final ListItem<KostZuweisungDO> item)
      {
        final KostZuweisungDO zuweisung = item.getModelObject();
        // row.add(new Kost1FormComponent("kost1", new PropertyModel<Kost1DO>(zuweisung, "kost1"), true)
        // .setVisible(isShowEditableKostZuweisungen()));
        final Component kost1 = new Label("kost1", KostFormatter.format(zuweisung.getKost1()));
        WicketUtils.addTooltip(kost1, KostFormatter.formatToolTip(zuweisung.getKost1()));
        item.add(kost1);
        // subItem.add(new Kost2FormComponent("kost2", new PropertyModel<Kost2DO>(zuweisung, "kost2"), true)
        // .setVisible(isShowEditableKostZuweisungen()));
        final Component kost2 = new Label("kost2", KostFormatter.format(zuweisung.getKost2()));
        WicketUtils.addTooltip(kost2, KostFormatter.formatToolTip(zuweisung.getKost2()));
        item.add(kost2);
        item.add(new Label("netto", CurrencyFormatter.format(zuweisung.getNetto())));
        final BigDecimal percentage;
        if (NumberHelper.isZeroOrNull(position.getNetSum()) == true || NumberHelper.isZeroOrNull(zuweisung.getNetto()) == true) {
          percentage = BigDecimal.ZERO;
        } else {
          percentage = zuweisung.getNetto().divide(position.getNetSum(), RoundingMode.HALF_UP);
        }
        final boolean percentageVisible = NumberHelper.isNotZero(percentage);
        item.add(new Label("percentage", NumberFormatter.formatPercent(percentage)).setVisible(percentageVisible));
        onRenderCostRow(position, zuweisung, kost1, kost2);
      }
    };
    table.add(listView);
    refresh();
  }

  /**
   * Does nothing at default.
   * @param position
   * @param costAssignment
   * @param cost1
   * @param cost2
   */
  protected void onRenderCostRow(final AbstractRechnungsPositionDO position, final KostZuweisungDO costAssignment, final Component cost1,
      final Component cost2)
  {

  }

  /**
   * @return the table
   */
  public WebMarkupContainer getTable()
  {
    return table;
  }

  /**
   * @return this for chaining.
   */
  public RechnungCostTablePanel refresh()
  {
    model.clear();
    final List<KostZuweisungDO> kostZuweisungen = position.getKostZuweisungen();
    if (kostZuweisungen != null) {
      for (final KostZuweisungDO zuweisung : kostZuweisungen) {
        if (NumberHelper.isZeroOrNull(zuweisung.getNetto()) == true) {
          continue;
        }
        model.add(zuweisung);
      }
    }
    return this;
  }
}
