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

package org.projectforge.web.wicket.flowlayout;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.projectforge.web.wicket.WicketUtils;

/**
 * Panel containing only one check-box. <br/>
 * This component calls setRenderBodyOnly(true). If the outer html element is needed, please call setRenderBodyOnly(false).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class ButtonPanel extends Panel
{
  public static final String BUTTON_ID = "button";
  private final Button button;

  private Label label;

  public ButtonPanel(final String id, final String label, final ButtonType... buttonTypes)
  {
    super(id);
    button = new Button(BUTTON_ID, new Model<String>(label));
    button.add(new Label("title", label));
    for (final ButtonType buttonType : buttonTypes) {
      button.add(AttributeModifier.append("class", buttonType.getClassAttrValue()));
    }
    add(button);
  }

  public ButtonPanel(final String id, final String title, final Button button, final ButtonType... buttonTypes)
  {
    super(id);
    this.button = button;
    button.add(new Label("title", title));
    for (final ButtonType buttonType : buttonTypes) {
      button.add(AttributeModifier.append("class", buttonType.getClassAttrValue()));
    }
    add(button);
  }


  /**
   * Click on this button opens the dialog. The markup id of the opened dialog is set with attribute data-dialog. @see
   * {@link ButtonPanel#setDataDialog(String)}.
   */
  //  public ButtonPanel setDataDialog(final String markupId)
  //  {
  //    button.add(AttributeModifier.append("class", "dialog_button"));
  //    button.add(AttributeModifier.append("data-dialog", markupId));
  //    return this;
  //  }

  /**
   * Sets tool-tip for the label.
   * @param tooltip
   * @return this for chaining.
   */
  public ButtonPanel setTooltip(final String tooltip)
  {
    WicketUtils.addTooltip(label, tooltip);
    return this;
  }
}
