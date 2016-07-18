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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.DivType;

/**
 * For displaying messages such as errors etc.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ModalMessageDialog extends ModalDialog
{
  private static final long serialVersionUID = -5232173543634705063L;

  private Component messageComponent;

  private DivType type;

  private IModel<String> messageModel;

  private boolean titleSet;

  /**
   * @param id
   */
  public ModalMessageDialog(final String id, final IModel<String> title)
  {
    super(id);
    this.autoGenerateGridBuilder = false;
    setTitle(title);
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#open(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  public ModalDialog open(final AjaxRequestTarget target)
  {
    if (messageComponent.getOutputMarkupId() == true) {
      target.add(messageComponent);
    }
    return super.open(target);
  }

  /**
   * Don't use this method for updating messages.
   * @param message
   * @return this for chaining.
   */
  public ModalMessageDialog setMessage(final IModel<String> message)
  {
    this.messageModel = message;
    final DivTextPanel textPanel = new DivTextPanel(getMessageComponentId(), message);
    gridContentContainer.add(textPanel);
    messageComponent = textPanel.getDiv().setOutputMarkupId(true);
    return this;
  }

  /**
   * For updating or setting a fixed message.
   * @param message
   * @return this for chaining.
   */
  public ModalMessageDialog setMessage(final String message)
  {
    if (messageModel == null) {
      setMessage(new Model<String>(message));
    }
    this.messageModel.setObject(message);
    return this;
  }

  /**
   * @param message
   * @return this for chaining.
   */
  public ModalMessageDialog setMessage(final Component message)
  {
    if (this.messageComponent != null) {
      gridContentContainer.remove(this.messageComponent);
    }
    this.messageComponent = message;
    gridContentContainer.add(this.messageComponent);
    return this;
  }

  public String getMessageComponentId()
  {
    return "flowform";
  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#setTitle(org.apache.wicket.model.IModel)
   */
  @Override
  public ModalDialog setTitle(final IModel<String> title)
  {
    if (titleSet == true) {
      throw new IllegalArgumentException("You can't set the title model twice!");
    }
    titleSet = true;
    return super.setTitle(title);
  }

  public ModalMessageDialog setType(final DivType type)
  {
    this.type = type;
    return this;
  }

  @Override
  public void init()
  {
    final Form<Void> form = new Form<Void>(getFormId());
    init(form);
    if (messageComponent == null) {
      setMessage("");
    }
    if (type != null) {
      messageComponent.add(AttributeModifier.append("class", type.getClassAttrValue() + " alert-danger"));
    }
    if (titleSet == false) {
      if (type == null) {
        setTitle(getString("dialog.title.message"));
      } else if (type == DivType.ALERT_ERROR) {
        setTitle(getString("dialog.title.error"));
      } else if (type == DivType.ALERT_WARNNING) {
        setTitle(getString("dialog.title.warning"));
      } else if (type == DivType.ALERT_INFO) {
        setTitle(getString("dialog.title.information"));
      } else if (type == DivType.ALERT_SUCCESS) {
        setTitle(getString("dialog.title.success"));
      } else {
        setTitle(getString("dialog.title.message"));
      }
    }
  }
}
