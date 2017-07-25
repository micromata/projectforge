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

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.web.wicket.WicketUtils;

/**
 * Panel containing only one check-box. <br/>
 * This component calls setRenderBodyOnly(true). If the outer html element is needed, please call setRenderBodyOnly(false).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@SuppressWarnings("serial")
public class RadioGroupPanel<T extends Serializable> extends Panel
{
  private RadioGroup<T> radioGroup;

  private RepeatingView repeater;

  private boolean autosubmit;

  public RadioGroupPanel(final String id, final String groupName, final IModel<T> model)
  {
    this(id, groupName, model, null);
  }

  public RadioGroupPanel(final String id, final String groupName, final IModel<T> model, final FormComponentUpdatingBehavior updatingBehavior)
  {
    super(id);
    radioGroup = new RadioGroup<T>("radioGroup", model);

    if (updatingBehavior != null) {
      radioGroup.add(updatingBehavior);
    }
    add(radioGroup);
    radioGroup.add(repeater = new RepeatingView("repeater"));
    setRenderBodyOnly(true);
  }

  public Radio<T> add(final Model<T> model, final String label)
  {
    return add(model, label, null);
  }

  public Radio<T> add(final Model<T> model, final String labelString, final String tooltip)
  {
    final WebMarkupContainer cont = new WebMarkupContainer(repeater.newChildId());
    repeater.add(cont);
    final Radio<T> radio = new Radio<T>("radio", model, radioGroup);
    if (autosubmit == true) {
      radio.add(AttributeModifier.replace("onchange", "javascript:submit();"));
    }
    cont.add(radio);
    final Label label = new Label("label", labelString);
    label.add(AttributeModifier.replace("for", radio.setOutputMarkupId(true).getMarkupId()));
    label.setRenderBodyOnly(true);
    cont.add(label);
    if (tooltip != null) {
      WicketUtils.addTooltip(label, tooltip);
    }
    return radio;
  }

  /**
   * @return the radioGroup
   */
  public RadioGroup<T> getRadioGroup()
  {
    return radioGroup;
  }
}
