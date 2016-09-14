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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.MyComponentsRepeater;

public class AbstractStandardForm<F, P extends AbstractStandardFormPage> extends AbstractForm<F, P>
{
  private static final long serialVersionUID = -2450673501083584299L;

  protected GridBuilder gridBuilder;

  protected MyComponentsRepeater<SingleButtonPanel> actionButtons;

  private WebMarkupContainer feedbackAndMessagesPanel;

  protected FeedbackPanel feedbackPanel;

  protected Component messagesComponent;

  /**
   * Cross site request forgery token.
   */
  private final CsrfTokenHandler csrfTokenHandler;

  public AbstractStandardForm(final P parentPage)
  {
    super(parentPage);
    csrfTokenHandler = new CsrfTokenHandler(this);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    feedbackAndMessagesPanel = new WebMarkupContainer("feedbackAndMessagesPanel") {
      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        return isMessageAndFeedbackPanelVisible();
      }
    };
    add(feedbackAndMessagesPanel);
    feedbackPanel = createFeedbackPanel();
    feedbackAndMessagesPanel.add(feedbackPanel);
    messagesComponent = createMessageComponent();
    feedbackAndMessagesPanel.add(messagesComponent);
    gridBuilder = newGridBuilder(this, "flowform");
    actionButtons = new MyComponentsRepeater<SingleButtonPanel>("buttons");
    final WebMarkupContainer buttonBar = new WebMarkupContainer("buttonBar") {
      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        return actionButtons.hasEntries();
      }
    };
    add(buttonBar);
    buttonBar.add(actionButtons.getRepeatingView());
  }

  protected SingleButtonPanel addCancelButton(final Button cancelButton)
  {
    cancelButton.setDefaultFormProcessing(false); // No validation of the form.
    final SingleButtonPanel cancelButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), cancelButton, getString("cancel"),
        SingleButtonPanel.CANCEL);
    actionButtons.add(cancelButtonPanel);
    return cancelButtonPanel;
  }

  /**
   * Adds invisible container as default.
   */
  protected Component createMessageComponent()
  {
    return new WebMarkupContainer("message").setVisible(false);
  }

  /**
   * @return true if the embedded feedback and/or message container is visible.
   */
  protected boolean isMessageAndFeedbackPanelVisible()
  {
    return feedbackPanel.anyErrorMessage() || messagesComponent.isVisible();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractForm#onBeforeRender()
   */
  @Override
  public void onBeforeRender()
  {
    super.onBeforeRender();
    actionButtons.render();
  }

  /**
   * @see org.apache.wicket.markup.html.form.Form#onSubmit()
   */
  @Override
  protected void onSubmit()
  {
    super.onSubmit();
    csrfTokenHandler.onSubmit();
  }
}
