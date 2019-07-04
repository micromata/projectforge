package org.projectforge.web.plugin;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebPage;
import org.projectforge.menu.builder.MenuCreator;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.menu.builder.MenuItemDefId;
import org.projectforge.registry.Registry;
import org.projectforge.web.MenuItemRegistry;
import org.projectforge.web.kotlinsupport.KotlinComponents;
import org.projectforge.web.registry.WebRegistry;
import org.projectforge.web.registry.WebRegistryEntry;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PluginWicketRegistrationService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginWicketRegistrationService.class);

  public MenuCreator getMenuCreator() {
    return KotlinComponents.getMenuCreator();
  }

  @Autowired
  public MenuItemRegistry menuItemRegistry;

  public MenuItemDef getMenuItemDef(final MenuItemDefId menuItemDefId) {
    return getMenuCreator().findById(menuItemDefId);
  }

  public void registerMenuItem(final MenuItemDefId parentId, final MenuItemDef menuItemDef) {
    registerMenuItem(parentId, menuItemDef, null);
  }

  public void registerMenuItem(final MenuItemDefId parentId, final MenuItemDef menuItemDef, Class<? extends Page> pageClass) {
    registerMenuItem(parentId.getId(), menuItemDef, pageClass);
  }


  public void registerMenuItem(final String parentId, final MenuItemDef menuItemDef) {
    registerMenuItem(parentId, menuItemDef, null);
  }

  public void registerMenuItem(final String parentId, final MenuItemDef menuItemDef, Class<? extends Page> pageClass) {
    if (StringUtils.isEmpty(menuItemDef.getUrl())) {
      String url = WebRegistry.getInstance().getMountPoint(pageClass);
      menuItemDef.setUrl(url);
    }
    getMenuCreator().add(parentId, menuItemDef);
    if (pageClass != null)
      menuItemRegistry.register(menuItemDef.getId(), pageClass);
  }

  public void registerTopLevelMenuItem(final MenuItemDef menuItemDef, Class<? extends Page> pageClass) {
    getMenuCreator().addTopLevelMenu(menuItemDef);
    if (pageClass != null)
      menuItemRegistry.register(menuItemDef.getId(), pageClass);
  }

  /**
   * Use this method if your entities don't support the general search page (e. g. if you have no data-base entities
   * which implements {@link Historizable}).
   *
   * @param id
   * @return this for chaining.
   * @see WebRegistry#register(String)
   */
  public void registerWeb(final String id) {
    WebRegistry.getInstance().register(id);
  }

  /**
   * @param id
   * @param existingEntryId
   * @param insertBefore
   * @return this for chaining.
   * @see WebRegistry#register(WebRegistryEntry, boolean, WebRegistryEntry)
   */
  public void registerWeb(final String id, final String existingEntryId, final boolean insertBefore) {
    final WebRegistryEntry existingEntry = WebRegistry.getInstance().getEntry(id);
    WebRegistry.getInstance().register(existingEntry, insertBefore, new WebRegistryEntry(Registry.getInstance(), id));
  }

  /**
   * @param id
   * @param pageListClass list page to mount. Needed for displaying the result-sets by the general search page if the
   *                      list page implements {@link IListPageColumnsCreator}.
   * @param pageEditClass edit page to mount.
   * @return this for chaining.
   * @see WebRegistry#register(String, Class)
   * @see WebRegistry#addMountPages(String, Class, Class)
   */
  public void registerWeb(final String id, final Class<? extends WebPage> pageListClass,
                          final Class<? extends WebPage> pageEditClass) {
    registerWeb(id, pageListClass, pageEditClass, null, false);
  }

  /**
   * @param id
   * @param pageListClass list page to mount. Needed for displaying the result-sets by the general search page if the
   *                      list page implements {@link IListPageColumnsCreator}.
   * @param pageEditClass edit page to mount.
   * @return this for chaining.
   * @see WebRegistry#register(String, Class)
   * @see WebRegistry#addMountPages(String, Class, Class)
   */
  @SuppressWarnings("unchecked")
  public void registerWeb(final String id, final Class<? extends WebPage> pageListClass,
                          final Class<? extends WebPage> pageEditClass, final String existingEntryId, final boolean insertBefore) {
    WebRegistryEntry entry;
    if (IListPageColumnsCreator.class.isAssignableFrom(pageListClass) == true) {
      entry = new WebRegistryEntry(Registry.getInstance(), id,
              (Class<? extends IListPageColumnsCreator<?>>) pageListClass);
    } else {
      entry = new WebRegistryEntry(Registry.getInstance(), id);
    }
    if (existingEntryId != null) {
      final WebRegistryEntry existingEntry = WebRegistry.getInstance().getEntry(existingEntryId);
      WebRegistry.getInstance().register(existingEntry, insertBefore, entry);
    } else {
      WebRegistry.getInstance().register(entry);
    }
    WebRegistry.getInstance().addMountPages(id, pageListClass, pageEditClass);
  }

  /**
   * @param mountPage
   * @param pageClass
   * @return this for chaining.
   * @see WebRegistry#addMountPages(String, Class)
   */
  public void addMountPage(final String mountPage, final Class<? extends WebPage> pageClass) {
    WebRegistry.getInstance().addMountPage(mountPage, pageClass);
  }

  /**
   * @param mountPageBasename
   * @param pageListClass
   * @param pageEditClass
   * @return this for chaining.
   * @see WebRegistry#addMountPages(String, Class, Class)
   */
  public void addMountPages(final String mountPageBasename, final Class<? extends WebPage> pageListClass,
                            final Class<? extends WebPage> pageEditClass) {
    WebRegistry.getInstance().addMountPages(mountPageBasename, pageListClass, pageEditClass);
  }

}
