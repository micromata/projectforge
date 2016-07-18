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

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.common.StringHelper;
import org.projectforge.reporting.Kost2Art;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ProjectEditCost2TypeTablePanel extends Panel
{
  private static final long serialVersionUID = -5732520730823126042L;

  /**
   * @param id
   */
  public ProjectEditCost2TypeTablePanel(final String id)
  {
    super(id);
  }

  public void init(final List<Kost2Art> kost2Arts)
  {
    final Iterator<Kost2Art> it = kost2Arts.iterator();
    final RepeatingView kost2artRowsRepeater = new RepeatingView("kost2artRows");
    add(kost2artRowsRepeater);
    while (it.hasNext() == true) {
      final WebMarkupContainer rowItem = new WebMarkupContainer(kost2artRowsRepeater.newChildId());
      kost2artRowsRepeater.add(rowItem);
      final RepeatingView kost2artColsRepeater = new RepeatingView("kost2artCols");
      rowItem.add(kost2artColsRepeater);
      for (int i = 0; i < 2 && it.hasNext() == true; i++) {
        final WebMarkupContainer colItem = new WebMarkupContainer(kost2artColsRepeater.newChildId());
        kost2artColsRepeater.add(colItem);
        final Kost2Art kost2Art = it.next();
        String style = null;
        if (kost2Art.isExistsAlready() == true) {
          if (kost2Art.isProjektStandard() == true) {
            style = "color: green;";
          }
        } else {
          if (kost2Art.isProjektStandard() == true) {
            style = "color: red;";
          }
        }
        final CheckBox checkBox = new CheckBox("kost2artSelect", new PropertyModel<Boolean>(kost2Art, "selected"));
        colItem.add(checkBox);
        final IconPanel image = new IconPanel("acceptImage", IconType.ACCEPT);
        image.setTooltip(new ResourceModel("fibu.projekt.edit.kost2DoesAlreadyExists"));
        colItem.add(image);
        if (kost2Art.isExistsAlready() == true) {
          checkBox.setVisible(false);
        } else {
          image.setVisibilityAllowed(false);
        }
        final Label kost2artNummerLabel = new Label("kost2artNummer", StringHelper.format2DigitNumber(kost2Art.getId()));
        colItem.add(kost2artNummerLabel);
        final Label kost2artNameLabel = new Label("kost2artName", kost2Art.isFakturiert() == true ? kost2Art.getName() : kost2Art.getName()
            + " (nf)");
        colItem.add(kost2artNameLabel);
        if (style != null) {
          kost2artNummerLabel.add(AttributeModifier.replace("style", style));
          kost2artNameLabel.add(AttributeModifier.replace("style", style));
        }
      }
    }
  }
}
