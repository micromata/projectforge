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

package org.projectforge.web.wicket.autocompletion;

import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.StringValue;
import org.projectforge.web.wicket.WicketUtils;

public abstract class PFAutoCompleteTextField<T> extends TextField<T>
{
  private static final long serialVersionUID = 3207038195316387588L;

  /** auto complete behavior attached to this textfield */
  private PFAutoCompleteBehavior<T> behavior;

  private AbstractDefaultAjaxBehavior deleteBehavior;

  private PFAutoCompleteSettings settings;

  protected boolean providesTooltip;

  private boolean tooltipRightAlignment;

  private IAutoCompleteRenderer<String> renderer;

  private static final String CONTENT = "delete";

  /**
   * @param id
   * @param model
   */
  protected PFAutoCompleteTextField(final String id, final IModel<T> model)
  {
    this(id, model, false);
  }

  /**
   * @param id
   * @param model
   * @param tooltipRightAlignment
   */
  protected PFAutoCompleteTextField(final String id, final IModel<T> model, final boolean tooltipRightAlignment)
  {
    this(id, model, PFAutoCompleteRenderer.INSTANCE, new PFAutoCompleteSettings(), tooltipRightAlignment);
  }

  protected PFAutoCompleteTextField(final String id, final IModel<T> model, final IAutoCompleteRenderer<String> renderer,
      final PFAutoCompleteSettings settings)
  {
    this(id, model, renderer, settings, false);
  }

  protected PFAutoCompleteTextField(final String id, final IModel<T> model, final IAutoCompleteRenderer<String> renderer,
      final PFAutoCompleteSettings settings, final boolean tooltipRightAlignment)
  {
    super(id, model);
    this.renderer = renderer;
    this.settings = settings;
    this.tooltipRightAlignment = tooltipRightAlignment;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    behavior = new PFAutoCompleteBehavior<T>(renderer, settings) {
      private static final long serialVersionUID = 1L;

      @Override
      protected List<T> getChoices(final String input)
      {
        return PFAutoCompleteTextField.this.getChoices(input);
      }

      @Override
      protected List<T> getFavorites()
      {
        return PFAutoCompleteTextField.this.getFavorites();
      }

      @Override
      protected List<String> getRecentUserInputs()
      {
        return PFAutoCompleteTextField.this.getRecentUserInputs();
      }

      @Override
      protected String formatValue(final T value)
      {
        return PFAutoCompleteTextField.this.formatValue(value);
      }

      @Override
      protected String formatLabel(final T value)
      {
        return PFAutoCompleteTextField.this.formatLabel(value);
      }
    };
    add(behavior);
    deleteBehavior = new AbstractDefaultAjaxBehavior() {
      private static final long serialVersionUID = 3014042180471042845L;

      @Override
      protected void respond(final AjaxRequestTarget target)
      {
        // Gather query params ?...&content=kssel
        final StringValue contentValue = RequestCycle.get().getRequest().getQueryParameters().getParameterValue(CONTENT);
        if (contentValue != null) {
          final String contentString = contentValue.toString();
          if (getForm() instanceof AutoCompleteIgnoreForm) {
            ((AutoCompleteIgnoreForm) getForm()).ignore(PFAutoCompleteTextField.this, contentString);
          } // else { just ignore }
        }
      }
    };
    add(deleteBehavior);
  }

  @Override
  public void renderHead(final IHeaderResponse response)
  {
    super.renderHead(response);
    response.render(OnDomReadyHeaderItem.forScript("$('#"
        + this.getMarkupId()
        + "').data('callback', '"
        + deleteBehavior.getCallbackUrl()
        + "');"));
  }

