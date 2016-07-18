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
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.CSSColor;
import org.projectforge.web.wicket.bootstrap.GridPanel;
import org.projectforge.web.wicket.components.JiraIssuesPanel;

/**
 * Represents a entry of a group panel. This can be a label, text field or other form components.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class FieldsetPanel extends AbstractFieldsetPanel<FieldsetPanel>
{
  public static final String FIELD_SET_CLASS = "control-group";

  public static final String LABEL_SUFFIX_ID = "labelSuffix";

  public static final String DESCRIPTION_SUFFIX_ID = "descriptionSuffix";

  private static final long serialVersionUID = -6318707656650110365L;

  private final WebMarkupContainer controls;

  private RepeatingView iconContainer;

  private Label feedbackMessageLabel;

  private String feedbackMessage;

  private Component labelSuffix, descriptionSuffix;

  private boolean initialized;

  private int childCounter = 0;

  /**
   * Adds this FieldsetPanel to the parent panel.
   * @param parent
   * @param label
   */
  public FieldsetPanel(final DivPanel parent, final FieldProperties< ? > fieldProperties)
  {
    this(parent, getString(parent, fieldProperties.getLabel()), //
        getString(parent, fieldProperties.getLabelDescription(), fieldProperties.isTranslateLabelDecsription()));
  }

  private static String getString(final Component parent, final String label)
  {
    return getString(parent, label, true);
  }

  private static String getString(final Component parent, final String label, final boolean translate)
  {
    if (translate == false || label == null) {
      return label;
    }
    return parent.getString(label);
  }

  /**
   * Adds this FieldsetPanel to the parent panel.
   * @param parent
   * @param label
   */
  public FieldsetPanel(final DivPanel parent, final String label)
  {
    this(parent, label, null);
  }

  /**
   * Adds this FieldsetPanel to the parent panel.
   * @param parent
   * @param label
   * @param description Description below or beside the label of the field-set.
   */
  public FieldsetPanel(final DivPanel parent, final String labelText, final String description)
  {
    this(parent.newChildId(), labelText, description);
    parent.add(this);
  }

  /**
   * Adds this FieldsetPanel to the parent panel.
   * @param parent
   * @param label
   * @param description Description below or beside the label of the field-set.
   */
  public FieldsetPanel(final GridPanel parent, final String labelText)
  {
    this(parent.newChildId(), labelText);
    parent.add(this);
  }

  /**
   */
  public FieldsetPanel(final String id, final String labeltext)
  {
    this(id, labeltext, null);
  }

  /**
   * @param id
   * @param labeltext If null, then the label field is invisible.
   * @param description
   */
  @SuppressWarnings("serial")
  public FieldsetPanel(final String id, final String labeltext, final String description)
  {
    super(id);
    this.labelText = labeltext;
    fieldset = new WebMarkupContainer("fieldset");
    superAdd(fieldset);
    fieldset.add(AttributeModifier.append("class", FIELD_SET_CLASS));
    fieldset.add((label = new WebMarkupContainer("label")));
    if (labelText != null) {
      label.add(new Label("labeltext", new Model<String>() {
        @Override
        public String getObject()
        {
          return labelText;
        };
      }).setRenderBodyOnly(true));
      if (description != null) {
        label.add(new Label("labeldescription", description));
      } else {
        label.add(WicketUtils.getInvisibleComponent("labeldescription"));
      }
    } else {
      label.setVisible(false);
    }
    fieldset.add(controls = new WebMarkupContainer("controls"));
    controls.add(feedbackMessageLabel = new Label("feedbackMessage", new Model<String>() {
      /**
       * @see org.apache.wicket.model.Model#getObject()
       */
      @Override
      public String getObject()
      {
        return feedbackMessage;
      }
    }) {
      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        return feedbackMessage != null;
      }
    });
    fieldsRepeater = new RepeatingView("fields");
    controls.add(fieldsRepeater);
  }

  /**
   * Please note: only labelSide=false is supported and shouldn't be called twice. The label is placed above the input fields. Default is
   * labelSide = true.
   * @param labelSide
   */
  public FieldsetPanel setLabelSide(final boolean labelSide)
  {
    if (labelSide == false) {
      fieldset.add(AttributeModifier.append("class", "vertical"));
    }
    return this;
  }

  /**
   * @param labelSuffix the labelSuffix to set
   * @return this for chaining.
   */
  public FieldsetPanel setLabelSuffix(final Component labelSuffix)
  {
    this.labelSuffix = labelSuffix;
    label.add(labelSuffix);
    return this;
  }

  /**
   * @param descriptionSuffix the descriptionSuffix to set
   * @return this for chaining.
   */
  public FieldsetPanel setDescriptionSuffix(final Component descriptionSuffix)
  {
    this.descriptionSuffix = descriptionSuffix;
    label.add(descriptionSuffix);
    return this;
  }

  /**
   * No wrap of the multiple children.
   * @return this for chaining.
   */
  public FieldsetPanel setNowrap()
  {
    // fieldDiv.add(AttributeModifier.append("style", "white-space: nowrap;"));
    return this;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel#modifyAddedChild(org.apache.wicket.Component)
   */
  @Override
  protected void modifyAddedChild(final Component child)
  {
    if (child instanceof InputPanel) {
      final InputPanel inputPanel = (InputPanel) child;
      if (inputPanel.getField() instanceof TextField) {
        inputPanel.getField().add(AttributeModifier.append("class", "text"));
      }
    }
  }

  /**
   * @param id
   * @param label
   * @param listChoice
   * @return The created ListMultipleChoicePanel.
   * @see ListMultipleChoicePanel#ListMultipleChoicePanel(String, String, ListMultipleChoice)
   */
  public <T> ListMultipleChoicePanel<T> add(final ListMultipleChoice<T> listChoice)
  {
    final ListMultipleChoicePanel<T> listChoicePanel = new ListMultipleChoicePanel<T>(newChildId(), listChoice);
    listChoicePanel.getListMultipleChoice().setLabel(new Model<String>(getLabel()));
    add(listChoicePanel);
    return listChoicePanel;
  }

  /**
   * @return The Wicket id of the embedded text fiel of {@link ListMultipleChoicePanel}.
   */
  public String getListChoiceId()
  {
    return ListMultipleChoicePanel.WICKET_ID;
  }

  /**
   * @param model
   * @param labelString
   * @return The created CheckBoxPanel.
   * @see CheckBoxPanel#CheckBoxPanel(String, IModel, String)
   */
  public CheckBoxPanel addCheckBox(final IModel<Boolean> model, final String labelString)
  {
    return addCheckBox(model, labelString, null);
  }

  /**
   * @param model
   * @param labelString
   * @return The created CheckBoxPanel.
   * @see CheckBoxPanel#CheckBoxPanel(String, IModel, String)
   */
  public CheckBoxPanel addCheckBox(final IModel<Boolean> model, final String labelString, final String tooltip)
  {
    final CheckBoxPanel checkBox = new CheckBoxPanel(newChildId(), model, labelString);
    if (tooltip != null) {
      checkBox.setTooltip(tooltip);
    }
    add(checkBox);
    return checkBox;
  }

  /**
   * Adds an alert icon at the top left corner of the field set label.
   * @param tooltip
   * @return this for chaining.
   */
  public FieldsetPanel addAlertIcon(final String tooltip)
  {
    final IconPanel icon = WicketUtils.getAlertTooltipIcon(this, new ResourceModel("common.attention"), Model.of(tooltip));
    add(icon, FieldSetIconPosition.TOP_LEFT);
    return this;
  }

  /**
   * Adds a help icon at the top right corner of the field set.
   * @param title
   * @param tooltip
   * @return The created IconPanel.
   */
  public IconPanel addHelpIcon(final IModel<String> title, final IModel<String> tooltip)
  {
    return addHelpIcon(title, tooltip, FieldSetIconPosition.TOP_RIGHT);
  }

  /**
   * Adds a help icon at the top right corner of the field set.
   * @param title
   * @param tooltip
   * @param iconPosition
   * @return The created IconPanel.
   */
  public IconPanel addHelpIcon(final IModel<String> title, final IModel<String> tooltip, final FieldSetIconPosition iconPosition)
  {
    final IconPanel icon = new IconPanel(newIconChildId(), IconType.HELP, title, tooltip).setColor(CSSColor.GRAY);
    add(icon, iconPosition);
    return icon;
  }

  /**
   * Adds a help icon at the top right corner of the field set.
   * @param tooltip
   * @return The created IconPanel.
   */
  public IconPanel addHelpIcon(final String tooltip)
  {
    return addHelpIcon(tooltip, FieldSetIconPosition.TOP_RIGHT);
  }

  /**
   * Adds a help icon at the top right corner of the field set.
   * @param tooltip
   * @return The created IconPanel.
   */
  public IconPanel addHelpIcon(final String tooltip, final FieldSetIconPosition iconPosition)
  {
    final IconPanel icon = new IconPanel(newIconChildId(), IconType.HELP, tooltip).setColor(CSSColor.GRAY);
    add(icon, iconPosition);
    return icon;
  }

  /**
   * Adds a help icon at the top right corner of the field set.
   * @param tooltip
   * @return The created IconPanel.
   */
  public IconPanel addHelpIcon(final IModel<String> tooltip)
  {
    final IconPanel icon = new IconPanel(newIconChildId(), IconType.HELP, tooltip).setColor(CSSColor.GRAY);
    add(icon, FieldSetIconPosition.TOP_RIGHT);
    return icon;
  }

  /**
   * Adds a keyboard icon at the bottom right corner of the field set.
   * @param tooltip
   * @return this for chaining.
   */
  public FieldsetPanel addKeyboardHelpIcon(final String tooltip)
  {
    return add(new IconPanel(newIconChildId(), IconType.KEYBOARD, tooltip).setColor(CSSColor.GRAY), FieldSetIconPosition.BOTTOM_RIGHT);
  }

  /**
   * Adds a keyboard icon at the bottom right corner of the field set.
   * @param tooltip
   * @return this for chaining.
   */
  public FieldsetPanel addKeyboardHelpIcon(final IModel<String> title, final IModel<String> tooltip)
  {
    return add(new IconPanel(newIconChildId(), IconType.KEYBOARD, title, tooltip).setColor(CSSColor.GRAY),
        FieldSetIconPosition.BOTTOM_RIGHT);
  }

  /**
   * Adds a JIRA icon at the bottom right corner of the field set (only if JIRA is configured, otherwise this method does nothing). This
   * method is automatically called by {@link #addJIRAField()}.
   * @param tooltip
   * @return this for chaining.
   */
  public FieldsetPanel addJIRASupportHelpIcon()
  {
    if (WicketUtils.isJIRAConfigured() == true) {
      return add(WicketUtils.getJIRASupportTooltipIcon(this).setColor(CSSColor.GRAY), FieldSetIconPosition.TOP_RIGHT);
    } else {
      // No JIRA configured.
      return this;
    }
  }

  /**
   * Adds a JIRA icon at the bottom right corner of the field set (only if JIRA is configured, otherwise this method does nothing).
   * @param tooltip
   * @return this for chaining.
   */
  public FieldsetPanel addJIRAField(final IModel<String> model)
  {
    if (WicketUtils.isJIRAConfigured() == true) {
      add(new JiraIssuesPanel(newChildId(), model));
      add(WicketUtils.getJIRASupportTooltipIcon(this).setColor(CSSColor.GRAY), FieldSetIconPosition.TOP_RIGHT);
    }
    return this;
  }

  public FieldsetPanel add(final IconPanel icon, final FieldSetIconPosition iconPosition)
  {
    icon.getDiv().add(AttributeModifier.append("class", iconPosition.getStyleAttrValue()));
    if (iconContainer == null) {
      throw new IllegalArgumentException("No icons container given! May-be you forget to call newIconChildId() before adding an image.");
    }
    iconContainer.add(icon).setRenderBodyOnly(true);
    return this;
  }

  public String newIconChildId()
  {
    if (iconContainer == null) {
      iconContainer = new RepeatingView("icons");
      fieldset.add(iconContainer);
    }
    return iconContainer.newChildId();
  }

  /**
   * @return the feedbackMessage
   */
  public Label getFeedbackMessageLabel()
  {
    return feedbackMessageLabel;
  }

  /**
   * @param feedbackMessage
   * @return this for chaining.
   */
  public FieldsetPanel setFeedbackMessage(final String feedbackMessage)
  {
    this.feedbackMessage = feedbackMessage;
    return this;
  }

  /**
   * Creates and add a new RepeatingView as div-child if not already exist.
   * @see RepeatingView#newChildId()
   */
  @Override
  public String newChildId()
  {
    if (childCounter++ == 1) {
      controls.add(AttributeModifier.append("class", "controls-row"));
    }
    return fieldsRepeater.newChildId();
  }

  public FieldsetPanel removeAllFields()
  {
    fieldsRepeater.removeAll();
    return this;
  }

  public FieldsetPanel setDivStyle(final DivType divType)
  {
    this.controls.add(AttributeModifier.append("class", divType.getClassAttrValue()));
    return this;
  }

  public DivPanel addNewCheckBoxButtonDiv()
  {
    final DivPanel checkBoxDiv = new DivPanel(newChildId(), DivType.BTN_GROUP);
    add(checkBoxDiv);
    return checkBoxDiv;
  }

  public DivPanel addNewRadioBoxButtonDiv()
  {
    final DivPanel radioBoxDiv = new DivPanel(newChildId(), DivType.BTN_GROUP);
    add(radioBoxDiv);
    return radioBoxDiv;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel#onBeforeRender()
   */
  @Override
  protected void onBeforeRender()
  {
    if (initialized == false) {
      // Can't be done in onInitialize() because this component is added to its parent in constructor and onInitialize() is some times
      // called before the setter methods (e. g. for labelSide) are called.
      initialized = true;
      if (labelSuffix == null) {
        label.add(labelSuffix = WicketUtils.getInvisibleComponent("labelSuffix"));
      }
      if (descriptionSuffix == null) {
        label.add(descriptionSuffix = WicketUtils.getInvisibleComponent("descriptionSuffix"));
      }
      if (iconContainer == null) {
        fieldset.add(new WebMarkupContainer("icons").setVisible(false));
      }
    }
    super.onBeforeRender();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel#addChild(org.apache.wicket.Component[])
   */
  @Override
  protected MarkupContainer addChild(final Component... childs)
  {
    return controls.add(childs);
  }

  public WebMarkupContainer getControlsDiv()
  {
    return controls;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel#getThis()
   */
  @Override
  protected FieldsetPanel getThis()
  {
    return this;
  }

  public boolean hasChilds()
  {
    return childCounter > 0;
  }
}
