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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.RequestUtils;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.UrlUtils;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.projectforge.Const;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.common.BeanHelper;
import org.projectforge.common.DateFormatType;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.time.TimePeriod;
import org.projectforge.framework.utils.ClassHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.URLHelper;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.mobile.AbstractSecuredMobilePage;
import org.projectforge.web.mobile.MenuMobilePage;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

public class WicketUtils
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WicketUtils.class);

  private static String APPLICATION_CONTEXT = "/ProjectForge";

  public static String getContextPath()
  {
    return APPLICATION_CONTEXT;
  }

  static void setContextPath(final String contextPath)
  {
    APPLICATION_CONTEXT = contextPath;
  }

  public static HttpServletRequest getHttpServletRequest(final Request request)
  {
    return (HttpServletRequest) request.getContainerRequest();
  }

  public static HttpServletResponse getHttpServletResponse(final Response response)
  {
    return (HttpServletResponse) response.getContainerResponse();
  }

  public static boolean contains(final PageParameters parameters, final String name)
  {
    final StringValue sval = parameters.get(name);
    if (sval == null) {
      return false;
    } else {
      return sval.isNull() == false;
    }
  }

  public static String getAsString(final PageParameters parameters, final String name)
  {
    final StringValue sval = parameters.get(name);
    if (sval == null || sval.isNull() == true) {
      return null;
    } else {
      return sval.toString();
    }
  }

  public static Integer getAsInteger(final PageParameters parameters, final String name)
  {
    final StringValue sval = parameters.get(name);
    if (sval == null || sval.isNull() == true) {
      return null;
    } else {
      return sval.toInteger();
    }
  }

  public static int getAsInt(final PageParameters parameters, final String name, final int defaultValue)
  {
    final StringValue sval = parameters.get(name);
    if (sval == null || sval.isNull() == true) {
      return defaultValue;
    } else {
      return sval.toInt();
    }
  }

  public static Long getAsLong(final PageParameters parameters, final String name)
  {
    final StringValue sval = parameters.get(name);
    if (sval == null || sval.isNull() == true) {
      return null;
    } else {
      return sval.toLong();
    }
  }

  public static Boolean getAsBooleanObject(final PageParameters parameters, final String name)
  {
    final StringValue sval = parameters.get(name);
    if (sval == null || sval.isNull() == true) {
      return null;
    } else {
      return sval.toBooleanObject();
    }
  }

  public static boolean getAsBoolean(final PageParameters parameters, final String name)
  {
    final StringValue sval = parameters.get(name);
    if (sval == null || sval.isNull() == true) {
      return false;
    } else {
      return sval.toBoolean();
    }
  }

  public static Object getAsObject(final PageParameters parameters, final String name, final Class<?> type)
  {
    final StringValue sval = parameters.get(name);
    if (sval == null || sval.isNull() == true) {
      return null;
    } else {
      return sval.to(type);
    }
  }

  /**
   * Renders &lt;link type="image/x-icon" rel="shortcut icon" href="favicon.ico" /&gt;
   *
   * @param favicon The favicon file, e. g. "/ProjectForge/favicon.ico".
   */
  public static String getCssForFavicon(final String favicon)
  {
    return "<link type=\"image/x-icon\" rel=\"shortcut icon\" href=\"" + favicon + "\" />";
  }

  /**
   * Prepends APPLICATION_CONTEXT if url starts with '/', otherwise url is returned unchanged.
   *
   * @param url
   */
  public static final String getAbsoluteUrl(final String url)
  {
    if (url.startsWith("/") == true) {
      return APPLICATION_CONTEXT + url;
    }
    return url;
  }

  /**
   * Get the url for the given path (without image path). Later, the path of the images is changeable.
   *
   * @param requestCycle Needed to encode url.
   * @param subpath
   * @return
   */
  public static String getImageUrl(final RequestCycle requestCycle, final String path)
  {
    return getUrl(requestCycle, path, true);
  }

  /**
   * Should be c:url equivalent, but isn't yet (works for now).
   *
   * @param requestCycle Needed to encode url.
   * @param path
   * @param encodeUrl
   * @return path itself if not starts with '/' otherwise "/ProjectForge" + path with session id and params.
   */
  public static String getUrl(final RequestCycle requestCycle, final String path, final boolean encodeUrl)
  {
    String url = UrlUtils.rewriteToContextRelative(path, requestCycle);
    if (encodeUrl == true) {
      url = requestCycle.getResponse().encodeURL(url);
    }
    return url;
  }

  /**
   * Works for Wicket and non Wicket calling pages. For non Wicket callers the pageClass must be bookmarked in Wicket
   * application.
   *
   * @param pageClass
   * @param Optional  list of params in tupel form: key, value, key, value...
   */
  public static String getBookmarkablePageUrl(final Class<? extends Page> pageClass, final String... params)
  {
    final RequestCycle requestCylce = RequestCycle.get();
    if (requestCylce != null) {
      final PageParameters pageParameter = getPageParameters(params);
      return requestCylce.urlFor(pageClass, pageParameter).toString();
    } else {
      // RequestCycle.get().urlFor(pageClass, pageParameter).toString() can't be used for non wicket requests!
      final String alias = WicketApplication.getBookmarkableMountPath(pageClass);
      if (alias == null) {
        log.error("Given page class is not mounted. Please mount class in WicketApplication: " + pageClass);
        return getDefaultPageUrl();
      }
      if (params == null) {
        return Const.WICKET_APPLICATION_PATH + alias;
      }
      final StringBuffer buf = new StringBuffer();
      buf.append(Const.WICKET_APPLICATION_PATH).append(alias);
      try {
        for (int i = 0; i < params.length; i += 2) {
          if (i == 0) {
            buf.append("?");
          } else {
            buf.append("&");
          }
          buf.append(URLEncoder.encode(params[i], "UTF-8")).append("=");
          if (i + 1 < params.length) {
            buf.append(URLEncoder.encode(params[i + 1], "UTF-8"));
          }
        }
      } catch (final UnsupportedEncodingException ex) {
        log.error(ex.getMessage(), ex);
      }
      return buf.toString();
    }
  }

  /**
   * Tuples of parameters converted to Wicket parameters.
   *
   * @param params
   * @return
   */
  public static PageParameters getPageParameters(final String[] params)
  {
    final PageParameters pageParameters = new PageParameters();
    if (params != null) {
      for (int i = 0; i < params.length; i += 2) {
        if (i + 1 < params.length) {
          pageParameters.add(params[i], params[i + 1]);
        } else {
          pageParameters.add(params[i], null);
        }
      }
    }
    return pageParameters;
  }

  /**
   * @param relativePagePath
   * @return
   * @see RequestUtils#toAbsolutePath(String, String)
   * @see URLHelper#removeJSessionId(String)
   */
  public final static String toAbsolutePath(final String requestUrl, final String relativePagePath)
  {
    final String absoluteUrl = RequestUtils.toAbsolutePath(requestUrl, relativePagePath);
    return URLHelper.removeJSessionId(absoluteUrl);
  }

  /**
   * @param id
   * @return new PageParameters containing the given id as page parameter.
   */
  public final static PageParameters getEditPageParameters(final Integer id)
  {
    return new PageParameters().set(AbstractEditPage.PARAMETER_KEY_ID, id);
  }

  /**
   * @return Default page of ProjectForge. Currently {@link WicketApplication#DEFAULT_PAGE} is the default page (e. g.
   * to redirect after login if no forward url is specified).
   */
  public static String getDefaultPageUrl()
  {
    return getBookmarkablePageUrl(getDefaultPage());
  }

  /**
   * @return Default page of ProjectForge. Currently {@link WicketApplication#DEFAULT_PAGE} is the default page (e. g.
   * to redirect after cancel if no other return page is specified).
   */
  public static Class<? extends WebPage> getDefaultPage()
  {
    return WicketApplication.DEFAULT_PAGE;
  }

  /**
   * @return MenuMobilePage.class.
   */
  public static Class<? extends AbstractSecuredMobilePage> getDefaultMobilePage()
  {
    return MenuMobilePage.class;
  }

  /**
   * If value is null or value is default value then nothing is done. Otherwise the given value is added as page
   * parameter under the given key. Dates and TimePeriods are converted and can be gotten by
   * {@link #getPageParameter(PageParameters, String, Class)}.
   *
   * @param pageParameters
   * @param key
   * @param value
   * @see ClassHelper#isDefaultType(Class, Object)
   */
  public static void putPageParameter(final PageParameters pageParameters, final String key, final Object value)
  {
    if (value == null) {
      // Do not put null values to page parameters.
    } else if (ClassHelper.isDefaultType(value.getClass(), value)) {
      // Do not put default values to page parameters.
    } else if (value instanceof Date) {
      addOrReplaceParameter(pageParameters, key, ((Date) value).getTime());
    } else if (value instanceof TimePeriod) {
      addOrReplaceParameter(pageParameters, key, ((TimePeriod) value).getFromDate().getTime()
          + "-"
          + ((TimePeriod) value).getToDate().getTime());
    } else {
      addOrReplaceParameter(pageParameters, key, value);
    }
  }

  public static void addOrReplaceParameter(final PageParameters pageParameters, final String key, final Object value)
  {
    if (pageParameters.get(key).isNull() == true) {
      pageParameters.add(key, value);
    } else {
      pageParameters.set(key, value);
    }
  }

  public static void putPageParameters(final ISelectCallerPage callerPage, final Object dataObject,
      final Object filterObject,
      final PageParameters pageParameters, final String[] bookmarkableProperties)
  {
    if (bookmarkableProperties == null) {
      return;
    }
    // final String pre = prefix != null ? prefix + "." : "";
    for (final String propertyString : bookmarkableProperties) {
      final InitialPageParameterHolder paramHolder = new InitialPageParameterHolder(propertyString);
      final Object bean;
      if (paramHolder.isFilterParameter() == true) {
        bean = filterObject;
      } else {
        bean = dataObject;
      }
      try {
        final Object value = BeanHelper.getProperty(bean, paramHolder.property);
        WicketUtils.putPageParameter(pageParameters, paramHolder.prefix + paramHolder.alias, value);
      } catch (final Exception ex) {
        log.warn("Couldn't put page parameter '" + paramHolder.property + "' of bean '" + bean
            + "'. Ignoring this parameter.");
      }
    }
  }

  /**
   * @param pageParameters
   * @param key
   * @param objectType
   * @see #putPageParameter(PageParameters, String, Object)
   */
  public static Object getPageParameter(final PageParameters pageParameters, final String key,
      final Class<?> objectType)
  {
    if (objectType.isAssignableFrom(Date.class) == true) {
      final StringValue sval = pageParameters.get(key);
      if (sval.isNull() == true) {
        return null;
      }
      return new Date(sval.toLongObject());
    } else if (objectType.isAssignableFrom(Boolean.class) == true) {
      return pageParameters.get(key).toBooleanObject();
    } else if (objectType.isPrimitive() == true) {
      if (Boolean.TYPE.equals(objectType)) {
        return pageParameters.get(key).toBooleanObject();
      } else if (Integer.TYPE.equals(objectType) == true) {
        return pageParameters.get(key).toInteger();
      } else if (Long.TYPE.equals(objectType) == true) {
        return pageParameters.get(key).toLong();
      } else if (Float.TYPE.equals(objectType) == true) {
        return new Float(pageParameters.get(key).toDouble());
      } else if (Double.TYPE.equals(objectType) == true) {
        return pageParameters.get(key).toDouble();
      } else if (Character.TYPE.equals(objectType) == true) {
        return pageParameters.get(key).toChar();
      } else {
        log.warn(
            "Primitive objectType '" + objectType + "' not yet implemented. Parameter type '" + key + "' is ignored.");
      }
    } else if (Enum.class.isAssignableFrom(objectType) == true) {
      final StringValue sval = pageParameters.get(key);
      if (sval.isNull() == true) {
        return null;
      }
      final String sValue = sval.toString();
      @SuppressWarnings({ "unchecked", "rawtypes" })
      final Enum<?> en = Enum.valueOf((Class<Enum>) objectType, sValue);
      return en;
    } else if (objectType.isAssignableFrom(Integer.class) == true) {
      final StringValue sval = pageParameters.get(key);
      if (sval.isNull() == true) {
        return null;
      }
      return sval.toInteger();
    } else if (objectType.isAssignableFrom(String.class) == true) {
      return pageParameters.get(key).toString();
    } else if (objectType.isAssignableFrom(TimePeriod.class) == true) {
      final String sValue = pageParameters.get(key).toString();
      if (sValue == null) {
        return null;
      }
      final int pos = sValue.indexOf('-');
      if (pos < 0) {
        log.warn("PageParameter of type TimePeriod '" + objectType.getName() + "' in wrong format: " + sValue);
        return null;
      }
      final Long fromTime = NumberHelper.parseLong(sValue.substring(0, pos));
      final Long toTime = pos < sValue.length() - 1 ? NumberHelper.parseLong(sValue.substring(pos + 1)) : null;
      return new TimePeriod(fromTime != null ? new Date(fromTime) : null, toTime != null ? new Date(toTime) : null);
    } else {
      log.error("PageParameter of type '" + objectType.getName() + "' not yet supported.");
    }
    return null;
  }

  public static boolean hasParameter(final PageParameters parameters, final String name)
  {
    final StringValue sval = parameters.get(name);
    return sval != null && sval.isNull() == false;
  }

  /**
   * At least one parameter should be given for setting the fill the bean with all book-markable properties (absent
   * properties will be set to zero). If the given bean is an instance of {@link ISelectCallerPage} then the
   * select/unselect methods are used, otherwise the properties will set directly of the given bean.
   *
   * @param bean
   * @param parameters
   * @param prefix
   * @param bookmarkableProperties
   */
  public static void evaluatePageParameters(final ISelectCallerPage callerPage, final Object dataObject,
      final Object filter,
      final PageParameters parameters, final String[] bookmarkableProperties)
  {
    if (bookmarkableProperties == null) {
      return;
    }
    // First check if any parameter is given:
    boolean useParameters = false;
    for (final String str : bookmarkableProperties) {
      final InitialPageParameterHolder paramHolder = new InitialPageParameterHolder(str);
      if (hasParameter(parameters, paramHolder.prefix + paramHolder.property) == true
          || hasParameter(parameters, paramHolder.prefix + paramHolder.alias) == true) {
        useParameters = true;
        break;
      }
    }
    if (useParameters == false) {
      // No book-markable parameters found.
      return;
    }
    for (final String str : bookmarkableProperties) {
      final InitialPageParameterHolder paramHolder = new InitialPageParameterHolder(str);
      String key = null;
      if (hasParameter(parameters, paramHolder.prefix + paramHolder.property) == true) {
        key = paramHolder.property;
      } else if (hasParameter(parameters, paramHolder.prefix + paramHolder.alias) == true) {
        key = paramHolder.alias;
      }
      if (paramHolder.isCallerPageParameter() == true) {
        if (callerPage == null) {
          log.warn("PageParameter '" + str + "' ignored, ISelectCallerPage isn't given.");
        } else if (key == null) {
          callerPage.unselect(paramHolder.property);
        } else {
          callerPage.select(paramHolder.property, parameters.get(paramHolder.prefix + key).toString());
        }
      } else {
        try {
          final Object bean;
          if (paramHolder.isFilterParameter() == true) {
            // Use filter object
            bean = filter;
            if (bean == null) {
              log.warn("PageParameter '" + str + "' ignored, filter isn't given.");
              continue;
            }
          } else {
            bean = dataObject;
            if (bean == null) {
              log.warn("PageParameter '" + str + "' ignored, dataObject isn't given.");
              continue;
            }
          }
          final Method method = BeanHelper.determineGetter(bean.getClass(), paramHolder.property);
          if (key == null) {
            BeanHelper.setProperty(bean, paramHolder.property, ClassHelper.getDefaultType(method.getReturnType()));
          } else {
            final Object value = WicketUtils.getPageParameter(parameters, paramHolder.prefix + key,
                method.getReturnType());
            BeanHelper.setProperty(bean, paramHolder.property, value);
          }
        } catch (final Exception ex) {
          log.warn("Property '" + key + "' not found. Ignoring URL parameter.");
        }
      }
    }
  }

  /**
   * Adds onclick attribute with "javascript:rowClick(this);".
   *
   * @param row Html tr element.
   */
  public static void addRowClick(final Component row)
  {
    row.add(AttributeModifier.replace("onclick", "javascript:rowClick(this);"));
    // add marker css class for contextMenu javaScript
    row.add(new AttributeAppender("class", Model.of("withContextMenu"), " "));
  }

  /**
   * @return
   */
  public static ContextImage getInvisibleDummyImage(final String id, final RequestCycle requestCylce)
  {
    final ContextImage image = new ContextImage(id, WicketUtils.getImageUrl(requestCylce, WebConstants.IMAGE_SPACER));
    image.setVisible(false);
    return image;
  }

  public static Component getInvisibleComponent(final String id)
  {
    return new Label(id).setVisible(false);
  }

  /**
   * @param label
   * @param unit
   * @return label [<unit>] (label with appended unit in brackets).
   */
  public static String getLabelWithUnit(final String label, final String unit)
  {
    return label + " [" + unit + "]";
  }

  /**
   * Uses "jiraSupportTooltipImage" as component id.
   *
   * @param parent only needed for localization
   * @param id
   * @return IconPanel which is invisible if JIRA isn't configured.
   */
  public static IconPanel getJIRASupportTooltipIcon(final Component parent, final String id)
  {
    final IconPanel icon = new IconPanel(id, IconType.JIRA_SUPPORT,
        Model.of(parent.getString("tooltip.jiraSupport.field.title")),
        Model.of(parent.getString("tooltip.jiraSupport.field.content")));
    if (isJIRAConfigured() == false) {
      icon.setVisible(false);
    }
    return icon;
  }

  /**
   * Uses "jiraSupportTooltipImage" as component id. Please use {@link FieldsetPanel#addJIRASupportHelpIcon()} instead
   * of this method if possible.
   *
   * @param fieldset needed for localization and for getting new child id.
   * @return IconPanel which is invisible if JIRA isn't configured.
   */
  public static IconPanel getJIRASupportTooltipIcon(final FieldsetPanel fieldset)
  {
    return getJIRASupportTooltipIcon(fieldset, fieldset.newIconChildId());
  }

  /**
   */
  public static IconPanel getAlertTooltipIcon(final FieldsetPanel fieldset, final String tooltip)
  {
    return getAlertTooltipIcon(fieldset, null, Model.of(tooltip));
  }

  /**
   */
  public static IconPanel getAlertTooltipIcon(final FieldsetPanel fieldset, final IModel<String> title,
      final IModel<String> tooltip)
  {
    final IconPanel icon = new IconPanel(fieldset.newIconChildId(), IconType.ALERT, title, tooltip);
    return icon;
  }

  public static final boolean isJIRAConfigured()
  {
    return ConfigXml.getInstance().isJIRAConfigured();
  }

  /**
   * Add JavaScript function showDeleteEntryQuestionDialog(). Depending on BaseDao.isHistorizable() a delete or
   * mark-as-deleted question will be displayed. Usage in markup: &lt;script
   * wicket:id="showDeleteEntryQuestionDialog"&gt;[...]&lt;/script&gt;
   *
   * @param parent
   * @param dao
   */
  public static void addShowDeleteRowQuestionDialog(final MarkupContainer parent, final BaseDao<?> dao)
  {
    final StringBuffer buf = new StringBuffer();
    buf.append("function showDeleteEntryQuestionDialog() {\n").append("  return window.confirm('");
    if (dao.isHistorizable() == true) {
      buf.append(parent.getString("question.markAsDeletedQuestion"));
    } else {
      buf.append(parent.getString("question.deleteQuestion"));
    }
    buf.append("');\n}\n");
    parent.add(new Label("showDeleteEntryQuestionDialog", buf.toString()).setEscapeModelStrings(false).add(
        AttributeModifier.replace("type", "text/javascript")));
  }

  /**
   * Sets the html attribute placeholder.
   *
   * @param component
   * @param value
   */
  public static void setPlaceHolderAttribute(Component component, final String value)
  {
    if (component instanceof ComponentWrapperPanel) {
      component = ((ComponentWrapperPanel) component).getFormComponent();
    }
    component.add(AttributeModifier.replace("placeholder", value));
  }

  /**
   * @param parent    Only for i18n needed.
   * @param startTime Start time or null.
   * @param stopTime  Stop time or null.
   * @return The weeks of year range for the given start an stop time.
   */
  public static String getCalendarWeeks(final MarkupContainer parent, final Date startTime, final Date stopTime)
  {
    int fromWeek = -1;
    int toWeek = -1;
    if (startTime != null) {
      fromWeek = DateHelper.getWeekOfYear(startTime);
    }
    if (stopTime != null) {
      toWeek = DateHelper.getWeekOfYear(stopTime);
    }
    if (fromWeek < 0 && toWeek < 0) {
      return "";
    }
    final StringBuffer buf = new StringBuffer();
    buf.append(parent.getString("calendar.weekOfYearShortLabel")).append(" ");
    if (fromWeek >= 0) {
      buf.append(StringHelper.format2DigitNumber(fromWeek));
      if (toWeek == -1) {
        buf.append("-");
      } else if (toWeek != fromWeek) {
        buf.append("-").append(StringHelper.format2DigitNumber(toWeek));
      }
    } else {
      buf.append("-").append(StringHelper.format2DigitNumber(toWeek));
    }
    return buf.toString();
  }

  /**
   * @param date
   */
  public static String getUTCDate(final Date date)
  {
    if (date == null) {
      return "";
    }
    final DateHolder dh = new DateHolder(date);
    return DateHelper.TECHNICAL_ISO_UTC.get().format(dh.getDate());
  }

  /**
   * @param label Label as prefix
   * @param date
   * @return <label>: <date>
   */
  public static String getUTCDate(final String label, final Date date)
  {
    if (date == null) {
      return label + ":";
    }
    final DateHolder dh = new DateHolder(date);
    return label + ": " + DateHelper.TECHNICAL_ISO_UTC.get().format(dh.getDate());
  }

  /**
   * @param startTime Start time or null.
   * @param stopTime  Stop time or null.
   */
  public static String getUTCDates(final Date startTime, final Date stopTime)
  {
    final StringBuffer buf = new StringBuffer();
    final DateHolder start = startTime != null ? new DateHolder(startTime) : null;
    final DateHolder stop = stopTime != null ? new DateHolder(stopTime) : null;
    if (start != null) {
      buf.append(DateHelper.TECHNICAL_ISO_UTC.get().format(start.getDate()));
      if (stop != null) {
        buf.append(" - ");
      }
    }
    if (stop != null) {
      buf.append(DateHelper.TECHNICAL_ISO_UTC.get().format(stop.getDate()));
    }
    return buf.toString();
  }

  public static LabelValueChoiceRenderer<Long> getDatumChoiceRenderer(final int lastNDays)
  {
    final LabelValueChoiceRenderer<Long> datumChoiceRenderer = new LabelValueChoiceRenderer<Long>();
    for (int i = 0; i > -lastNDays; i--) {
      final DayHolder day = new DayHolder();
      day.add(Calendar.DAY_OF_YEAR, i);
      datumChoiceRenderer.addValue(day.getSQLDate().getTime(),
          DateTimeFormatter.instance().getFormattedDate(day.getSQLDate(),
              DateFormats.getFormatString(DateFormatType.DATE)));
    }
    return datumChoiceRenderer;
  }

  public static void append(final Component component, final RowCssClass... rowCssClasses)
  {
    for (final RowCssClass rowCssClass : rowCssClasses) {
      component.add(AttributeModifier.append("class", rowCssClass.getCssClass()));
    }
  }

  /**
   * Adds a SimpleAttributeModifier("title", ...) to the given component.
   *
   * @param component
   * @param title
   * @param text
   * @see #createTooltip(String, String)
   * @see #setStyleHasTooltip(Component)
   */
  public static Component addTooltip(final Component component, final String title, final String text)
  {
    return addTooltip(component, title, text, true);
  }

  /**
   * Adds a SimpleAttributeModifier("title", ...) to the given component.
   *
   * @param component
   * @param title
   * @param text
   * @see #createTooltip(String, String)
   * @see #setStyleHasTooltip(Component)
   */
  public static Component addTooltip(final Component component, final String title, final String text,
      final boolean rightAlignment)
  {
    return addTooltip(component, Model.of(title), Model.of(text), rightAlignment);
  }

  /**
   * Adds a SimpleAttributeModifier("title", ...) to the given component.
   *
   * @param component
   * @param text
   * @see #createTooltip(String, String)
   * @see #setStyleHasTooltip(Component)
   */
  public static Component addTooltip(final Component component, final String text)
  {
    return addTooltip(component, text, true);
  }

  /**
   * Adds a SimpleAttributeModifier("title", ...) to the given component.
   *
   * @param component
   * @param text
   * @param rightAlignment If false (default is true) the tooltip will be aligned at the bottom.
   * @see #createTooltip(String, String)
   * @see #setStyleHasTooltip(Component)
   */
  public static Component addTooltip(final Component component, final String text, final boolean rightAlignment)
  {
    return addTooltip(component, null, Model.of(text), rightAlignment);
  }

  /**
   * Adds a SimpleAttributeModifier("title", ...) to the given component. Does not modify the given tool tip text!
   *
   * @param component
   * @param text
   */
  public static Component addTooltip(final Component component, final IModel<String> text)
  {
    return addTooltip(component, text, true);
  }

  /**
   * Adds a SimpleAttributeModifier("title", ...) to the given component. Does not modify the given tool tip text!
   *
   * @param component
   * @param text
   */
  public static Component addTooltip(final Component component, final IModel<String> text, final boolean rightAlignment)
  {
    return addTooltip(component, null, text, rightAlignment);
  }

  /**
   * Adds a SimpleAttributeModifier("title", ...) to the given component. Does not modify the given tool tip text!
   *
   * @param component
   * @param title
   * @param text      If the string contains "\n" characters then html=true and &lt;br/&gt; are used.
   */
  public static Component addTooltip(final Component component, final IModel<String> title, final IModel<String> text)
  {
    return addTooltip(component, title, text, true);
  }

  /**
   * Adds a SimpleAttributeModifier("title", ...) to the given component. Does not modify the given tool tip text!
   *
   * @param component
   * @param title
   * @param text           If the string contains "\n" characters then html=true and &lt;br/&gt; are used.
   * @param rightAlignment If false (default is true) the tooltip will be aligned at the bottom.
   */
  public static Component addTooltip(final Component component, final IModel<String> title, final IModel<String> text,
      final boolean rightAlignment)
  {
    @SuppressWarnings("serial")
    final IModel<String> myModel = new Model<String>()
    {
      /**
       * @see org.apache.wicket.model.Model#getObject()
       */
      @Override
      public String getObject()
      {
        if (text != null && text.getObject() != null) {
          return HtmlHelper.escapeHtml(text.getObject(), true);
        }
        return null;
      }
    };
    component.add(AttributeModifier.replace("data-html", true));
    if (title != null && title.getObject() != null) {
      component.add(AttributeModifier.replace("rel", rightAlignment ? "mypopup-right" : "mypopup"));
      component.add(AttributeModifier.replace("data-original-title", title));
      component.add(AttributeModifier.replace("data-content", myModel));
    } else {
      component.add(AttributeModifier.replace("rel", rightAlignment ? "mytooltip-right" : "mytooltip"));
      component.add(AttributeModifier.replace("title", myModel));
    }
    return component;
  }

  /**
   * You need to use {@link AjaxEditableLabel#getLabel()}.
   *
   * @param label
   * @return
   */
  public static Component addEditableLabelDefaultTooltip(final Component label)
  {
    return addTooltip(label, label.getString("form.ajaxEditableLabel.tooltip"));
  }

  public static Component setWarningTooltip(final Component component)
  {
    component.add(AttributeModifier.append("class", "warning"));
    return component;
  }

  /**
   * Sets readonly="readonly" and "readOnly" as class.
   *
   * @param component
   * @return This for chaining.
   */
  public static FormComponent<?> setReadonly(final FormComponent<?> component)
  {
    component.add(AttributeModifier.append("class", "readonly"));
    component.add(AttributeModifier.replace("readonly", "readonly"));
    return component;
  }

  /**
   * Sets attribute size (only for TextFields) and style="length: width"; The width value is size + 0.5 em and for drop
   * down choices size + 2em;
   *
   * @param component
   * @param size
   * @return This for chaining.
   */
  public static FormComponent<?> setSize(final FormComponent<?> component, final int size)
  {
    return setSize(component, size, true);
  }

  /**
   * Sets attribute size (only for TextFields) and style="length: width"; The width value is size + 0.5 em and for drop
   * down choices size + 2em;
   *
   * @param component
   * @param size
   * @param important If true then "!important" is appended to the width style (true is default).
   * @return This for chaining.
   */
  public static FormComponent<?> setSize(final FormComponent<?> component, final int size, final boolean important)
  {
    if (component instanceof TextField) {
      component.add(AttributeModifier.replace("size", String.valueOf(size)));
    }
    final StringBuffer buf = new StringBuffer(20);
    buf.append("width: ");
    if (component instanceof DropDownChoice) {
      buf.append(size + 2).append("em");
    } else {
      buf.append(size).append(".5em");
    }
    if (important == true) {
      buf.append(" !important;");
    }
    buf.append(";");
    component.add(AttributeModifier.append("style", buf.toString()));
    return component;
  }

  /**
   * Sets attribute size (only for TextFields) and style="width: x%";
   *
   * @param component
   * @param size
   * @return This for chaining.
   */
  public static FormComponent<?> setPercentSize(final FormComponent<?> component, final int size)
  {
    component.add(AttributeModifier.append("style", "width: " + size + "%;"));
    return component;
  }

  /**
   * Sets attribute font-size: style="font-size: 1.1em;";
   *
   * @param component
   * @param size
   * @return This for chaining.
   */
  public static Component setFontSizeLarge(final Component component)
  {
    component.add(AttributeModifier.append("style", "font-size: 1.5em;"));
    return component;
  }

  public static Component setStrong(final Component component)
  {
    component.add(AttributeModifier.append("style", "font-weight: bold;"));
    return component;
  }

  /**
   * Sets attribute style="height: <height>ex;"
   *
   * @param component
   * @param size
   * @return This for chaining.
   */
  public static FormComponent<?> setHeight(final FormComponent<?> component, final int height)
  {
    component.add(AttributeModifier.append("style", "height: " + height + "ex;"));
    return component;
  }

  /**
   * Adds class="focus" to the given component. It's evaluated by the adminica_ui.js. FocusOnLoadBehaviour doesn't work
   * because the focus is set to early (before the components are visible).
   *
   * @param component
   * @return This for chaining.
   */
  public static FormComponent<?> setFocus(final FormComponent<?> component)
  {
    component.add(setFocus());
    return component;
  }

  /**
   * Same as {@link #setFocus(FormComponent)}
   *
   * @return AttributeAppender
   */
  public static Behavior setFocus()
  {
    return new FocusOnLoadBehavior();
  }

  /**
   * For field-sets with multiple fields this method generates a multi label, such as "label1/label2", e. g.
   * "zip code/city".
   *
   * @param label
   * @return
   */
  public static String createMultipleFieldsetLabel(final String... labels)
  {
    return StringHelper.listToString("/", labels);
  }

  /**
   * If true then a tick-mark icon is returned, otherwise an invisible label.
   *
   * @param requestCycle
   * @param componentId
   * @param value
   * @return
   */
  public static Component createBooleanLabel(final RequestCycle requestCycle, final String componentId,
      final boolean value)
  {
    if (value == true) {
      return new IconPanel(componentId, IconType.ACCEPT);
    }
    return new Label(componentId, "invisible").setVisible(false);
  }

  /**
   * Searchs the attribute behavior (SimpleAttributeModifier or AttibuteApendModifier) with the given attribute name and
   * returns it if found, otherwise null.
   *
   * @param comp
   * @param name Name of attribute.
   */
  public static AttributeModifier getAttributeModifier(final Component comp, final String name)
  {
    for (final Behavior behavior : comp.getBehaviors()) {
      if (behavior instanceof AttributeAppender && name.equals(((AttributeAppender) behavior).getAttribute()) == true) {
        return (AttributeAppender) behavior;
      } else if (behavior instanceof AttributeModifier
          && name.equals(((AttributeModifier) behavior).getAttribute()) == true) {
        return (AttributeModifier) behavior;
      }
    }
    return null;
  }

  /**
   * Calls {@link Component#setResponsePage(Page)}. If the responseItem is an instance of a Page then setResponse for
   * this Page is called otherwise setResponse is called via {@link Component#getPage()}.
   *
   * @param component
   * @param responseItem Page or Component.
   */
  public static void setResponsePage(final Component component, final Component responseItem)
  {
    if (responseItem instanceof Page) {
      component.setResponsePage((Page) responseItem);
    } else {
      component.setResponsePage(responseItem.getPage());
    }
  }

  /**
   * Casts callerPage to Component and calls {@link #setResponsePage(Component, Component)}.
   *
   * @param component
   * @param callerPage Must be an instance of Component (otherwise a ClassCastException is thrown).
   */
  public static void setResponsePage(final Component component, final ISelectCallerPage callerPage)
  {
    setResponsePage(component, (Component) callerPage);
  }

  public static AttributeModifier javaScriptConfirmDialogOnClick(final String message)
  {
    final String escapedText = message.replace("'", "\'");
    return AttributeModifier.replace("onclick", "javascript:return showConfirmDialog('" + escapedText + "');");
  }

  @SuppressWarnings("unchecked")
  public static void setLabel(final FormComponent<?> component, final Label label)
  {
    final IModel<String> labelModel = (IModel<String>) label.getDefaultModel();
    if (component instanceof DatePanel) {
      ((DatePanel) component).getDateField().setLabel(labelModel);
    } else {
      component.setLabel(labelModel);
    }
  }

  public static boolean isParent(final Component parent, final Component descendant)
  {
    final MarkupContainer p = descendant.getParent();
    if (p == null) {
      return false;
    } else if (p == parent) {
      return true;
    } else {
      return isParent(parent, p);
    }
  }
}