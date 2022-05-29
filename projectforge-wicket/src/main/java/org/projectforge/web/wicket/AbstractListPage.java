/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.Constants;
import org.projectforge.business.excel.ExportSheet;
import org.projectforge.common.StringHelper;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.common.i18n.UserException;
import org.projectforge.export.DOListExcelExporter;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.api.impl.HibernateSearchMeta;
import org.projectforge.framework.utils.RecentQueue;
import org.projectforge.framework.utils.ReflectionHelper;
import org.projectforge.rest.core.AbstractPagesRest;
import org.projectforge.rest.core.PagesResolver;
import org.projectforge.rest.multiselect.MultiSelectionSupport;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractListPage<F extends AbstractListForm<?, ?>, D extends IDao<?>, O extends IdObject<?>>
    extends AbstractSecuredPage implements ISelectCallerPage {
  private static final long serialVersionUID = 622509418161777195L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractListPage.class);

  public static final String PARAMETER_KEY_STORE_FILTER = "storeFilter";

  public static final String PARAMETER_KEY_FILTER = "f";

  public static final String PARAMETER_KEY_SEARCH_STRING = PARAMETER_KEY_FILTER + ".s";

  public static final String PARAMETER_HIGHLIGHTED_ROW = "row";

  private boolean calledBySearchPage;

  private List<O> resultList;

  // Don't refresh result list on default (only, if last getList() run was <10s)
  private boolean refreshResultList = false;

  // Page is displayed initial after user's click on the menu item.
  private boolean intialDisplay = true;

  // For detecting new sort order (for forcing list reload)
  private Object lastSortProperty;
  private boolean lastSortAscending;

  protected static final String[] BOOKMARKABLE_INITIAL_PROPERTIES = new String[]{"f.searchString|s",
      "f.useModificationFilter|mod",
      "f.modifiedByUserId|mUser", "f.startTimeOfLastModification|mStart", "f.stopTimeOfLastModification|mStop",
      "f.deleted|del", "pageSize"};

  protected static final String[] mergeStringArrays(final String[] a1, final String a2[]) {
    final String[] result = new String[a1.length + a2.length];
    int pos = 0;
    for (final String str : a1) {
      result[pos++] = str;
    }
    for (final String str : a2) {
      result[pos++] = str;
    }
    return result;
  }

  protected F form;

  protected DataTable<O, String> dataTable;

  private Serializable highlightedRowId;

  protected ISelectCallerPage caller;

  protected String selectProperty;

  protected String i18nPrefix;

  protected ContentMenuEntryPanel exportExcelButton;

  protected ContentMenuEntryPanel newItemMenuEntry;

  protected ContentMenuEntryPanel selectAllMenuEntry;

  protected ContentMenuEntryPanel deselectAllMenuEntry;

  protected boolean storeFilter = true;

  protected MyListPageSortableDataProvider<O> listPageSortableDataProvider;

  /**
   * Change this value if the recent search terms should be stored. Should be set in setup-method of derived page class.
   */
  protected String recentSearchTermsUserPrefKey = null;

  protected RecentQueue<String> recentSearchTermsQueue;

  public static void addRowClick(final Item<?> cellItem) {
    final Item<?> row = (cellItem.findParent(Item.class));
    WicketUtils.addRowClick(row);
  }

  protected AbstractListPage(final PageParameters parameters, final String i18nPrefix) {
    this(parameters, null, null, i18nPrefix);
  }

  protected AbstractListPage(final ISelectCallerPage caller, final String selectProperty, final String i18nPrefix) {
    this(new PageParameters(), caller, selectProperty, i18nPrefix);
  }

  protected AbstractListPage(final PageParameters parameters, final ISelectCallerPage caller,
                             final String selectProperty,
                             final String i18nPrefix) {
    super(parameters);
    if (parameters.get(PARAMETER_KEY_STORE_FILTER) != null) {
      final Boolean flag = WicketUtils.getAsBooleanObject(parameters, PARAMETER_KEY_STORE_FILTER);
      if (flag != null && flag == false) {
        storeFilter = false;
      }
    }
    if (parameters.get(PARAMETER_HIGHLIGHTED_ROW) != null) {
      setHighlightedRowId(WicketUtils.getAsInteger(parameters, PARAMETER_HIGHLIGHTED_ROW));
    }
    this.i18nPrefix = i18nPrefix;
    this.caller = caller;
    this.selectProperty = selectProperty;
    setup();
    preInit();
    evaluateInitialPageParameters(parameters);
  }

  /**
   * Copies all fields of the given filter to the current filter of the form.
   *
   * @param filter
   */
  public void copySearchFieldsFrom(final BaseSearchFilter filter) {
    form.copySearchFieldsFrom(filter);
  }

  /**
   * Is called before the form is initialized in constructor. Overwrite this method if any variables etc. should be set
   * before initialization.
   */
  protected void setup() {
  }

  /**
   * Highlight the row representing the data object with the given id.
   *
   * @param highlightedRowId
   */
  public void setHighlightedRowId(final Serializable highlightedRowId) {
    this.highlightedRowId = highlightedRowId;
  }

  public Serializable getHighlightedRowId() {
    return highlightedRowId;
  }

  private F getForm() {
    if (form == null) {
      form = newListForm(this);
    }
    return form;
  }

  /**
   * @param item             The item where to add the css classes.
   * @param rowDataId        If the current row data is equals to the hightlightedRow then the style will contain highlighting.
   * @param highlightedRowId The current row to highlight (id of the data object behind the row).
   * @param isDeleted        Is this entry deleted? Then the deleted style will be added.
   * @return
   */
  protected static void appendCssClasses(final Item<?> item, final Serializable rowDataId,
                                         final Serializable highlightedRowId,
                                         final boolean isDeleted) {
    if (rowDataId == null) {
      return;
    }
    if (rowDataId instanceof Integer == false) {
      log.warn("Error in calling getCssStyle: Integer expected instead of " + rowDataId.getClass());
    }
    if (highlightedRowId != null && rowDataId != null && Objects.equals(highlightedRowId, rowDataId) == true) {
      appendCssClasses(item, RowCssClass.HIGHLIGHTED);
    }
    if (isDeleted == true) {
      appendCssClasses(item, RowCssClass.MARKED_AS_DELETED);
    }
  }

  /**
   * Adds some standard css classes such as {@link RowCssClass#MARKED_AS_DELETED} for deleted entries.
   *
   * @param item      The item where to add the css classes.
   * @param rowDataId If the current row data is equals to the hightlightedRow then the style will contain highlighting.
   * @param isDeleted Is this entry deleted? Then the deleted style will be added.
   * @return
   */
  protected void appendCssClasses(final Item<?> item, final Serializable rowDataId, final boolean isDeleted) {
    appendCssClasses(item, rowDataId, this.highlightedRowId, isDeleted);
  }

  /**
   * @param item          The item where to add the css classes.
   * @param rowCssClasses The css class to append to the given item.
   * @return
   */
  protected static void appendCssClasses(final Item<?> item, final RowCssClass... rowCssClasses) {
    WicketUtils.append(item, rowCssClasses);
  }

  /**
   * Adds storeFilter=false to the parameters.
   *
   * @see org.projectforge.web.wicket.AbstractSecuredPage#getBookmarkableInitialParameters()
   */
  @Override
  public PageParameters getBookmarkableInitialParameters() {
    final PageParameters pageParameters = super.getBookmarkableInitialParameters();
    WicketUtils.addOrReplaceParameter(pageParameters, PARAMETER_KEY_STORE_FILTER, false);
    return pageParameters;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredPage#getBookmarkableInitialProperties()
   */
  @Override
  protected String[] getBookmarkableInitialProperties() {
    return BOOKMARKABLE_INITIAL_PROPERTIES;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredPage#getFilterObjectForInitialParameters()
   */
  @Override
  protected Object getFilterObjectForInitialParameters() {
    return form.getSearchFilter();
  }

  /**
   * @return the form.
   * @see org.projectforge.web.wicket.AbstractSecuredPage#getDataObjectForInitialParameters()
   */
  @Override
  protected Object getDataObjectForInitialParameters() {
    return form;
  }

  /**
   * @return This page as link with the page parameters of this page.
   */
  @Override
  public String getPageAsLink() {
    return getPageAsLink(new PageParameters());
  }

  @SuppressWarnings("serial")
  private void preInit() {
    getForm();
    body.add(form);
    form.init();
    if (isSelectMode() == false
        && (accessChecker.isDemoUser() == true || getBaseDao().hasInsertAccess(getUser()) == true)) {
      newItemMenuEntry = new ContentMenuEntryPanel(contentMenuBarPanel.newChildId(), new Link<Object>("link") {
        @Override
        public void onClick() {
          redirectToEditPage(null);
        }

      }, IconType.PLUS);
      newItemMenuEntry.setAccessKey(WebConstants.ACCESS_KEY_ADD).setTooltip(
          getString(WebConstants.ACCESS_KEY_ADD_TOOLTIP_TITLE),
          getString(WebConstants.ACCESS_KEY_ADD_TOOLTIP));
      contentMenuBarPanel.addMenuEntry(newItemMenuEntry);
    }
    final Label hintQuickSelectLabel = new Label("hintQuickSelect",
        new Model<String>(getString("hint.selectMode.quickselect"))) {
      @Override
      public boolean isVisible() {
        return isSelectMode();
      }
    };
    form.add(hintQuickSelectLabel);
    addTopRightMenu();
    addTopPanel();
    addBottomPanel("bottomPanel");
    init();
  }

  /**
   * Will be called by the constructors.
   */
  protected abstract void init();

  /**
   * Called if the user clicks on the "new" (new entry) link.
   *
   * @param params nullable or set by derived class methods before calling super.onNewClick();
   * @return The edit page (response page). The return value has no effect. It's only useful for derived class methods
   * which calls super.onNewClick();
   */
  protected AbstractEditPage<?, ?, ?> redirectToEditPage(PageParameters params) {
    if (params == null) {
      params = new PageParameters();
    }
    final Class<?> editPageClass = getClass().getAnnotation(ListPage.class).editPage();
    final AbstractEditPage<?, ?, ?> editPage = (AbstractEditPage<?, ?, ?>) ReflectionHelper.newInstance(editPageClass,
        PageParameters.class, params);
    editPage.setReturnToPage(AbstractListPage.this);
    setResponsePage(editPage);
    return editPage;
  }

  public abstract D getBaseDao();

  /**
   * @return true, if response page is set for redirect (e. g. for successful quick selection), otherwise false.
   */
  @SuppressWarnings("unchecked")
  protected boolean onSearchSubmit() {
    log.debug("onSearchSubmit");
    refresh();
    if (isSelectMode() == true) {
      final List<O> list = getList();
      if (list != null && list.size() == 1) {
        // Quick select:
        final O obj = list.get(0);
        caller.select(selectProperty, ((BaseDO<Integer>) obj).getId());
        WicketUtils.setResponsePage(this, caller);
        return true;
      }
    } else {
      // auto-select of a single entry:
      // final String searchString = form.searchFilter.getSearchString();
      // if (searchString != null && searchString.matches("id:[0-9]+") == true) {
      // final Integer id = NumberHelper.parseInteger(searchString.substring(3));
      // if (id != null) {
      // final PageParameters pageParams = new PageParameters();
      // pageParams.add(AbstractEditPage.PARAMETER_KEY_ID, String.valueOf(id));
      // redirectToEditPage(pageParams);
      // return true;
      // }
      // }
    }
    return false;
  }

  protected void onResetSubmit() {
    log.debug("onResetSubmit");
    form.getSearchFilter().reset();
    refresh();
    form.clearInput();
  }

  /**
   * User has pressed the cancel button. If in selection mode then redirect to the caller.
   */
  protected void onCancelSubmit() {
    log.debug("onCancelSubmit");
    if (isSelectMode() == true && caller != null) {
      WicketUtils.setResponsePage(this, caller);
      caller.cancelSelection(selectProperty);
    }
  }

  /**
   * Called, if the list must be refreshed. Sets list to null and page size of data table.
   */
  public void refresh() {
    this.resultList = null; // Force reload of list
    this.refreshResultList = true;
    long itemsPerPage = dataTable.getItemsPerPage();
    if (form.getPageSize() != itemsPerPage) {
      itemsPerPage = form.getPageSize();
    }
    if (itemsPerPage < 1) {
      itemsPerPage = 25; // cannot be less than 1
    }
    dataTable.setItemsPerPage(itemsPerPage);
    addRecentSearchTerm();
  }

  public List<O> getList() {
    return internalGetList();
  }

  private List<O> internalGetList() {
    final String userPrefKey = this.getClass().getName() + ".durationOfLastGetList";
    if (this.intialDisplay) {
      this.intialDisplay = false;
      // Detect to show the result list on initial display or not.
      Object obj = getUserPrefEntry(userPrefKey);
      Long durationOfLastGetList = null;
      if (obj instanceof Long) {
        durationOfLastGetList = (Long) obj;
      }
      if (durationOfLastGetList != null && durationOfLastGetList < Constants.MILLIS_PER_SECOND * 10L) {
        this.refreshResultList = true;
      }
    }
    if (!this.refreshResultList) {
      return this.resultList;
    }
    try {
      long startMillis = System.currentTimeMillis();
      internalBuildList();
      long duration = System.currentTimeMillis() - startMillis;
      putUserPrefEntry(userPrefKey, duration, true);
    } catch (Exception ex) {
      // Just for case any exception occurred, avoid to reload page on next page view.
      // Otherwise an filter producing db errors can't be reset by user, if db query will be done instantly on page view.
      removeUserPrefEntryIfNotExists(userPrefKey);
    }
    this.refreshResultList = false;
    return this.resultList;
  }

  private void internalBuildList() {
    try {
      this.resultList = buildList();
      listPageSortableDataProvider.setCompleteList(this.resultList);
      if (this.resultList == null) {
        // An error occured:
        form.addError("search.error");
      }
      return;
    } catch (
        final Exception ex) {
      if (ex instanceof UserException) {
        final UserException userException = (UserException) ex;
        error(getLocalizedMessage(userException.getI18nKey(), userException.getParams()));
      } else {
        log.error(ex.getMessage(), ex);
      }
    }
    this.resultList = new ArrayList<O>();
  }

  @SuppressWarnings("unchecked")
  protected List<O> buildList() {
    List<O> list = (List<O>) getBaseDao().getList(form.getSearchFilter());
    int size = 0;
    if (list != null) {
      size = list.size();
    }
    int maxRows = form.getSearchFilter().getMaxRows();
    if (maxRows <= 0) {
      maxRows = QueryFilter.QUERY_FILTER_MAX_ROWS;
    }
    if (list != null && list.size() == maxRows) {
      form.addError("search.maxRowsExceeded", maxRows);
    }
    return list;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#onBeforeRender()
   */
  @Override
  protected void onBeforeRender() {
    final SingleSortState<?> newSortState = (SingleSortState<?>) listPageSortableDataProvider.getSortState();
    final SortParam<?> sortParam = newSortState.getSort();
    final Object newSortProperty = sortParam != null ? sortParam.getProperty() : null;
    final boolean newAscending = sortParam != null && sortParam.isAscending();
    if (!intialDisplay) {
      if (!Objects.equals(newSortProperty, lastSortProperty) || newAscending != lastSortAscending)
        // User pressed the head of the colum to sort the result list.
        refreshResultList = true;
    }
    lastSortProperty = newSortProperty;
    lastSortAscending = newAscending;
    internalGetList();
    super.onBeforeRender();
  }

  /**
   * @see org.apache.wicket.markup.html.WebPage#onAfterRender()
   */
  @Override
  protected void onAfterRender() {
    super.onAfterRender();
    this.resultList = null; // Don't waste memory.
  }

  protected abstract F newListForm(AbstractListPage<?, ?, ?> parentPage);

  protected String getSearchToolTip() {
    return getLocalizedMessage("search.string.info", getSearchFields());
  }

  @SuppressWarnings("serial")
  protected void addTopRightMenu() {
    if (isSelectMode() == false
        && ((getBaseDao() instanceof BaseDao<?>) || providesOwnRebuildDatabaseIndex() == true || true)) {
      new AbstractReindexTopRightMenu(this.contentMenuBarPanel, accessChecker.isLoggedInUserMemberOfAdminGroup()) {
        @Override
        protected void rebuildDatabaseIndex(final boolean onlyNewest) {
          if (providesOwnRebuildDatabaseIndex() == true) {
            ownRebuildDatabaseIndex(onlyNewest);
          } else {
            if (onlyNewest == true) {
              ((IPersistenceService<?>) getBaseDao()).rebuildDatabaseIndex4NewestEntries();
            } else {
              ((IPersistenceService<?>) getBaseDao()).rebuildDatabaseIndex();
            }
          }
        }

        @Override
        protected String getString(final String i18nKey) {
          return AbstractListPage.this.getString(i18nKey);
        }
      };
    }
  }

  protected boolean providesOwnRebuildDatabaseIndex() {
    return false;
  }

  protected void ownRebuildDatabaseIndex(final boolean onlyNewest) {
  }

  /**
   * Override this method if you need a top panel. The default top panel is empty and not visible.
   */
  protected void addTopPanel() {
    final Panel topPanel = new EmptyPanel("topPanel");
    topPanel.setVisible(false);
    form.add(topPanel);
  }

  /**
   * Override this method if you need a bottom panel. The default bottom panel is empty and not visible.
   */
  protected void addBottomPanel(final String id) {
    final Panel bottomPanel = new EmptyPanel(id);
    bottomPanel.setVisible(false);
    form.add(bottomPanel);
  }

  /**
   * Later: Try AjaxFallBackDatatable again.
   *
   * @param columns
   * @param sortProperty
   * @param ascending
   * @return
   */
  protected DataTable<O, String> createDataTable(final List<IColumn<O, String>> columns, final String sortProperty,
                                                 final SortOrder sortOrder) {
    int pageSize = form.getPageSize();
    if (pageSize < 1) {
      pageSize = 50;
    }
    final SortParam<String> sortParam = sortProperty != null
        ? new SortParam<String>(sortProperty, sortOrder == SortOrder.ASCENDING) : null;
    return new DefaultDataTable<O, String>("table", columns, createSortableDataProvider(sortParam), pageSize);
    // return new AjaxFallbackDefaultDataTable<O>("table", columns, createSortableDataProvider(sortProperty, ascending), pageSize);
  }

  /**
   * At default a new SortableDOProvider is returned. Overload this method e. g. for avoiding
   * LazyInitializationExceptions due to sorting.
   *
   * @param sortProperty
   * @param ascending
   */
  protected ISortableDataProvider<O, String> createSortableDataProvider(final SortParam<String> sortParam) {
    return createSortableDataProvider(sortParam, null);
  }

  /**
   * At default a new SortableDOProvider is returned. Overload this method e. g. for avoiding
   * LazyInitializationExceptions due to sorting.
   *
   * @param sortProperty
   * @param ascending
   */
  protected ISortableDataProvider<O, String> createSortableDataProvider(final SortParam<String> sortParam,
                                                                        final SortParam<String> secondSortParam) {
    if (listPageSortableDataProvider == null) {
      listPageSortableDataProvider = new MyListPageSortableDataProvider<O>(sortParam, secondSortParam, this);
    }
    return listPageSortableDataProvider;
  }

  /**
   * For displaying the hibernate search fields. Returns list as csv. These fields the user can directly address in his
   * search string, e. g. street:marie.
   *
   * @return
   * @see org.projectforge.framework.persistence.api.BaseDao#getSearchFields()
   */
  public String getSearchFields() {
    return StringHelper.listToString(", ", HibernateSearchMeta.INSTANCE.getSearchFields(getBaseDao()));
  }

  /**
   * @return true, if this page is called for selection by a caller otherwise false.
   */
  public boolean isSelectMode() {
    return this.caller != null;
  }

  /**
   * Calls getString(key) with key "[i18nPrefix].title.list" or "[i18nPrefix].title.list.select" dependent weather the
   * list is shown for browsing or selecting (select mode).
   *
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#getTitle()
   * @see #isSelectMode()
   */
  @Override
  protected String getTitle() {
    if (isSelectMode() == true) {
      return getString(i18nPrefix + ".title.list.select");
    } else {
      return getString(i18nPrefix + ".title.list");
    }
  }

  /**
   * If false then the action filter will not be stored (the previous stored filter will be preserved). true is default.
   */
  public boolean isStoreFilter() {
    return storeFilter;
  }

  /**
   * Adds a excel export content menu entry. ProjectForge exports all data fields (annotated with {@link PropertyInfo}
   * of the current displayed result list.
   *
   * @param filenameIdentifier If given then the id will be part of the exported filename, may be null.
   * @param sheetTitle         may be null.
   */
  public void addExcelExport(final String filenameIdentifier, final String sheetTitle) {
    exportExcelButton = new ContentMenuEntryPanel(getNewContentMenuChildId(),
        new Link<Object>("link") {
          @Override
          public void onClick() {
            exportExcel(filenameIdentifier, sheetTitle);
          }

        }, getString("exportAsXls")).setTooltip(getString("tooltip.export.excel"));
    addContentMenuEntry(exportExcelButton);
  }

  public void addNewMassSelect(final Class<? extends AbstractPagesRest<?, ?, ?>> pagesRestClazz) {
    addNewMassSelect("multiselection.button", pagesRestClazz, null, null);
  }

  public void addNewMassSelect(final Class<? extends AbstractPagesRest<?, ?, ?>> pagesRestClazz, final Object data) {
    addNewMassSelect("multiselection.button", pagesRestClazz, null, data);
  }

  public void addNewMassSelect(final String buttonTitleKey, final Class<? extends AbstractPagesRest<?, ?, ?>> pagesRestClazz) {
    addNewMassSelect(buttonTitleKey, pagesRestClazz, null, null);
  }

  public void addNewMassSelect(final String buttonTitleKey, final Class<? extends AbstractPagesRest<?, ?, ?>> pagesRestClass, final String toolTipKey) {
    addNewMassSelect(buttonTitleKey, pagesRestClass, toolTipKey, null);
  }

  public void addNewMassSelect(final String buttonTitleKey, final Class<? extends AbstractPagesRest<?, ?, ?>> pagesRestClass, final String toolTipKey,
                               final Object data) {
    final String caller = ((String) urlFor(this.getClass(), new PageParameters())).replace("./", "/wa/");
    final ContentMenuEntryPanel button = new ContentMenuEntryPanel(getNewContentMenuChildId(),
        new Link<Object>("link") {
          @Override
          public void onClick() {
            refreshResultList = true; // List was deleted due to not wasting memory.
            MultiSelectionSupport.registerEntitiesForSelection(WicketUtils.getHttpServletRequest(getRequest()), pagesRestClass, getList(),
                caller, data, getForm().searchFilter.getPageSize());
            resultList = null; // Don't waste memory.
            final String redirectUrl = PagesResolver.getMultiSelectionPageUrl(pagesRestClass, true);
            throw new RedirectToUrlException(redirectUrl);
          }

        }, getString(buttonTitleKey));
    if (toolTipKey != null) {
      button.setTooltip(getString(toolTipKey));
    }
    addContentMenuEntry(button);
  }

  protected DOListExcelExporter createExcelExporter(final String filenameIdentifier) {
    return new DOListExcelExporter(filenameIdentifier);
  }

  protected void exportExcel(final String filenameIdentifier, final String sheetTitle) {
    refresh();
    final DOListExcelExporter exporter = createExcelExporter(filenameIdentifier);
    if (exporter == null) {
      // Nothing to export.
      form.addError("validation.error.nothingToExport");
      return;
    }
    final List<?> list = getList();
    if (list != null && list.size() > 0) {
      final ExportSheet sheet = exporter.addSheet(sheetTitle != null ? sheetTitle : "data");
      exporter.addList(sheet, list);
      if (exporter.isExcelAutoFilter() == true) {
        sheet.setAutoFilter();
      }
    }
    exporter.onBeforeDownload();
    if (exporter.getWorkbook().getNumberOfSheets() == 0) {
      // Nothing to export.
      form.addError("validation.error.nothingToExport");
      return;
    }
    DownloadUtils.setDownloadTarget(exporter.getWorkbook().getAsByteArray(), exporter.getFilename());
  }

  /**
   * Does nothing at default. If overload, don't forget to call super.cancelSelection(String) if no property matches.
   *
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  @Override
  public void cancelSelection(final String property) {
    // Do nothing.
  }

  /**
   * Handles modifiedByUserId. If overload, don't forget to call super.select(String) if no property matches.
   *
   * @see org.projectforge.web.wicket.AbstractListPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue) {
    if ("modifiedByUserId".equals(property) == true) {
      form.getSearchFilter().setModifiedByUserId((Integer) selectedValue);
      form.getSearchFilter().setUseModificationFilter(true);
      refresh();
    } else {
      log.error("Property '" + property + "' not supported for selection in class " + getClass().getName() + ".");
    }
  }

  /**
   * Handles modifiedByUserId. If overload, don't forget to call super.select(String) if no property matches.
   *
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property) {
    if ("modifiedByUserId".equals(property) == true) {
      form.getSearchFilter().setModifiedByUserId(null);
      form.getSearchFilter().setUseModificationFilter(true);
      refresh();
    } else {
      log.error("Property '" + property + "' not supported for selection in class " + getClass().getName() + ".");
    }
  }

  @SuppressWarnings("unchecked")
  public RecentQueue<String> getRecentSearchTermsQueue() {
    if (recentSearchTermsQueue == null) {
      recentSearchTermsQueue = (RecentQueue<String>) getUserPrefEntry(this.recentSearchTermsUserPrefKey);
    }
    if (recentSearchTermsQueue == null) {
      recentSearchTermsQueue = new RecentQueue<String>();
      if (isRecentSearchTermsStorage() == true) {
        putUserPrefEntry(this.recentSearchTermsUserPrefKey, recentSearchTermsQueue, true);
      }
    }
    return recentSearchTermsQueue;
  }

  /**
   * Adds the search string to the recent list, if filter is from type BaseSearchFilter and the search string is not
   * blank and not from type id:4711.
   *
   * @param Filter The search filter.
   */
  protected void addRecentSearchTerm() {
    if (StringUtils.isNotBlank(form.searchFilter.getSearchString()) == true) {
      final String s = form.searchFilter.getSearchString();
      if (s.startsWith("id:") == false || StringUtils.isNumeric(s.substring(3)) == false) {
        // OK, search string is not from type id:4711
        getRecentSearchTermsQueue().append(s);
      }
    }
  }

  /**
   * @return True, if the user-pref-key for storing the recent search terms is given, otherwise false.
   */
  public boolean isRecentSearchTermsStorage() {
    return this.recentSearchTermsUserPrefKey != null;
  }

  /**
   * Tiny helper method.
   *
   * @param propertyName
   * @param sortable
   * @return return sortable ? propertyName : null;
   */
  protected static String getSortable(final String propertyName, final boolean sortable) {
    return sortable ? propertyName : null;
  }

  /**
   * ONLY for internal purposes to tell the IListPageColumnsCreator that it's instantiated by the SearchAreaPanel.
   *
   * @param calledBySearchPage the calledBySearchForm to set
   */
  public void setCalledBySearchPage(final boolean calledBySearchPage) {
    this.calledBySearchPage = calledBySearchPage;
  }

  /**
   * @return the calledBySearchForm
   */
  public boolean isCalledBySearchPage() {
    return calledBySearchPage;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredPage#getReturnToPage()
   */
  @Override
  public WebPage getReturnToPage() {
    if (this.returnToPage != null) {
      return this.returnToPage;
    } else if (caller != null && caller instanceof WebPage) {
      return (WebPage) caller;
    }
    return null;
  }
}
