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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.Const;
import org.projectforge.SystemAlertMessage;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.configuration.DomainService;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.core.MenuBarPanel;
import org.projectforge.web.core.NavTopPanel;
import org.projectforge.web.dialog.ModalDialog;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;

import java.util.Random;

/**
 * All pages with required login should be derived from this page.
 */
public abstract class AbstractSecuredPage extends AbstractSecuredBasePage
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractSecuredPage.class);

  @SpringBean
  private ConfigurationService configurationService;

  @SpringBean
  private DomainService domainService;

  private static final long serialVersionUID = -8721451198050398835L;

  protected MenuBarPanel contentMenuBarPanel;

  /**
   * If set then return after save, update or cancel to this page. If not given then return to given list page.
   */
  protected WebPage returnToPage;

  private final RepeatingView modalDialogs;

  @SuppressWarnings("serial")
  public AbstractSecuredPage(final PageParameters parameters)
  {
    super(parameters);
    if (ThreadLocalUserContext.getUser() == null) {
      log.warn("AbstractSecuredPage called without logged-in user: " + this.getPageAsLink(parameters));
      // Shouldn't occur, but safe is safe:
      throw new RedirectToUrlException("/");
    }
    modalDialogs = new RepeatingView("modalDialogs");
    body.add(modalDialogs);
    if (UserFilter.isUpdateRequiredFirst() == false) {
      final NavTopPanel topMenuPanel = new NavTopPanel("topMenu");
      body.add(topMenuPanel);
      topMenuPanel.init(this);
    } else {
      final DivPanel emptyDivPanel = new DivPanel("topMenu");
      body.add(emptyDivPanel);
    }
    contentMenuBarPanel = new MenuBarPanel("menuBar");
    final Model<String> alertMessageModel = new Model<String>()
    {
      @Override
      public String getObject()
      {
        if (SystemAlertMessage.INSTANCE.getAlertMessage() == null) {
          return "neverDisplayed";
        }
        return SystemAlertMessage.INSTANCE.getAlertMessage();
      }
    };
    final WebMarkupContainer alertMessageContainer = new WebMarkupContainer("alertMessageContainer")
    {
      @Override
      public boolean isVisible()
      {
        return (SystemAlertMessage.INSTANCE.getAlertMessage() != null);
      }
    };
    body.add(alertMessageContainer);
    final Label alertMessageLabel = new Label("alertMessage", alertMessageModel);
    alertMessageContainer.add(alertMessageLabel.setRenderBodyOnly(true));

    body.add(getSnowEffectPanel(parameters));
  }

  private Panel getSnowEffectPanel(final PageParameters parameters)
  {
    boolean enableSnowEffect = configurationService.isSnowEffectEnabled();
    Boolean disableSnowEffectPermant = (Boolean) userXmlPreferencesCache.getEntry(ThreadLocalUserContext.getUserId(), "disableSnowEffectPermant");
    if (disableSnowEffectPermant != null) {
      enableSnowEffect = disableSnowEffectPermant ? false : enableSnowEffect;
    }
    enableSnowEffect = (enableSnowEffect && parameters.get("snowEffectEnable") != null && parameters.get("snowEffectEnable").isEmpty() == false) ?
        parameters.get("snowEffectEnable").toBoolean() :
        enableSnowEffect;
    enableSnowEffect = enableSnowEffect && new Random().nextInt(100) >= 50;
    if (enableSnowEffect) {
      return new SnowEffectPanel("snowEffect");
    } else {
      return new DivPanel("snowEffect");
    }
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    final WebMarkupContainer breadcrumbContainer = new WebMarkupContainer("breadcrumb");
    body.add(breadcrumbContainer);
    breadcrumbContainer.add(contentMenuBarPanel);
    if (isBreadCrumbVisible() == true) {
      final RepeatingView breadcrumbItems = new RepeatingView("li");
      breadcrumbContainer.add(breadcrumbItems);
      final WebPage returnTo = this.getReturnToPage();
      if (returnTo != null && returnTo instanceof AbstractSecuredPage) {
        addBreadCrumbs(breadcrumbItems, (AbstractSecuredPage) returnTo);
      } else {
        breadcrumbItems.setVisible(false);
      }
      breadcrumbContainer.add(new Label("active", getTitle()));
    } else {
      breadcrumbContainer.setVisible(false);
    }
  }

  @SuppressWarnings("serial")
  private void addBreadCrumbs(final RepeatingView breadcrumbItems, final AbstractSecuredPage page)
  {
    final WebPage returnTo = page.getReturnToPage();
    if (returnTo != null && returnTo instanceof AbstractSecuredPage) {
      addBreadCrumbs(breadcrumbItems, (AbstractSecuredPage) returnTo);
    }
    final WebMarkupContainer li = new WebMarkupContainer(breadcrumbItems.newChildId());
    breadcrumbItems.add(li);
    final Link<Void> pageLink = new Link<Void>("link")
    {

      @Override
      public void onClick()
      {
        setResponsePage(page);
      }
    };
    li.add(pageLink);
    pageLink.add(new Label("label", page.getTitle()));
  }

  /**
   * If set then return after save, update or cancel to this page. If not given then return to given list page. As an
   * alternative you can set the returnToPage as a page parameter (if supported by the derived page).
   *
   * @param returnToPage
   */
  public AbstractSecuredPage setReturnToPage(final WebPage returnToPage)
  {
    this.returnToPage = returnToPage;
    return this;
  }

  /**
   * @return the returnToPage
   */
  public WebPage getReturnToPage()
  {
    return returnToPage;
  }

  public void addContentMenuEntry(final ContentMenuEntryPanel panel)
  {
    this.contentMenuBarPanel.addMenuEntry(panel);
  }

  public String getNewContentMenuChildId()
  {
    return this.contentMenuBarPanel.newChildId();
  }

  /**
   * @return This page as link with the page parameters of this page.
   */
  public String getPageAsLink()
  {
    return getPageAsLink(getPageParameters());
  }

  /**
   * @param parameters
   * @return This page as link with the given page parameters.
   */
  public String getPageAsLink(final PageParameters parameters)
  {
    String relativeUrl = (String) urlFor(this.getClass(), parameters);
    if (relativeUrl.contains("../")) {
      // Therefore ignore relative pathes ../:
      relativeUrl = relativeUrl.replace("../", "");
    }

    String baseUrl = domainService.getDomainWithContextPath() + "/" + Const.WICKET_APPLICATION_PATH;

    return WicketUtils.toAbsolutePath(baseUrl, relativeUrl);
  }

  /**
   * Evaluates the page parameters and sets the properties, if parameters are given.
   *
   * @param parameters
   * @see WicketUtils#evaluatePageParameters(Object, PageParameters, String, String[])
   */
  protected void evaluateInitialPageParameters(final PageParameters parameters)
  {
    if (getBookmarkableInitialProperties() != null) {
      WicketUtils.evaluatePageParameters(getICallerPageForInitialParameters(), getDataObjectForInitialParameters(),
          getFilterObjectForInitialParameters(), parameters, getBookmarkableInitialProperties());
    }
  }

  /**
   * Adds additional page parameter. Used by NavTopPanel to show direct page links including the page parameters
   * returned by {@link #getBookmarkableInitialProperties()}.
   *
   * @see org.projectforge.web.wicket.AbstractUnsecurePage#getBookmarkableInitialParameters()
   */
  public PageParameters getBookmarkableInitialParameters()
  {
    final PageParameters pageParameters = new PageParameters();
    WicketUtils.putPageParameters(getICallerPageForInitialParameters(), getDataObjectForInitialParameters(),
        getFilterObjectForInitialParameters(), pageParameters, getBookmarkableInitialProperties());
    return pageParameters;
  }

  /**
   * The title of the page link shown with initial parameters (overridden by e. g. AbstractEditPage).
   *
   * @return
   */
  public String getTitleKey4BookmarkableInitialParameters()
  {
    return "bookmark.directPageExtendedLink";
  }

  /**
   * Properties which should be evaluated for new entries. These properties, if given in PageParameters, will be set as
   * initial values. All this properties will be set via {@link ISelectCallerPage#select(String, Object)}.
   */
  protected String[] getBookmarkableInitialProperties()
  {
    return null;
  }

  /**
   * Overwritten by e. g. {@link AbstractEditPage}.
   *
   * @return this at default (works if the page holds the data directly).
   */
  protected Object getDataObjectForInitialParameters()
  {
    return this;
  }

  /**
   * Overwritten by e. g. {@link AbstractListPage}.
   *
   * @return this at default (works if the page holds the data directly).
   */
  protected Object getFilterObjectForInitialParameters()
  {
    return null;
  }

  /**
   * @return this at default (works if the page is an instance of {@link ISelectCallerPage}.
   */
  protected ISelectCallerPage getICallerPageForInitialParameters()
  {
    if (this instanceof ISelectCallerPage) {
      return (ISelectCallerPage) this;
    } else {
      return null;
    }
  }

  public String newModalDialogId()
  {
    return modalDialogs.newChildId();
  }

  public void add(final ModalDialog modalDialog)
  {
    modalDialogs.add(modalDialog);
  }

  protected boolean isBreadCrumbVisible()
  {
    return true;
  }
}
