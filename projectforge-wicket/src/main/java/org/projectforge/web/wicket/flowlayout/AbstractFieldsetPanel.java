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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.LabeledWebMarkupContainer;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.common.StringHelper;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.WicketUtils;
import org.wicketstuff.select2.Select2MultiChoice;

/**
 * Represents a entry of a group panel. This can be a label, text field or other form components.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class AbstractFieldsetPanel<T extends AbstractFieldsetPanel<?>> extends Panel
{
  private static final long serialVersionUID = -4215154959282166107L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AbstractFieldsetPanel.class);

  protected WebMarkupContainer fieldset;

  protected WebMarkupContainer label;

  protected boolean labelFor;

  protected String labelText;

  protected RepeatingView fieldsRepeater;

  private final List<FormComponent<?>> allFormComponents = new ArrayList<FormComponent<?>>();

  private Object storeObject;

  /**
   * Adds this FieldsetPanel to the parent panel.
   *
   * @param parent
   * @param label
   */
  protected AbstractFieldsetPanel(final String id)
  {
    super(id);
  }

  public T setUnit(final String unit)
  {
    this.labelText = WicketUtils.getLabelWithUnit(labelText, unit);
    return getThis();
  }

  /**
   * @return the labelText
   */
  public String getLabel()
  {
    return labelText;
  }

  /**
   * Sets the background color of this whole fieldset to red.
   *
   * @return this for chaining.
   */
  public T setWarningBackground()
  {
    fieldset.add(AttributeModifier.replace("style", WebConstants.CSS_BACKGROUND_COLOR_RED));
    return getThis();
  }

  public T setLabelFor(final Component component)
  {
    if (component instanceof ComponentWrapperPanel) {
      this.label.add(AttributeModifier.replace("for", ((ComponentWrapperPanel) component).getComponentOutputId()));
    } else {
      this.label.add(AttributeModifier.replace("for", component.getMarkupId()));
    }
    labelFor = true;
    return getThis();
  }

  /**
   * Declares that there is no validation field which the label should set for. This has no other meaning and effect than not to display the
   * development warning "No label set for field...'.
   *
   * @return
   */
  public T suppressLabelForWarning()
  {
    labelFor = true;
    return getThis();
  }

  public Component superAdd(final Component... childs)
  {
    return super.add(childs);
  }

  /**
   * @see org.apache.wicket.MarkupContainer#add(org.apache.wicket.Component[])
   */
  @Override
  public MarkupContainer add(final Component... childs)
  {
    checkLabelFor(childs);
    for (final Component component : childs) {
      modifyAddedChild(component);
      addFormComponent(component);
    }
    return fieldsRepeater.add(childs);
  }

  private void addFormComponent(final Component component)
  {
    if (component instanceof FormComponent<?>) {
      this.allFormComponents.add((FormComponent<?>) component);
    } else if (component instanceof ComponentWrapperPanel) {
      final FormComponent<?> fc = ((ComponentWrapperPanel) component).getFormComponent();
      if (fc != null) {
        this.allFormComponents.add(fc);
      }
    }
  }

  /**
   * Checks all child form components and calls {@link FormComponent#isValid()}.
   *
   * @return true if all childs are valid, otherwise false (if any child is invalid);
   */
  public boolean isValid()
  {
    for (final FormComponent<?> formComponent : allFormComponents) {
      if (formComponent.isValid() == false) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return true if any form child has a feedback message.
   * @see org.apache.wicket.Component#hasFeedbackMessage()
   */
  public boolean hasFormChildsFeedbackMessage()
  {
    for (final FormComponent<?> formComponent : allFormComponents) {
      if (formComponent.hasFeedbackMessage() == true) {
        return true;
      }
    }
    return false;
  }

  public String getFormChildsFeedbackMessages(final boolean markAsRendered)
  {
    if (hasFormChildsFeedbackMessage() == false) {
      return null;
    }
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final FormComponent<?> formComponent : allFormComponents) {
      if (formComponent.hasFeedbackMessage() == true) {
        final FeedbackMessage feedbackMessage = formComponent.getFeedbackMessages().first();
        if (markAsRendered == true) {
          feedbackMessage.markRendered();
        }
        first = StringHelper.append(buf, first, feedbackMessage.getMessage().toString(), "\n");
      }
    }
    return buf.toString();
  }

  /**
   * @param textField
   * @return The created InputPanel.
   * @see InputPanel#InputPanel(String, Component)
   */
  public InputPanel add(final TextField<?> textField)
  {
    final InputPanel input = new InputPanel(newChildId(), textField);
    add(input);
    return input;
  }

  /**
   * @param textField
   * @return The created InputPanel.
   * @see InputPanel#InputPanel(String, Component)
   */
  public InputPanel add(final TextField<?> textField, final FieldProperties<?> fieldProperties)
  {
    final InputPanel input = add(textField);
    if (fieldProperties.getFieldType() != null) {
      setFieldType(input, fieldProperties.getFieldType());
    }
    return input;
  }

  protected InputPanel setFieldType(final InputPanel input, final FieldType fieldType)
  {
    input.setFieldType(fieldType);
    return input;
  }

  /**
   * @param passwordField
   * @return The created PasswordPanel.
   * @see PasswordPanel#PasswordPanel(String, Component)
   */
  public PasswordPanel add(final PasswordTextField passwordField)
  {
    final PasswordPanel passwordInput = new PasswordPanel(newChildId(), passwordField);
    add(passwordInput);
    return passwordInput;
  }

  /**
   * @return The Wicket id of the embedded text field of InputPanel
   */
  public final String getTextFieldId()
  {
    return InputPanel.WICKET_ID;
  }

  /**
   * @param textArea
   * @return The created InputPanel.
   * @see TextAreaPanel#TextAreaPanel(String, Component)
   */
  public TextAreaPanel add(final TextArea<?> textArea)
  {
    return add(textArea, false);
  }

  /**
   * @param textArea
   * @return The created InputPanel.
   * @see TextAreaPanel#TextAreaPanel(String, Component)
   */
  public TextAreaPanel add(final TextArea<?> textArea, final boolean autogrow)
  {
    final TextAreaPanel panel = new TextAreaPanel(newChildId(), textArea, autogrow);
    add(panel);
    return panel;
  }

  /**
   * @return The Wicket id of the embedded text field of TextAreaPanel
   */
  public final String getTextAreaId()
  {
    return TextAreaPanel.WICKET_ID;
  }

  /**
   * @param id
   * @param label
   * @param model
   * @param values
   * @param renderer
   * @return The created DropDownChoicePanel.
   * @see DropDownChoicePanel#DropDownChoicePanel(String, String, IModel, List, IChoiceRenderer)
   */
  public <C> DropDownChoicePanel<C> addDropDownChoice(final IModel<C> model, final List<? extends C> values,
      final IChoiceRenderer<C> renderer)
  {
    return addDropDownChoice(model, values, renderer, false);
  }

  /**
   * @param id
   * @param label
   * @param model
   * @param values
   * @param renderer
   * @param submitOnChange.
   * @return The created DropDownChoicePanel.
   * @see DropDownChoicePanel#DropDownChoicePanel(String, String, IModel, List, IChoiceRenderer, boolean))
   */
  public <C> DropDownChoicePanel<C> addDropDownChoice(final IModel<C> model, final List<? extends C> values,
      final IChoiceRenderer<C> renderer, final boolean submitOnChange)
  {
    final DropDownChoicePanel<C> dropDownChoicePanel = new DropDownChoicePanel<C>(newChildId(), model, values, renderer, submitOnChange);
    add(dropDownChoicePanel);
    return dropDownChoicePanel;
  }

  /**
   * @param id
   * @param label
   * @param dropDownChoice
   * @return The created DropDownChoicePanel.
   * @see DropDownChoicePanel#DropDownChoicePanel(String, String, DropDownChoice)
   */
  public <C> DropDownChoicePanel<C> add(final DropDownChoice<C> dropDownChoice)
  {
    return add(dropDownChoice, false);
  }

  /**
   * @param id
   * @param label
   * @param dropDownChoice
   * @return The created DropDownChoicePanel.
   * @see DropDownChoicePanel#DropDownChoicePanel(String, String, DropDownChoice, boolean)
   */
  public <C> DropDownChoicePanel<C> add(final DropDownChoice<C> dropDownChoice, final boolean submitOnChange)
  {

    final DropDownChoicePanel<C> dropDownChoicePanel = new DropDownChoicePanel<C>(newChildId(), dropDownChoice, submitOnChange);
    add(dropDownChoicePanel);
    return dropDownChoicePanel;
  }

  /**
   * @return The Wicket id of the embedded select field of {@link DropDownChoicePanel}.
   */
  public String getDropDownChoiceId()
  {
    return DropDownChoicePanel.WICKET_ID;
  }

  /**
   * @param id
   * @param select
   * @return The created SelectPanel.
   * @see SelectPanel#SelectPanel(String, Select)
   */
  public <C> SelectPanel<C> add(final Select<C> select)
  {

    final SelectPanel<C> selectPanel = new SelectPanel<C>(newChildId(), select);
    add(selectPanel);
    return selectPanel;
  }

  /**
   * @return The Wicket id of the embedded select field of {@link DropDownChoicePanel}.
   */
  public String getSelectId()
  {
    return SelectPanel.WICKET_ID;
  }

  /**
   * @param id
   * @param ajaxEditableLabel
   * @return The created AjaxEditableLabelPanel.
   * @see SelectPanel#SelectPanel(String, Select)
   */
  public <C> AjaxEditableLabelPanel<C> add(final AjaxEditableLabel<C> ajaxEditableLabel)
  {

    final AjaxEditableLabelPanel<C> ajaxEditableLabelPanel = new AjaxEditableLabelPanel<C>(newChildId(), ajaxEditableLabel);
    add(ajaxEditableLabelPanel);
    return ajaxEditableLabelPanel;
  }

  /**
   * @return The Wicket id of the embedded select field of {@link DropDownChoicePanel}.
   */
  public String getAjaxEditableLabelId()
  {
    return AjaxEditableLabelPanel.WICKET_ID;
  }

  public <C> Select2MultiChoicePanel<C> add(final Select2MultiChoice<C> select2MultiChoice)
  {
    final Select2MultiChoicePanel<C> select2MultiChoicePanel = new Select2MultiChoicePanel<C>(newChildId(), select2MultiChoice);
    add(select2MultiChoicePanel);
    return select2MultiChoicePanel;
  }

  /**
   * @return The Wicket id of the embedded text fiel of {@link DropDownChoicePanel}.
   */
  public String getSelect2MultiChoiceId()
  {
    return Select2MultiChoicePanel.WICKET_ID;
  }

  public abstract String newChildId();

  protected void modifyAddedChild(final Component child)
  {

  }

  protected abstract MarkupContainer addChild(Component... childs);

  private void checkLabelFor(final Component... components)
  {
    if (labelFor == true) {
      return;
    }
    final Component component = components[0];
    if (component instanceof ComponentWrapperPanel) {
      this.label.add(AttributeModifier.replace("for", ((ComponentWrapperPanel) component).getComponentOutputId()));
      labelFor = true;
    }
    for (final Component comp : components) {
      if (comp instanceof LabeledWebMarkupContainer) {
        final LabeledWebMarkupContainer labeledComponent = (LabeledWebMarkupContainer) comp;
        if (labeledComponent.getLabel() == null) {
          labeledComponent.setLabel(new Model<String>(labelText));
        }
      } else if (component instanceof ComponentWrapperPanel) {
        final FormComponent<?> formComponent = ((ComponentWrapperPanel) component).getFormComponent();
        if (formComponent != null && formComponent.getLabel() == null) {
          formComponent.setLabel(new Model<String>(labelText));
        }
      }
    }
  }

  /**
   * @see org.apache.wicket.Component#onBeforeRender()
   */
  @Override
  protected void onBeforeRender()
  {
    if (labelFor == false && WebConfiguration.isDevelopmentMode() == true) {
      log.warn("No label set for field '"
          + labelText
          + "'. Please call setLabelFor(component) for this fieldset or supressLabelForWarning().");
    }
    super.onBeforeRender();
  }

  protected abstract T getThis();

  /**
   * Can be used by e. g. TimesheetPageSupport for storing objects, used by the supported form.
   *
   * @param storeObject the childField to set
   * @return this for chaining.
   */
  public T setStoreObject(final Object storeObject)
  {
    this.storeObject = storeObject;
    return getThis();
  }

  /**
   * Can be used by e. g. TimesheetPageSupport for storing objects, used by the supported form.
   *
   * @return the childField
   */
  public Object getStoreObject()
  {
    return storeObject;
  }

  /**
   * @return the fieldset
   */
  public WebMarkupContainer getFieldset()
  {
    return fieldset;
  }

}
