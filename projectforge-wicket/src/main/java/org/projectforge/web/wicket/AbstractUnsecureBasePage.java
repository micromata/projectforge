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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.http.handler.RedirectRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.projectforge.AppVersion;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.doc.DocumentationPage;
import org.projectforge.web.servlet.LogoServlet;
import org.projectforge.web.session.MySession;

/**
 * Do only derive from this page, if no login is required!
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class AbstractUnsecureBasePage extends WebPage
{
  private static final long serialVersionUID = 7396310612549535899L;

  private static PackageTextTemplate jsTemplate;

  protected WebMarkupContainer body, html;

  protected boolean alreadySubmitted = false;

  @SpringBean
  private ConfigurationService configService;

  /**
   * Convenience method for creating a component which is in the mark-up file but should not be visible.
   * 
   * @param wicketId
   * @return
   */
  public static Label createInvisibleDummyComponent(final String wicketId)
  {
    final Label dummyLabel = new Label(wicketId);
    dummyLabel.setVisible(false);
    return dummyLabel;
  }

  /**
   * Constructor that is invoked when page is invoked without a session.
   * 
   * @param parameters Page parameters
   */
  @SuppressWarnings("serial")
  public AbstractUnsecureBasePage(final PageParameters parameters)
  {
    super(parameters);

    html = new TransparentWebMarkupContainer("html");
    add(html);
    add(new Label("windowTitle", new Model<String>()
    {
      @Override
      public String getObject()
      {
        return getWindowTitle();
      }
    }));

    body = new WebMarkupContainer("body")
    {
      @Override
      protected void onComponentTag(final ComponentTag tag)
      {
        onBodyTag(tag);
      }
    };

    if (WicketApplication.getTestsystemMode()) {
      body.add(new AttributeAppender("style", "background:" + WicketApplication.getTestsystemColor() + " !important;"));
    }

    add(body);
    final String logoServlet = LogoServlet.getBaseUrl();
    if (logoServlet != null) {
      body.add(new ContextImage("logoLeftImage", logoServlet));
    } else {
      body.add(new Label("logoLeftImage", "[invisible]").setVisible(false));
    }

    final WebMarkupContainer developmentSystem = new WebMarkupContainer("developmentSystem");
    developmentSystem.setOutputMarkupId(true);
    developmentSystem.setMarkupId("pf_develHint");
    body.add(developmentSystem);
    if (WebConfiguration.isDevelopmentMode() == false && WicketApplication.getTestsystemMode() == false) {
      developmentSystem.setVisible(false);
    }

    final PFUserDO user = ThreadLocalUserContext.getUser();
    AbstractLink link;
    link = new ExternalLink("footerNewsLink", "http://www.projectforge.org/pf-en/News");
    body.add(link);
    link.add(new Label("version", "Version " + AppVersion.VERSION.toString() + ", " + AppVersion.RELEASE_DATE)
        .setRenderBodyOnly(true));
    link.setOutputMarkupId(true);
    link.setMarkupId("pf_footerNewsLink");
  }

  @Override
  public void renderHead(final IHeaderResponse response)
  {
    super.renderHead(response);
    response.render(StringHeaderItem
        .forString(WicketUtils.getCssForFavicon(getUrl(configService.getServletContextPath() + "/favicon.ico"))));
    WicketRenderHeadUtils.renderMainCSSIncludes(response);
    //response.renderCSSReference();
    WicketRenderHeadUtils.renderMainJavaScriptIncludes(response);
    initializeContextMenu(response);
  }

  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    alreadySubmitted = false;
  }

  /**
   * Gets the version of this Application.
   * 
   * @see AppVersion#NUMBER
   */
  public final String getAppVersion()
  {
    return AppVersion.NUMBER;
  }

  /**
   * Gets the release date of this Application.
   * 
   * @see AppVersion#RELEASE_DATE
   */
  public final String getAppReleaseDate()
  {
    return AppVersion.RELEASE_DATE;
  }

  /**
   * Gets the release date of this Application.
   * 
   * @see AppVersion#RELEASE_DATE
   */
  public final String getAppReleaseTimestamp()
  {
    return AppVersion.RELEASE_TIMESTAMP;
  }

  /**
   * Includes session id (encode URL) at default.
   * 
   * @see #getUrl(String, boolean)
   */
  public String getUrl(final String path)
  {
    return getUrl(path, true);
  }

  /**
   * @see WicketUtils#getImageUrl(org.apache.wicket.Response, String)
   */
  public String getImageUrl(final String subpath)
  {
    return WicketUtils.getImageUrl(getRequestCycle(), subpath);
  }

  /**
   * @see WicketUtils#getUrl(org.apache.wicket.Response, String, boolean)
   */
  public String getUrl(final String path, final boolean encodeUrl)
  {
    return WicketUtils.getUrl(getRequestCycle(), path, encodeUrl);
  }

  /**
   * @param url
   * @see #getUrl(String)
   */
  protected void redirectToUrl(final String url)
  {
    getRequestCycle().scheduleRequestHandlerAfterCurrent(new RedirectRequestHandler(getUrl(url)));
  }

  protected abstract String getTitle();

  /**
   * Security. Implement this method if you are really sure that you want to implement an unsecure page (meaning this
   * page is available without any authorization, it's therefore public)!
   */
  protected abstract void thisIsAnUnsecuredPage();

  protected String getWindowTitle()
  {
    return AppVersion.APP_ID + " - " + getTitle();
  }

  /**
   * If your page need to manipulate the body tag overwrite this method, e. g.: tag.put("onload", "...");
   * 
   * @return
   */
  protected void onBodyTag(final ComponentTag bodyTag)
  {
  }

  protected WicketApplicationInterface getWicketApplication()
  {
    return (WicketApplicationInterface) getApplication();
  }

  /**
   * @see StringEscapeUtils#escapeHtml(String)
   */
  protected String escapeHtml(final String str)
  {
    return StringEscapeUtils.escapeHtml(str);
  }

  public MySession getMySession()
  {
    Session session = getSession();
    return (MySession) session;
  }

  /**
   * Always returns null for unsecured page, otherwise the logged-in user.
   * 
   * @return null
   * @see AbstractSecuredPage#getUser()
   */
  protected PFUserDO getUser()
  {
    return null;
  }

  /**
   * Always returns null for unsecured page, otherwise the id of the logged-in user.
   * 
   * @return null
   * @see AbstractSecuredPage#getUser()
   */
  protected Integer getUserId()
  {
    return null;
  }

  public String getLocalizedMessage(final String key, final Object... params)
  {
    if (params == null || params.length == 0) {
      return getString(key);
    }
    return MessageFormat.format(getString(key), params);
  }

  private void initializeContextMenu(final IHeaderResponse response)
  {

    // context menu
    final Map<String, String> i18nKeyMap = new HashMap<String, String>();
    i18nKeyMap.put("newTab", getString("contextMenu.newTab"));
    i18nKeyMap.put("cancel", getString("contextMenu.cancel"));
    response.render(OnDomReadyHeaderItem.forScript(getJstemplate().asString(i18nKeyMap)));
  }

  /**
   * @return the jstemplate
   */
  private static PackageTextTemplate getJstemplate()
  {
    if (jsTemplate == null) {
      jsTemplate = new PackageTextTemplate(AbstractUnsecureBasePage.class, "ContextMenu.js.template");
    }
    return jsTemplate;
  }

  protected TenantRegistry getTenantRegistry()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  protected UserGroupCache getUserGroupCache()
  {
    return getTenantRegistry().getUserGroupCache();
  }
}
