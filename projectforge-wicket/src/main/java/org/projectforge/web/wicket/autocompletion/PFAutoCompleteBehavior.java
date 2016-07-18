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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.util.string.Strings;
import org.projectforge.web.core.JsonBuilder;
import org.projectforge.web.wicket.WicketRenderHeadUtils;

public abstract class PFAutoCompleteBehavior<T> extends AbstractDefaultAjaxBehavior
{
  private static final long serialVersionUID = -6532710378025987377L;

  protected PFAutoCompleteSettings settings;

  protected IAutoCompleteRenderer<String> renderer;

  public PFAutoCompleteBehavior(final IAutoCompleteRenderer<String> renderer, final PFAutoCompleteSettings settings)
  {
    this.renderer = renderer;
    this.settings = settings;
  }

  /**
   * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#renderHead(org.apache.wicket.Component,
   *      org.apache.wicket.markup.html.IHeaderResponse)
   */
  @Override
  public void renderHead(final Component component, final IHeaderResponse response)
  {
    super.renderHead(component, response);
    WicketRenderHeadUtils.renderMainJavaScriptIncludes(response);
    response.render(JavaScriptReferenceHeaderItem.forUrl("scripts/jquery.wicket-autocomplete.js"));
    renderAutocompleteHead(response);
  }

  /**
   * Render autocomplete init javascript and other head contributions
   * 
   * @param response
   */
  private void renderAutocompleteHead(final IHeaderResponse response)
  {
    final String id = getComponent().getMarkupId();
    String indicatorId = findIndicatorId();
    if (Strings.isEmpty(indicatorId)) {
      indicatorId = "null";
    } else {
      indicatorId = "'" + indicatorId + "'";
    }
    final StringBuffer buf = new StringBuffer();
    buf.append("var favorite" + id + " = ");
    final List<T> favorites = getFavorites();
    final MyJsonBuilder builder = new MyJsonBuilder();
    if (favorites != null) {
      buf.append(builder.append(favorites).getAsString());
    } else {
      buf.append(builder.append(getRecentUserInputs()).getAsString());
    }
    buf.append(";").append("var z = $(\"#").append(id).append("\");\n").append("z.autocomplete(\"").append(getCallbackUrl()).append("\",{");
    boolean first = true;
    for (final String setting : getSettingsJS()) {
      if (first == true)
        first = false;
      else buf.append(", ");
      buf.append(setting);
    }
    if (first == true)
      first = false;
    else buf.append(", ");
    buf.append("favoriteEntries:favorite" + id);
    buf.append("});");
    if (settings.isHasFocus() == true) {
      buf.append("\nz.focus();");
    }
    final String initJS = buf.toString();
    // String initJS = String.format("new Wicket.AutoComplete('%s','%s',%s,%s);", id, getCallbackUrl(), constructSettingsJS(), indicatorId);
    response.render(OnDomReadyHeaderItem.forScript(initJS));
  }

  protected final List<String> getSettingsJS()
  {
    final List<String> result = new ArrayList<String>();
    addSetting(result, "matchContains", settings.isMatchContains());
    addSetting(result, "minChars", settings.getMinChars());
    addSetting(result, "delay", settings.getDelay());
    addSetting(result, "matchCase", settings.isMatchCase());
    addSetting(result, "matchSubset", settings.isMatchSubset());
    addSetting(result, "cacheLength", settings.getCacheLength());
    addSetting(result, "mustMatch", settings.isMustMatch());
    addSetting(result, "selectFirst", settings.isSelectFirst());
    addSetting(result, "selectOnly", settings.isSelectOnly());
    addSetting(result, "maxItemsToShow", settings.getMaxItemsToShow());
    addSetting(result, "autoFill", settings.isAutoFill());
    addSetting(result, "autoSubmit", settings.isAutoSubmit());
    addSetting(result, "scroll", settings.isScroll());
    addSetting(result, "scrollHeight", settings.getScrollHeight());
    addSetting(result, "width", settings.getWidth());
    addSetting(result, "deletableItem", settings.isDeletableItem());
    if (settings.isLabelValue() == true) {
      addSetting(result, "labelValue", settings.isLabelValue());
    }
    return result;
  }

  private final void addSetting(final List<String> result, final String name, final Boolean value)
  {
    if (value == null) {
      return;
    }
    result.add(name + ":" + ((value == true) ? "1" : "0"));
  }