  @SuppressWarnings("serial")
  public PFAutoCompleteTextField<T> enableTooltips()
  {
    WicketUtils.addTooltip(this, new Model<String>() {
      @Override
      public String getObject()
      {
        return PFAutoCompleteTextField.this.getTooltip();
      }
    }, tooltipRightAlignment);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  protected void onComponentTag(final ComponentTag tag)
  {
    super.onComponentTag(tag);
    // disable browser's autocomplete
    tag.put("autocomplete", "off");
  }

  /**
   * Override this callback method that for returning favorite entries to show, if the user double clicks the empty input field. These
   * objects will be passed to the renderer to generate output.
   * 
   * @see AutoCompleteBehavior#getChoices(String)
   * 
   * @return null, if no favorites to show.
   */
  protected List<T> getFavorites()
  {
    return null;
  }

  /**
   * Override this callback method that for returning recent user inputs to show, if the user double clicks the empty input field.
   * 
   * @return null means: don't show recent user inputs.
   */
  protected List<String> getRecentUserInputs()
  {
    return null;
  }

  /**
   * Uses ObjectUtils.toString(Object) as default.
   * @param value
   * @return
   */
  protected String formatValue(final T value)
  {
    return ObjectUtils.toString(value);
  }

  /**
   * Only used if labelValue is set to true.
   * @param value
   * @return The label to show in the drop down choice. If not overloaded null is returned.
   */
  protected String formatLabel(final T value)
  {
    return null;
  }

  /**
   * Overwrite this method if a title attribute for the input text field should be set. Don't forget to call {@link #enableTooltips()}.
   * @return Tool-tip of the object currently represented by the input field or null.
   */
  protected String getTooltip()
  {
    return null;
  }

  /**
   * Callback method that should return a list of all possible assist choice objects. These objects will be passed to the renderer to
   * generate output.
   * 
   * @see AutoCompleteBehavior#getChoices(String)
   * 
   * @param input current input
   * @return list of all possible choice objects
   */
  protected abstract List<T> getChoices(String input);

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withAutoFill(boolean)
   */
  public PFAutoCompleteTextField<T> withAutoFill(final boolean autoFill)
  {
    settings.withAutoFill(autoFill);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withAutoSubmit(boolean)
   */
  public PFAutoCompleteTextField<T> withAutoSubmit(final boolean autoSubmit)
  {
    settings.withAutoSubmit(autoSubmit);
    return this;
  }

  /**
   * @return the settings
   */
  public PFAutoCompleteSettings getSettings()
  {
    return settings;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withFocus(boolean)
   */
  public PFAutoCompleteTextField<T> withFocus(final boolean hasFocus)
  {
    settings.withFocus(hasFocus);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withCacheLength(int)
   */
  public PFAutoCompleteTextField<T> withCacheLength(final int cacheLength)
  {
    settings.withCacheLength(cacheLength);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withDelay(int)
   */
  public PFAutoCompleteTextField<T> withDelay(final int delay)
  {
    settings.withDelay(delay);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withMatchCase(boolean)
   */
  public PFAutoCompleteTextField<T> withMatchCase(final boolean matchCase)
  {
    settings.withMatchCase(matchCase);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withMatchContains(boolean)
   */
  public PFAutoCompleteTextField<T> withMatchContains(final boolean matchContains)
  {
    settings.withMatchContains(matchContains);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withMatchSubset(boolean)
   */
  public PFAutoCompleteTextField<T> withMatchSubset(final boolean matchSubset)
  {
    settings.withMatchSubset(matchSubset);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withMaxItemsToShow(int)
   */
  public PFAutoCompleteTextField<T> withMaxItemsToShow(final int maxItemsToShow)
  {
    settings.withMaxItemsToShow(maxItemsToShow);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withMinChars(int)
   */
  public PFAutoCompleteTextField<T> withMinChars(final int minChars)
  {
    settings.withMinChars(minChars);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withMustMatch(boolean)
   */
  public PFAutoCompleteTextField<T> withMustMatch(final boolean mustMatch)
  {
    settings.withMustMatch(mustMatch);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withScroll(boolean)
   */
  public PFAutoCompleteTextField<T> withScroll(final boolean scroll)
  {
    settings.withScroll(scroll);
    return this;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteTextField<T> withScrollHeight(final int scrollHeight)
  {
    settings.withScrollHeight(scrollHeight);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withSelectFirst(boolean)
   */
  public PFAutoCompleteTextField<T> withSelectFirst(final boolean selectFirst)
  {
    settings.withSelectFirst(selectFirst);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withSelectOnly(boolean)
   */
  public PFAutoCompleteTextField<T> withSelectOnly(final boolean selectOnly)
  {
    settings.withSelectOnly(selectOnly);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withWidth(int)
   */
  public PFAutoCompleteTextField<T> withWidth(final int width)
  {
    settings.withWidth(width);
    return this;
  }

  /**
   * Fluent.
   * @see PFAutoCompleteSettings#withLabelValue(boolean)
   */
  public PFAutoCompleteTextField<T> withLabelValue(final boolean labelValue)
  {
    settings.withLabelValue(labelValue);
    return this;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteTextField<T> withDeletableItem(final boolean deletableItem)
  {
    settings.setDeletableItem(deletableItem);
    return this;
  }
}
