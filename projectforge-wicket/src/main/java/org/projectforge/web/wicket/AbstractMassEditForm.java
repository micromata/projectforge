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

package org.projectforge.web.wicket;

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.MyComponentsRepeater;

public abstract class AbstractMassEditForm<O extends Serializable, P extends AbstractMassEditPage> extends AbstractSecuredForm<O, P>
{
  private static final long serialVersionUID = -6707610179583359099L;

  /**
   * List to create content menu in the desired order before creating the RepeatingView.
   */
  protected MyComponentsRepeater<SingleButtonPanel> actionButtons;

  protected FeedbackPanel feedbackPanel;

  protected GridBuilder gridBuilder;

  public AbstractMassEditForm(final P parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    feedbackPanel = new FeedbackPanel("feedback");
    feedbackPanel.setOutputMarkupId(true);
    add(feedbackPanel);

    gridBuilder = newGridBuilder(this, "flowform");

    actionButtons = new MyComponentsRepeater<SingleButtonPanel>("buttons");
    add(actionButtons.getRepeatingView());
    {
      final Button cancelButton = new Button("button", new Model<String>("cancel")) {
        @Override
        public final void onSubmit()
        {
          parentPage.cancel();
        }
      };
      cancelButton.setDefaultFormProcessing(false); // No validation of the
      final SingleButtonPanel cancelButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), cancelButton, getString("cancel"),
          SingleButtonPanel.CANCEL);
      actionButtons.add(cancelButtonPanel);
    }
    {
      final Button updateAllButton = new Button("button", new Model<String>("updateAll")) {
        @Override
        public final void onSubmit()
        {
          parentPage.updateAll();
        }
      };
      final SingleButtonPanel updateAllButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), updateAllButton,
          getString("updateAll"), SingleButtonPanel.DEFAULT_SUBMIT);
      updateAllButton.add(AttributeModifier.replace("onclick", "return showUpdateQuestionDialog()"));
      actionButtons.add(updateAllButtonPanel);
      setDefaultButton(updateAllButton);
    }
  }

  @Override
  public void onBeforeRender()
  {
    actionButtons.render();
    super.onBeforeRender();
  }
}