  private final void addSetting(final List<String> result, final String name, final Integer value)
  {
    if (value == null) {
      return;
    }
    result.add(name + ":" + value);
  }

  /**
   * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#onBind()
   */
  @Override
  protected void onBind()
  {
    // add empty AbstractDefaultAjaxBehavior to the component, to force
    // rendering wicket-ajax.js reference if no other ajax behavior is on
    // page
    getComponent().add(new AbstractDefaultAjaxBehavior() {
      private static final long serialVersionUID = 1L;

      @Override
      protected void respond(final AjaxRequestTarget target)
      {
      }
    });
  }

  /**
   * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#respond(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  protected void respond(final AjaxRequestTarget target)
  {
    final RequestCycle requestCycle = RequestCycle.get();
    final org.apache.wicket.util.string.StringValue val = requestCycle.getRequest().getQueryParameters().getParameterValue("q");
    onRequest(val != null ? val.toString() : null, requestCycle);
  }

  protected final void onRequest(final String val, final RequestCycle requestCycle)
  {
    // final PageParameters pageParameters = new PageParameters(requestCycle.getRequest().getParameterMap());
    final List<T> choices = getChoices(val);
    final MyJsonBuilder builder = new MyJsonBuilder();
    final String json = builder.append(choices).getAsString();
    requestCycle.scheduleRequestHandlerAfterCurrent(new TextRequestHandler("application/json", "utf-8", json));

    /*
     * IRequestTarget target = new IRequestTarget() {
     * 
     * public void respond(RequestCycle requestCycle) {
     * 
     * WebResponse r = (WebResponse) requestCycle.getResponse(); // Determine encoding final String encoding =
     * Application.get().getRequestCycleSettings().getResponseRequestEncoding(); r.setCharacterEncoding(encoding);
     * r.setContentType("application/json"); // Make sure it is not cached by a r.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
     * r.setHeader("Cache-Control", "no-cache, must-revalidate"); r.setHeader("Pragma", "no-cache");
     * 
     * final List<T> choices = getChoices(val); renderer.renderHeader(r); renderer.render(JsonBuilder.buildRows(false, choices), r, val);
     * renderer.renderFooter(r); }
     * 
     * public void detach(RequestCycle requestCycle) { } }; requestCycle.setRequestTarget(target);
     */
  }

  /**
   * Callback method that should return an iterator over all possible choice objects. These objects will be passed to the renderer to
   * generate output. Usually it is enough to return an iterator over strings.
   * 
   * @param input current input
   * @return iterator over all possible choice objects
   */
  protected abstract List<T> getChoices(String input);

  /**
   * Callback method that should return a list of all possible default choice objects to show, if the user double clicks the empty input
   * field. These objects will be passed to the renderer to generate output. Usually it is enough to return an iterator over strings.
   */
  protected abstract List<T> getFavorites();

  /**
   * Callback method that should return a list of all recent user inputs in the text input field. They will be shown, if the user double
   * clicks the empty input field. These objects will be passed to the renderer to generate output. Usually it is enough to return an
   * iterator over strings. <br/>
   * Please note: Please, use only getFavorites() OR getRecentUserInputs()!
   */
  protected abstract List<String> getRecentUserInputs();

  /**
   * Used for formatting the values.
   */
  protected abstract String formatValue(T value);

  /**
   * Used for formatting the labels if labelValue is set to true.
   * @return null at default (if not overload).
   */
  protected String formatLabel(final T value)
  {
    return null;
  }

  private class MyJsonBuilder extends JsonBuilder
  {
    private MyJsonBuilder()
    {
      setEscapeHtml(true);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String formatValue(final Object obj)
    {
      if (obj instanceof String) {
        return obj.toString();
      } else {
        return PFAutoCompleteBehavior.this.formatValue((T) obj);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object transform(final Object obj)
    {
      if (settings.isLabelValue() == true) {
        final Object[] oa = new Object[2];
        if (obj instanceof String) {
          oa[0] = obj;
          oa[1] = obj;
        } else {
          oa[0] = PFAutoCompleteBehavior.this.formatLabel((T) obj);
          oa[1] = PFAutoCompleteBehavior.this.formatValue((T) obj);
        }
        return oa;
      } else {
        return obj;
      }
    }
  };
}
