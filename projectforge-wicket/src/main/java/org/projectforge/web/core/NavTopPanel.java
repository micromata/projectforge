/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.core;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.menu.builder.MenuCreator;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.rest.ChangePasswordPageRest;
import org.projectforge.rest.MyMenuPageRest;
import org.projectforge.rest.my2fa.My2FASetupPageRest;
import org.projectforge.rest.MyAccountPageRest;
import org.projectforge.rest.core.PagesResolver;
import org.projectforge.web.WicketLoginService;
import org.projectforge.web.WicketMenuBuilder;
import org.projectforge.web.WicketMenuEntry;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.core.menuconfig.MenuConfig;
import org.projectforge.web.dialog.ModalDialog;
import org.projectforge.web.session.MySession;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.CsrfTokenHandler;
import org.projectforge.web.wicket.FeedbackPage;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import java.util.Collection;
import java.util.Optional;

/**
 * Displays the favorite menu.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class NavTopPanel extends NavAbstractPanel {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NavTopPanel.class);

  private static final long serialVersionUID = -7858806882044188339L;

  private BookmarkDialog bookmarkDialog;

  /**
   * Cross site request forgery token.
   */
  private CsrfTokenHandler csrfTokenHandler;
  private RepeatingView menuRepeater;

  public NavTopPanel(final String id) {
    super(id);
  }

  @SuppressWarnings("serial")
  public void init(final AbstractSecuredPage page) {
    getMenu();
    favoritesMenu = WicketSupport.get(WicketMenuBuilder.class).getFavoriteMenu();
    add(new MenuConfig("menuconfig", getMenu()));
    add(new BookmarkablePageLink<Void>("feedbackLink", FeedbackPage.class));
    {
      final AjaxLink<Void> showBookmarkLink = new AjaxLink<Void>("showBookmarkLink") {
        /**
         * @see org.apache.wicket.ajax.markup.html.AjaxLink#onClick(org.apache.wicket.ajax.AjaxRequestTarget)
         */
        @Override
        public void onClick(final AjaxRequestTarget target) {
          bookmarkDialog.open(target);
          // Redraw the content:
          bookmarkDialog.redraw().addContent(target);
        }
      };
      add(showBookmarkLink);
      addBookmarkDialog();
    }
    {
      add(new Label("user", ThreadLocalUserContext.getLoggedInUser().getFullname()));

      final RepeatingView pluginPersonalMenuEntriesRepeater = new RepeatingView("pluginPersonalMenuEntriesRepeater");
      add(pluginPersonalMenuEntriesRepeater);
      if (WicketSupport.get(AccessChecker.class).isRestrictedUser() == true) {
        // Show ChangePasswordPage as my account for restricted users.
        final ExternalLink changePasswordLink = new ExternalLink("myAccountLink", PagesResolver.getDynamicPageUrl(ChangePasswordPageRest.class));
        add(changePasswordLink);
        addVacationViewLink().setVisible(false);
        pluginPersonalMenuEntriesRepeater.setVisible(false);
      } else {
        final ExternalLink myAccountLink = new ExternalLink("myAccountLink", PagesResolver.getDynamicPageUrl(MyAccountPageRest.class, null, null, true));
        add(myAccountLink);
        final ExternalLink myMenuLink = new ExternalLink("myMenuLink", PagesResolver.getDynamicPageUrl(MyMenuPageRest.class, null, null, true));
        add(myMenuLink);
        final ExternalLink my2FactorAuthentificationLink = new ExternalLink("my2FactorAuthentificationLink", PagesResolver.getDynamicPageUrl(My2FASetupPageRest.class, null, null, true));
        add(my2FactorAuthentificationLink);
        addVacationViewLink();
        for (MenuItemDef menu : WicketSupport.get(MenuCreator.class).getPersonalMenuPluginEntries()) {
          // Now we add a new menu area (title with sub menus):
          final WebMarkupContainer linkContainer = new WebMarkupContainer(pluginPersonalMenuEntriesRepeater.newChildId());
          pluginPersonalMenuEntriesRepeater.add(linkContainer);
          String link = menu.getUrl();
          if (link != null && !link.startsWith("/")) {
            link = "/" + link;
          }
          String title = getString(menu.getI18nKey());
          final ExternalLink menuLink = new ExternalLink("menuLink", link);
          linkContainer.add(menuLink);
          menuLink.add(new Label("menuLabel", title));
        }
      }
      final Link<Void> logoutLink = new Link<Void>("logoutLink") {
        @Override
        public void onClick() {
          WicketSupport.get(WicketLoginService.class).logout((MySession) getSession(), (WebRequest) getRequest(), (WebResponse) getResponse());
          WicketUtils.redirectToLogin(this);
        }
      };
      logoutLink.setMarkupId("logout").setOutputMarkupId(true);
      add(logoutLink);
    }
    addCompleteMenu();
  }

  private ExternalLink addVacationViewLink() {
    final ExternalLink vacationViewLink = new ExternalLink("vacationViewLink", "/" + MenuItemDefId.VACATION_ACCOUNT.getUrl()) {
      @Override
      public boolean isVisible() {
        return WicketSupport.get(VacationService.class).hasAccessToVacationService(ThreadLocalUserContext.getLoggedInUser(), false);
      }
    };
    add(vacationViewLink);
    return vacationViewLink;
  }

  @SuppressWarnings("serial")
  private void addCompleteMenu() {
    final Label totalMenuSuffixLabel = new MenuSuffixLabel("totalMenuCounter", new Model<Integer>() {
      @Override
      public Integer getObject() {
        return menu.getTotalBadgeCounter();
      }

    });
    add(totalMenuSuffixLabel);

    final RepeatingView completeMenuCategoryRepeater = new RepeatingView("completeMenuCategoryRepeater");
    add(completeMenuCategoryRepeater);
    if (menu.getMenuEntries() != null) {
      for (final WicketMenuEntry menuEntry : menu.getMenuEntries()) {
        if (menuEntry.getSubMenuEntries() == null) {
          continue;
        }
        // Now we add a new menu area (title with sub menus):
        final WebMarkupContainer categoryContainer = new WebMarkupContainer(completeMenuCategoryRepeater.newChildId());
        completeMenuCategoryRepeater.add(categoryContainer);
        categoryContainer.add(new Label("menuCategoryLabel", getString(menuEntry.getI18nKey())));
        final Label areaSuffixLabel = getSuffixLabel(menuEntry);
        categoryContainer.add(areaSuffixLabel);

        // final WebMarkupContainer subMenuContainer = new WebMarkupContainer("subMenu");
        // categoryContainer.add(subMenuContainer);
        if (menuEntry.hasSubMenuEntries() == false) {
          // subMenuContainer.setVisible(false);
          continue;
        }

        final RepeatingView completeSubMenuRepeater = new RepeatingView("completeSubMenuRepeater");
        categoryContainer.add(completeSubMenuRepeater);
        for (final WicketMenuEntry subMenuEntry : menuEntry.getSubMenuEntries()) {
          if (subMenuEntry.getSubMenuEntries() != null) {
            log.error(
                "Oups: sub sub menus not supported: " + menuEntry.getId() + " has child menus which are ignored.");
          }
          // Now we add the next menu entry to the area:
          final WebMarkupContainer subMenuItem = new WebMarkupContainer(completeSubMenuRepeater.newChildId());
          completeSubMenuRepeater.add(subMenuItem);
          final AbstractLink link = getMenuEntryLink(subMenuEntry, true);
          if (link != null) {
            subMenuItem.add(link);
          } else {
            subMenuItem.setVisible(false);
          }
        }
      }
    }

  }

  private void addFavoriteMenu() {
    // Favorite menu:
    menuRepeater = new RepeatingView("menuRepeater");
    add(menuRepeater);
    final Collection<WicketMenuEntry> menuEntries = favoritesMenu.getMenuEntries();
    if (menuEntries != null) {
      for (final WicketMenuEntry menuEntry : menuEntries) {
        // Now we add a new menu area (title with sub menus):
        final WebMarkupContainer menuItem = new WebMarkupContainer(menuRepeater.newChildId());
        menuRepeater.add(menuItem);
        final AbstractLink link = getMenuEntryLink(menuEntry, true);
        if (link == null) {
          menuItem.setVisible(false);
          continue;
        }
        menuItem.add(link);

        final WebMarkupContainer subMenuContainer = new WebMarkupContainer("subMenu");
        menuItem.add(subMenuContainer);
        final WebMarkupContainer caret = new WebMarkupContainer("caret");
        link.add(caret);
        if (menuEntry.hasSubMenuEntries() == false) {
          subMenuContainer.setVisible(false);
          caret.setVisible(false);
          continue;
        }
        menuItem.add(AttributeModifier.append("class", "dropdown"));
        link.add(AttributeModifier.append("class", "dropdown-toggle"));
        link.add(AttributeModifier.append("data-toggle", "dropdown"));
        final RepeatingView subMenuRepeater = new RepeatingView("subMenuRepeater");
        subMenuContainer.add(subMenuRepeater);
        for (final WicketMenuEntry subMenuEntry : menuEntry.getSubMenuEntries()) {
          // Now we add the next menu entry to the area:
          if (subMenuEntry.hasSubMenuEntries() == false) {
            final WebMarkupContainer subMenuItem = new WebMarkupContainer(subMenuRepeater.newChildId());
            subMenuRepeater.add(subMenuItem);
            // Subsubmenu entries aren't yet supported, show only the sub entries without children, otherwise only the children are
            // displayed.
            final AbstractLink subLink = getMenuEntryLink(subMenuEntry, true);
            if (subLink == null) {
              subMenuItem.setVisible(false);
              continue;
            }
            subMenuItem.add(subLink);
            continue;
          }

          // final WebMarkupContainer subsubMenuContainer = new WebMarkupContainer("subsubMenu");
          // subMenuItem.add(subsubMenuContainer);
          // if (subMenuEntry.hasSubMenuEntries() == false) {
          // subsubMenuContainer.setVisible(false);
          // continue;
          // }
          // final RepeatingView subsubMenuRepeater = new RepeatingView("subsubMenuRepeater");
          // subsubMenuContainer.add(subsubMenuRepeater);
          for (final WicketMenuEntry subsubMenuEntry : subMenuEntry.getSubMenuEntries()) {
            // Now we add the next menu entry to the sub menu:
            final WebMarkupContainer subMenuItem = new WebMarkupContainer(subMenuRepeater.newChildId());
            subMenuRepeater.add(subMenuItem);
            // Subsubmenu entries aren't yet supported, show only the sub entries without children, otherwise only the children are
            // displayed.
            final AbstractLink subLink = getMenuEntryLink(subsubMenuEntry, true);
            if (subLink == null) {
              subMenuItem.setVisible(false);
              continue;
            }
            subMenuItem.add(subLink);
            // final WebMarkupContainer subsubMenuItem = new WebMarkupContainer(subsubMenuRepeater.newChildId());
            // subsubMenuRepeater.add(subsubMenuItem);
            // final AbstractLink subsubLink = getMenuEntryLink(subsubMenuEntry, subsubMenuItem);
            // subsubMenuItem.add(subsubLink);
          }
        }
      }
    }
  }


  @Override
  protected void onBeforeRender() {
    super.onBeforeRender();
    Optional.ofNullable(menuRepeater).ifPresent(this::remove);
    addFavoriteMenu();
  }

  private void addBookmarkDialog() {
    final AbstractSecuredPage parentPage = (AbstractSecuredPage) getPage();
    bookmarkDialog = new BookmarkDialog(parentPage.newModalDialogId());
    bookmarkDialog.setOutputMarkupId(true);
    parentPage.add(bookmarkDialog);
    bookmarkDialog.init();
  }

  @SuppressWarnings("serial")
  private class BookmarkDialog extends ModalDialog {
    /**
     * @param id
     */
    public BookmarkDialog(final String id) {
      super(id);
    }

    @Override
    public void init() {
      setTitle(getString("bookmark.title"));
      init(new Form<String>(getFormId()));
      gridBuilder.newFormHeading(""); // Otherwise it's empty and an IllegalArgumentException is thrown.
    }

    private BookmarkDialog redraw() {
      clearContent();
      final AbstractSecuredPage page = (AbstractSecuredPage) NavTopPanel.this.getPage();
      {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("bookmark.directPageLink")).setLabelSide(false);
        final TextArea<String> textArea = new TextArea<String>(fs.getTextAreaId(),
            new Model<String>(page.getPageAsLink()));
        fs.add(textArea);
        textArea.add(AttributeModifier.replace("onclick", "$(this).select();"));
      }
      final PageParameters params = page.getBookmarkableInitialParameters();
      if (params.isEmpty() == false) {
        final FieldsetPanel fs = gridBuilder.newFieldset(getString(page.getTitleKey4BookmarkableInitialParameters()))
            .setLabelSide(false);
        final TextArea<String> textArea = new TextArea<String>(fs.getTextAreaId(),
            new Model<String>(page.getPageAsLink(params)));
        fs.add(textArea);
        textArea.add(AttributeModifier.replace("onclick", "$(this).select();"));
      }
      return this;
    }
  }
}
