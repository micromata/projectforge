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

package org.projectforge.web;

import java.io.Serializable;

import org.apache.commons.lang.Validate;
import org.apache.wicket.Page;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserRight;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.wicket.WicketApplication;

/**
 * The menu is defined once. The user's personal menu is calculated by this menu definitions (which menu entries are
 * visible and which not).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MenuItemDef implements Serializable
{
  private static final long serialVersionUID = 6793153590139785117L;

  private String id;

  private String i18nKey;

  private MenuItemDef parent, mobileParentMenu;

  private Class<? extends Page> pageClass;

  private String url;

  private String[] params;

  private boolean newWindow;

  private Class<? extends Page> mobilePageClass;

  private boolean visible = true;

  private ProjectForgeGroup[] visibleForGroups;

  private IUserRightId requiredRightId;

  private UserRightValue[] requiredRightValues;

  private int orderNumber, mobileMenuOrderNumber;

  private boolean mobileMenuSupport = false;

  private boolean desktopMenuSupport = true;

  private boolean visibleForRestrictedUsers;

  private MenuItemDefVisibility visibility;

  /**
   * Overwrite this if you need special access checking.
   * 
   * @param context
   * @return visibility.
   * @see #isVisible()
   */
  protected boolean isVisible(final MenuBuilderContext context)
  {
    return isVisible();
  }

  /**
   * Calls {@link MenuItemDefVisibility#isVisible()} if visibility object is given, otherwise flag set by
   * {@link #setVisible(boolean)} (true as default).
   * 
   * @return
   * @see MenuItemDefVisibility#isVisible()
   */
  public boolean isVisible()
  {
    if (this.visibility != null) {
      try {
        return this.visibility.isVisible();
      } catch (final Exception ex) {
        // log.error("Exception occured while trying to evaluate visibility of a menu entry: " + ex.getMessage(), ex);
        return false;
      }
    }
    return this.visible;
  }

  /**
   * @param visible
   * @return this for chaining.
   */
  public MenuItemDef setVisible(final boolean visible)
  {
    this.visible = visible;
    return this;
  }

  public MenuItemDef setVisibility(final MenuItemDefVisibility visibility)
  {
    this.visibility = visibility;
    return this;
  }

  /**
   * Most menus aren't visibly for restrictedUsers.
   * 
   * @return the visibleByRestrictedUsers
   */
  public boolean isVisibleForRestrictedUsers()
  {
    return visibleForRestrictedUsers;
  }

  /**
   * @param visibleForRestrictedUsers the visibleByRestrictedUsers to set
   * @return this for chaining.
   */
  public MenuItemDef setVisibleForRestrictedUsers(final boolean visibleForRestrictedUsers)
  {
    this.visibleForRestrictedUsers = visibleForRestrictedUsers;
    return this;
  }

  /**
   * Creates a menu entry if the user has access to.
   * 
   * @return null if not visible otherwise the created MenuEntry.
   */
  protected MenuEntry createMenuEntry(final Menu menu, final MenuBuilderContext context)
  {
    if (visibleForRestrictedUsers == false && context.getLoggedInUser().isRestrictedUser() == true) {
      // Restricted users have not access to.
      return null;
    }
    if (context.isMobileMenu() == true && mobileMenuSupport == false) {
      // this menu item doesn't support the mobile menu.
      return null;
    }
    if (context.isMobileMenu() == false && desktopMenuSupport == false) {
      // this menu item doesn't support the desktop menu.
      return null;
    }
    if (requiredRightId != null
        && hasRight(context.getAccessChecker(), context.getUserRights(), context.getLoggedInUser()) == false) {
      return null;
    }
    if (isVisible(context) == false) {
      return null;
    }
    final ProjectForgeGroup[] visibleForGroups = getVisibleForGroups();
    if (visibleForGroups != null
        && visibleForGroups.length > 0
        && context.getAccessChecker().isLoggedInUserMemberOfGroup(visibleForGroups) == false) {
      // Do nothing because menu is not visible for logged in user.
      return null;
    }
    final MenuEntry menuEntry = new MenuEntry(this, context);
    afterMenuEntryCreation(menuEntry, context);
    menuEntry.setMenu(menu);
    return menuEntry;
  }

  /**
   * Override this method if some modifications needed after a menu entry for an user's menu is created.
   * 
   * @param createdMenuEntry The fresh created menu entry (is never null).
   * @param context
   */
  protected void afterMenuEntryCreation(final MenuEntry createdMenuEntry, final MenuBuilderContext context)
  {
  }

  public MenuItemDef()
  {
  }

  /**
   * @param parent The parent menu entry
   * @param id The unique id
   * @param orderNumber The order of the sibling menu entries is done implemented by this order number (ascending
   *          order).
   * @param i18nKey For displaying the menu entry localized.
   * @param pageClass The linked page class (if the user clicks on this menu entry).
   * @param requiredRightId Reduce the visibility of this menu entry (if wanted): which user right is required?
   * @param requiredRightValues Reducing the visibility: which right values are required?
   */
  public MenuItemDef(final MenuItemDef parent, final String id, final int orderNumber, final String i18nKey,
      final Class<? extends Page> pageClass, final IUserRightId requiredRightId,
      final UserRightValue... requiredRightValues)
  {
    this(parent, id, orderNumber, i18nKey, pageClass, null, requiredRightId, requiredRightValues);
  }

  /**
   * @param parent The parent menu entry
   * @param id The unique id
   * @param orderNumber The order of the sibling menu entries is done implemented by this order number (ascending
   *          order).
   * @param i18nKey For displaying the menu entry localized.
   * @param pageClass The linked page class (if the user clicks on this menu entry).
   * @param params Parameters used when calling the pageClass (PageParameters).
   * @param requiredRightId Reduce the visibility of this menu entry (if wanted): which user right is required?
   * @param requiredRightValues Reducing the visibility: which right values are required?
   */
  public MenuItemDef(final MenuItemDef parent, final String id, final int orderNumber, final String i18nKey,
      final Class<? extends Page> pageClass, final String[] params, final IUserRightId requiredRightId,
      final UserRightValue... requiredRightValues)
  {
    this.parent = parent;
    this.id = id;
    this.orderNumber = orderNumber;
    this.i18nKey = i18nKey;
    this.pageClass = pageClass;
    this.params = params;
    this.requiredRightId = requiredRightId;
    this.requiredRightValues = requiredRightValues;
  }

  /**
   * @param parent The parent menu entry
   * @param id The unique id
   * @param orderNumber The order of the sibling menu entries is done implemented by this order number (ascending
   *          order).
   * @param i18nKey For displaying the menu entry localized.
   * @param pageClass The linked page class (if the user clicks on this menu entry).
   * @param visibleForGroups Reduce the visibility of this menu entry (if wanted).
   */
  public MenuItemDef(final MenuItemDef parent, final String id, final int orderNumber, final String i18nKey,
      final Class<? extends Page> pageClass, final ProjectForgeGroup... visibleForGroups)
  {
    this(parent, id, orderNumber, i18nKey, pageClass, null, visibleForGroups);
  }

  /**
   * @param parent The parent menu entry
   * @param id The unique id
   * @param orderNumber The order of the sibling menu entries is done implemented by this order number (ascending
   *          order).
   * @param i18nKey For displaying the menu entry localized.
   * @param pageClass The linked page class (if the user clicks on this menu entry).
   * @param params Parameters used when calling the pageClass (PageParameters).
   * @param visibleForGroups Reduce the visibility of this menu entry (if wanted).
   */
  public MenuItemDef(final MenuItemDef parent, final String id, final int orderNumber, final String i18nKey,
      final Class<? extends Page> pageClass, final String[] params, final ProjectForgeGroup... visibleForGroups)
  {
    this.parent = parent;
    this.id = id;
    this.orderNumber = orderNumber;
    this.i18nKey = i18nKey;
    this.pageClass = pageClass;
    this.params = params;
    this.visibleForGroups = visibleForGroups;
  }

  /**
   * @param parent The parent menu entry
   * @param id The unique id
   * @param orderNumber The order of the sibling menu entries is done implemented by this order number (ascending
   *          order).
   * @param i18nKey For displaying the menu entry localized.
   * @param url The linked url (if the user clicks on this menu entry).
   * @param visibleForGroups Reduce the visibility of this menu entry (if wanted).
   */
  public MenuItemDef(final MenuItemDef parent, final String id, final int orderNumber, final String i18nKey,
      final String url,
      final ProjectForgeGroup... visibleForGroups)
  {
    this(parent, id, orderNumber, i18nKey, url, false, visibleForGroups);
  }

  /**
   * @param parent The parent menu entry
   * @param id The unique id
   * @param orderNumber The order of the sibling menu entries is done implemented by this order number (ascending
   *          order).
   * @param i18nKey For displaying the menu entry localized.
   * @param url The linked url (if the user clicks on this menu entry).
   * @param newWindow If true, then the link will be opened in a new browser window.
   * @param visibleForGroups Reduce the visibility of this menu entry (if wanted).
   */
  public MenuItemDef(final MenuItemDef parent, final String id, final int orderNumber, final String i18nKey,
      final String url,
      final boolean newWindow, final ProjectForgeGroup... visibleForGroups)
  {
    this.parent = parent;
    this.id = id;
    this.orderNumber = orderNumber;
    this.i18nKey = i18nKey;
    this.url = url;
    this.newWindow = newWindow;
    this.visibleForGroups = visibleForGroups;
  }

  /**
   * A menu entry without a link (e. g. a parent menu entry).
   * 
   * @param parent The parent menu entry
   * @param id The unique id
   * @param orderNumber The order of the sibling menu entries is done implemented by this order number (ascending
   *          order).
   * @param i18nKey For displaying the menu entry localized.
   * @param visibleForGroups Reduce the visibility of this menu entry (if wanted).
   */
  public MenuItemDef(final MenuItemDef parent, final String id, final int orderNumber, final String i18nKey,
      final ProjectForgeGroup... visibleForGroups)
  {
    this.parent = parent;
    this.id = id;
    this.orderNumber = orderNumber;
    this.i18nKey = i18nKey;
    this.visibleForGroups = visibleForGroups;
  }

  /**
   * @param parent The parent menu entry
   * @param id The unique id
   * @param orderNumber The order of the sibling menu entries is done implemented by this order number (ascending
   *          order).
   * @param i18nKey For displaying the menu entry localized.
   * @param url The linked url (if the user clicks on this menu entry).
   * @param requiredRightId Reduce the visibility of this menu entry (if wanted): which user right is required?
   * @param requiredRightValues Reducing the visibility: which right values are required?
   */
  public MenuItemDef(final MenuItemDef parent, final String id, final int orderNumber, final String i18nKey,
      final String url,
      final IUserRightId requiredRightId, final UserRightValue... requiredRightValues)
  {
    this(parent, id, orderNumber, i18nKey, url, false, requiredRightId, requiredRightValues);
  }

  /**
   * @param parent The parent menu entry
   * @param id The unique id
   * @param orderNumber The order of the sibling menu entries is done implemented by this order number (ascending
   *          order).
   * @param i18nKey For displaying the menu entry localized.
   * @param url The linked url (if the user clicks on this menu entry).
   * @param newWindow If true, then the link will be opened in a new browser window.
   * @param requiredRightId Reduce the visibility of this menu entry (if wanted): which user right is required?
   * @param requiredRightValues Reducing the visibility: which right values are required?
   */
  public MenuItemDef(final MenuItemDef parent, final String id, final int orderNumber, final String i18nKey,
      final String url,
      final boolean newWindow, final IUserRightId requiredRightId, final UserRightValue... requiredRightValues)
  {
    this.parent = parent;
    this.id = id;
    this.orderNumber = orderNumber;
    this.i18nKey = i18nKey;
    this.url = url;
    this.newWindow = newWindow;
    this.requiredRightId = requiredRightId;
    this.requiredRightValues = requiredRightValues;
  }

  /**
   * @return parent menu item definition or null if this definition represents a top level menu item.
   */
  public MenuItemDef getParent()
  {
    return parent;
  }

  /**
   * @return Id used for html markup and for referencing in config.xml.
   */
  public String getId()
  {
    return id;
  }

  /**
   * Order number for sorting menu entries.
   */
  public int getOrderNumber()
  {
    return orderNumber;
  }

  /**
   * If set to false then this menu entry will not be displayed in the classical web menu version. Default is true.
   * 
   * @param desktopMenuSupport
   */
  public void setDesktopMenuSupport(final boolean desktopMenuSupport)
  {
    this.desktopMenuSupport = desktopMenuSupport;
  }

  /**
   * Will be automatically set if any setter regarding mobile menu properties is calles (with not-null params), default
   * is false.
   * 
   * @param mobileMenuSupport
   */
  public void setMobileMenuSupport(final boolean mobileMenuSupport)
  {
    this.mobileMenuSupport = mobileMenuSupport;
  }

  /**
   * Order number for sorting menu entries (mobile menu).
   */
  public int getMobileMenuOrderNumber()
  {
    return mobileMenuOrderNumber;
  }

  /**
   * @return The parent menu entry of the mobile menu.
   */
  public MenuItemDef getMobileParentMenu()
  {
    return mobileParentMenu;
  }

  /**
   * TODO: Not yet supported. The menu entry is set as a top level menu entry.
   * 
   * @param mobileParentEntry
   * @param mobileMenuOrderNumber
   * @return this for chaining.
   */
  public MenuItemDef setMobileMenu(final MenuItemDef mobileParentEntry, final Class<? extends Page> mobilePageClass,
      final int mobileMenuOrderNumber)
  {
    this.mobileParentMenu = mobileParentEntry;
    setMobileMenu(mobilePageClass, mobileMenuOrderNumber);
    return this;
  }

  /**
   * Adds the given menu entry as root menu entry.
   * 
   * @param mobileParentEntry
   * @param mobileMenuOrderNumber
   * @return this for chaining.
   */
  public MenuItemDef setMobileMenu(final Class<? extends Page> mobilePageClass, final int mobileMenuOrderNumber)
  {
    this.mobileMenuSupport = true;
    this.mobilePageClass = mobilePageClass;
    this.mobileMenuOrderNumber = mobileMenuOrderNumber;
    return this;
  }

  /**
   * @return Key used in the i18n resource bundle.
   */
  public String getI18nKey()
  {
    return i18nKey;
  }

  /**
   * @return Wicket page or null for non Wicket pages.
   */
  public Class<? extends Page> getPageClass()
  {
    return pageClass;
  }

  /**
   * @param pageClass the pageClass to set
   * @return this for chaining.
   */
  public void setPageClass(final Class<? extends Page> pageClass)
  {
    this.pageClass = pageClass;
  }

  /**
   * @return Wicket page or null for non Wicket pages.
   */
  public Class<? extends Page> getMobilePageClass()
  {
    return mobilePageClass;
  }

  /**
   * @return true, if pageClass (Wicket page) is given otherwise false.
   */
  public boolean isWicketPage()
  {
    return this.pageClass != null;
  }

  public boolean hasUrl()
  {
    return url != null;
  }

  /**
   * @return true if this menu entry has a link (to a Wicket page or an url). Otherwise false (e. g. if this menu item
   *         def represents only a menu with sub menus).
   */
  public boolean isLink()
  {
    return isWicketPage() == true || hasUrl() == true;
  }

  /**
   * @return The url for non-Wicket pages (relative to "secure/") or the bookmarkable url for Wicket pages (relative to
   *         "wa/").
   */
  public String getUrl()
  {
    if (url == null) {
      // Late binding: may be this enum class was instantiated before WicketApplication was initialized.
      this.url = WicketApplication.getBookmarkableMountPath(this.pageClass);
    }
    return url;
  }

  public String[] getParams()
  {
    return params;
  }

  public boolean isNewWindow()
  {
    return newWindow;
  }

  public ProjectForgeGroup[] getVisibleForGroups()
  {
    return visibleForGroups;
  }

  public IUserRightId getRequiredRightId()
  {
    return requiredRightId;
  }

  public UserRightValue[] getRequiredRightValues()
  {
    return requiredRightValues;
  }

  public boolean hasRight(final AccessChecker accessChecker, UserRightService userRights, final PFUserDO loggedInUser)
  {
    if (requiredRightId == null || requiredRightValues == null) {
      // Should not occur, for security reasons deny at default.
      return false;
    }
    if (requiredRightValues.length == 0) {
      final UserRight right = userRights.getRight(requiredRightId);
      if (right instanceof UserRightAccessCheck<?>) {
        Validate.notNull(loggedInUser);
        return (((UserRightAccessCheck<?>) right).hasSelectAccess(loggedInUser) == true);
      } else {
        // Should not occur, for security reasons deny at default.
        return false;
      }
    }

    if (accessChecker.hasLoggedInUserRight(requiredRightId, false, requiredRightValues) == true) {
      return true;
    }
    return false;
  }

  @Override
  public String toString()
  {
    return id;
  }
}
