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

package org.projectforge.web.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;

/**
 * For displaying yes/no questions.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ModalQuestionDialog extends ModalDialog
{
  private static final long serialVersionUID = -7595805701020378523L;

  private Label questionComponent;

  private final IModel<String> headingModel, questionModel;

  private boolean confirmed = false;

  /**
   * @param id
   */
  public ModalQuestionDialog(final String id, final IModel<String> heading, final IModel<String> question)
  {
    super(id);
    this.headingModel = heading;
    this.questionModel = question;
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#open(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  public ModalDialog open(final AjaxRequestTarget target)
  {
    target.add(questionComponent);
    confirmed = false;
    return super.open(target);
  }

  /**
   * For updating or setting a fixed message.
   * @param message
   * @return this for chaining.
   */
  public ModalQuestionDialog setQuestion(final String question)
  {
    this.questionModel.setObject(question);
    return this;
  }

  /**
   * For updating or setting a fixed heading.
   * @param message
   * @return this for chaining.
   */
  public ModalQuestionDialog setHeading(final String heading)
  {
    this.headingModel.setObject(heading);
    return this;
  }

  /**
   * @return true if the user clicked the Yes button, otherwise false.
   */
  public boolean isConfirmed()
  {
    return confirmed;
  }

  @Override
  public void init()
  {
    setTitle(this.headingModel);
    setShowCancelButton();
    setCloseButtonLabel(getString("yes"));
    final Form<Void> form = new Form<Void>(getFormId());
    init(form);
    gridBuilder.newGridPanel();
    final DivPanel panel = gridBuilder.getPanel();
    final DivTextPanel questionPanel = new DivTextPanel(panel.newChildId(), this.questionModel);
    this.questionComponent = questionPanel.getLabel4Ajax();
    panel.add(questionPanel);
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#onCloseButtonSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  protected boolean onCloseButtonSubmit(final AjaxRequestTarget target)
  {
    confirmed = true;
    return super.onCloseButtonSubmit(target);
  }
}
