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

import java.util.MissingResourceException;

import org.apache.commons.lang.ClassUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.LabeledWebMarkupContainer;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.resource.loader.BundleStringResourceLoader;
import org.apache.wicket.settings.ResourceSettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.projectforge.ProjectForgeApp;
import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.test.AbstractTestBase;
import org.projectforge.web.LoginPage;
import org.projectforge.web.LoginService;
import org.projectforge.web.MenuBuilder;
import org.projectforge.web.session.MySession;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;

/**
 * Your wicket tester class must extends this or any derived class from AbstractTestBase for correct initialization of
 * Spring, data-base, resource locator etc. Before your tests a new data-base is initialized and set-up with test data.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class WicketPageTestBase extends AbstractTestBase
{
  protected static final String KEY_LOGINPAGE_BUTTON_LOGIN = "loginButton:button";

  protected WicketTester tester;

  @Autowired
  private UserXmlPreferencesCache userXmlPreferencesCache;

  @Autowired
  private MenuBuilder menuBuilder;

  @Autowired
  private PluginAdminService pluginAdminService;

  @Autowired
  private LoginService loginService;

  /**
   * don't know why, but we chache it...
   */
  private static ResourceSettings resourceSettings;

  private class WicketTestApplication extends WebApplication implements WicketApplicationInterface
  {

    @Override
    protected void init()
    {
      log.info("Init WicketTestApplication");
      super.init();
      getComponentInstantiationListeners().add(new SpringComponentInjector(this, applicationContext));
      if (ProjectForgeApp.getInstance() == null || ProjectForgeApp.getInstance().isInitialized() == false) {
        // Only on first initialization.
        log.info("Init resource loader");
        addResourceBundle(WicketApplication.RESOURCE_BUNDLE_NAME);
        resourceSettings = getResourceSettings();
        ProjectForgeApp.init(applicationContext, isDevelopmentSystem());
        addPluginResources();
      } else {
        log.info("Restore resource settings from last initialization");
        setResourceSettings(resourceSettings);
      }
    }

    @Override
    public Class<? extends Page> getHomePage()
    {
      return WicketUtils.getDefaultPage();
    }

    @Override
    public Session newSession(final Request request, final Response response)
    {
      return new MySession(request);
    }

    /**
     * @see org.projectforge.web.wicket.WicketApplicationInterface#isDevelopmentSystem()
     */
    @Override
    public boolean isDevelopmentSystem()
    {
      return false;
    }

    /**
     * @see org.projectforge.web.wicket.WicketApplicationInterface#isStripWicketTags()
     */
    @Override
    public boolean isStripWicketTags()
    {
      return true;
    }

    private void addPluginResources()
    {
      for (AbstractPlugin plugin : pluginAdminService.getActivePlugin()) {
        for (String bundleName : plugin.getResourceBundleNames()) {
          addResourceBundle(bundleName);
        }
      }
    }

    private void addResourceBundle(String bundleName)
    {
      // Prepend the resource bundle for overwriting some Wicket default localizations (such as StringValidator.*)
      getResourceSettings().getStringResourceLoaders().add(new BundleStringResourceLoader(bundleName));
      I18nHelper.addBundleName(bundleName);
    }
  }

  @BeforeClass
  public void setUpWicketApplication()
  {
    tester = new WicketTester(new WicketTestApplication());
  }

  /**
   * Logs the user in, if not already logged-in. If an user is already logged in then nothing is done. Therefore you
   * must log-out an user before any new login.
   *
   * @param username
   * @param password not encrypted.
   */
  public void login(final String username, final String password)
  {
    login(username, password, true);
  }

  /**
   * Logs the user in, if not already logged-in. If an user is already logged in then nothing is done. Therefore you
   * must log-out an user before any new login.
   *
   * @param username
   * @param password not encrypted.
   */
  public void login(final String username, final String password, final boolean checkDefaultPage)
  {
    // start and render the test page
    tester.startPage(new LoginPage(new PageParameters()));
    if (ClassUtils.isAssignable(tester.getLastRenderedPage().getClass(), WicketUtils.getDefaultPage()) == true) {
      // Already logged-in.
      return;
    }
    // assert rendered page class
    tester.assertRenderedPage(LoginPage.class);
    final FormTester form = tester.newFormTester("body:form");
    form.setValue(findComponentByLabel(form, "username"), username);
    form.setValue(findComponentByLabel(form, "password"), password);
    form.submit(KEY_LOGINPAGE_BUTTON_LOGIN);
    if (checkDefaultPage == true) {
      tester.assertRenderedPage(WicketUtils.getDefaultPage());
    }
  }

  public void loginTestAdmin()
  {
    login(AbstractTestBase.TEST_ADMIN_USER, AbstractTestBase.TEST_ADMIN_USER_PASSWORD);
  }

  /**
   * Searches FormComponents (model object), LabeledWebmarkupContainers (label model) and ContentMenuEntryPanels
   * (label).
   *
   * @param container
   * @param label     i18n key of the label or label (for buttons).
   * @return Found component with the given label or null if no such component found.
   * @see FormComponent#getModelObject()
   * @see LabeledWebMarkupContainer#getLabel()
   * @see ContentMenuEntryPanel#getLabel()
   */
  public Component findComponentByLabel(final MarkupContainer container, final String label)
  {
    String str = label;
    try {
      str = container.getString(label);
    } catch (final MissingResourceException ex) {
      // OK
    }
    final String locLabel = str;
    final Component[] component = new Component[1];
    container.visitChildren(new IVisitor<Component, Void>()
    {
      @Override
      public void component(final Component object, final IVisit<Void> visit)
      {
        if (object instanceof AbstractLink) {
          final MarkupContainer parent = object.getParent();
          if (parent instanceof ContentMenuEntryPanel) {
            if (labelEquals(((ContentMenuEntryPanel) parent).getLabel(), label, locLabel) == true) {
              component[0] = object;
              visit.stop();
            }
          } else if (object.getId().equals(locLabel) == true) {
            component[0] = object;
            visit.stop();
          }
        } else {
          if (object instanceof LabeledWebMarkupContainer) {
            final IModel<String> labelModel = ((LabeledWebMarkupContainer) object).getLabel();
            if (labelModel != null) {
              if (labelEquals(labelModel.getObject(), label, locLabel) == true) {
                component[0] = object;
                visit.stop();
              }
            }
          }
          if (object instanceof FormComponent<?>) {
            final Object modelObject = ((FormComponent<?>) object).getModelObject();
            if (modelObject instanceof String) {
              if (labelEquals((String) modelObject, label, locLabel) == true) {
                component[0] = object;
                visit.stop();
              }
            }
          }
        }
      }
    });
    return component[0];
  }

  /**
   * Searches ContentMenuEntryPanels.
   *
   * @param container
   * @param accessKey
   * @return Found component with the given label or null if no such component found.
   * @see FormComponent#getModelObject()
   * @see LabeledWebMarkupContainer#getLabel()
   * @see ContentMenuEntryPanel#getLabel()
   */
  public Component findComponentByAccessKey(final MarkupContainer container, final char accessKey)
  {
    final Component[] component = new Component[1];
    container.visitChildren(new IVisitor<Component, Void>()
    {
      @Override
      public void component(final Component object, final IVisit<Void> visit)
      {
        if (object instanceof AbstractLink) {
          final AbstractLink link = (AbstractLink) object;
          final AttributeModifier attrMod = WicketUtils.getAttributeModifier(link, "accesskey");
          if (attrMod == null || attrMod.toString().contains("object=[n]") == false) {
            return;
          }
          component[0] = object;
          visit.stop();
        }
      }
    });
    return component[0];
  }

  private boolean labelEquals(final String label, final String l1, final String l2)
  {
    if (label == null) {
      return false;
    }
    return l1 != null && label.equals(l1) == true || l2 != null && label.equals(l2) == true;
  }

  public Component findComponentByLabel(final FormTester form, final String label)
  {
    return findComponentByLabel(form.getForm(), label);
  }

  /**
   * @param tester        WicketTester with the last rendered page.
   * @param containerPath path of the container to search in.
   * @param label
   * @see #findComponentByLabel(MarkupContainer, String)
   */
  public Component findComponentByLabel(final WicketTester tester, final String containerPath, final String label)
  {
    return findComponentByLabel((MarkupContainer) tester.getComponentFromLastRenderedPage(containerPath), label);
  }

  /**
   * @param tester        WicketTester with the last rendered page.
   * @param containerPath path of the container to search in.
   * @param label
   * @see #findComponentByLabel(MarkupContainer, String)
   */
  public Component findComponentByAccessKey(final WicketTester tester, final String containerPath, final char accessKey)
  {
    return findComponentByAccessKey((MarkupContainer) tester.getComponentFromLastRenderedPage(containerPath),
        accessKey);
  }

  /**
   * Logs out any current logged-in user and calls log-in page.
   */
  protected void logout()
  {
    loginService.logout((MySession) tester.getSession(), tester.getRequest(), tester.getResponse(), userXmlPreferencesCache, menuBuilder);
    tester.startPage(LoginPage.class);
    tester.assertRenderedPage(LoginPage.class);
  }

}
