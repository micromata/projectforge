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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.multitenancy.TenantChecker;
import org.projectforge.business.user.UserCache;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.framework.persistence.api.IPersistenceService;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.time.DateFormatType;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.admin.WizardPage;
import org.projectforge.web.task.TaskTreePage;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.flowlayout.DiffTextPanel;

import de.micromata.genome.db.jpa.history.api.HistoryServiceManager;

public abstract class AbstractEditPage<O extends AbstractBaseDO<Integer>, F extends AbstractEditForm<O, ?>, D extends IPersistenceService<O>>
    extends
    AbstractSecuredPage implements IEditPage<O, D>
{
  private static final long serialVersionUID = 8283877351980165438L;

  public static final String PARAMETER_KEY_ID = "id";

  public static final String PARAMETER_KEY_DATA_PRESET = "__data";

  protected F form;

  protected List<DisplayHistoryEntry> historyEntries;

  protected boolean showHistory;

  protected boolean showModificationTimes = true;

  protected String i18nPrefix;

  protected WebMarkupContainer topMenuPanel;

  protected WebMarkupContainer bottomPanel;

  @SpringBean
  protected UserFormatter userFormatter;

  @SpringBean
  protected DateTimeFormatter dateTimeFormatter;

  @SpringBean
  private TenantChecker tenantChecker;

  @SpringBean
  private UserCache userCache;

  protected EditPageSupport<O, D, AbstractEditPage<O, F, D>> editPageSupport;

  public AbstractEditPage(final PageParameters parameters, final String i18nPrefix)
  {
    super(parameters);
    this.i18nPrefix = i18nPrefix;
  }

  protected void init()
  {
    init(null);
  }

  @SuppressWarnings({ "serial", "unchecked" })
  protected void init(O data)
  {
    final StringBuffer buf = new StringBuffer();
    buf.append("function showDeleteQuestionDialog() {\n").append("  return window.confirm('");

    //    boolean entWithHistory = HistoryServiceManager.get().getHistoryService().hasHistory(data.getClass());
    boolean entWithHistory = HistoryServiceManager.get().getHistoryService().hasHistory(getBaseDao().getEntityClass());
    showHistory = entWithHistory;
    if (entWithHistory == true) {
      buf.append(getString("question.markAsDeletedQuestion"));
    } else {
      buf.append(getString("question.deleteQuestion"));
    }
    buf.append("');\n}\n");
    body.add(new Label("showDeleteQuestionDialog", buf.toString()).setEscapeModelStrings(false));
    final Integer id = WicketUtils.getAsInteger(getPageParameters(), PARAMETER_KEY_ID);
    if (data == null) {
      if (id != null) {
        data = getBaseDao().getById(id);
      }
      if (data == null) {
        data = (O) WicketUtils.getAsObject(getPageParameters(), PARAMETER_KEY_DATA_PRESET,
            getBaseDao().getEntityClass());
        if (data == null) {
          data = getBaseDao().newInstance();
        }
        tenantChecker.setCurrentTenant(data);
      }
    }
    form = newEditForm(this, data);

    body.add(form);
    form.init();
    if (form.isNew() == true) {
      showHistory = false;
      showModificationTimes = false;
    }
    body.add(new Label("tabTitle", getTitle()).setRenderBodyOnly(true));
    final List<IColumn<DisplayHistoryEntry, String>> columns = new ArrayList<IColumn<DisplayHistoryEntry, String>>();
    final CellItemListener<DisplayHistoryEntry> cellItemListener = new CellItemListener<DisplayHistoryEntry>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<DisplayHistoryEntry>> item, final String componentId,
          final IModel<DisplayHistoryEntry> rowModel)
      {
        // Later a link should show the history entry as popup.
        item.add(AttributeModifier.append("class", new Model<String>("notrlink")));
      }
    };
    final DatePropertyColumn<DisplayHistoryEntry> timestampColumn = new DatePropertyColumn<DisplayHistoryEntry>(
        dateTimeFormatter,
        getString("timestamp"), null, "timestamp", cellItemListener);
    timestampColumn.setDatePattern(DateFormats.getFormatString(DateFormatType.DATE_TIME_SHORT_MINUTES));
    columns.add(timestampColumn);
    columns
        .add(new UserPropertyColumn<DisplayHistoryEntry>(userCache, getString("user"), null, "user", cellItemListener)
            .withUserFormatter(userFormatter));
    columns
        .add(new CellItemListenerPropertyColumn<DisplayHistoryEntry>(getString("history.entryType"), null, "entryType",
            cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<DisplayHistoryEntry>(getString("history.propertyName"), null, "propertyName",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<DisplayHistoryEntry>(getString("history.newValue"), null, "newValue",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<DisplayHistoryEntry>> item, final String componentId,
          final IModel<DisplayHistoryEntry> rowModel)
      {
        final DisplayHistoryEntry historyEntry = rowModel.getObject();
        item.add(
            new DiffTextPanel(componentId, Model.of(historyEntry.getNewValue()), Model.of(historyEntry.getOldValue())));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    final IDataProvider<DisplayHistoryEntry> dataProvider = new ListDataProvider<DisplayHistoryEntry>(getHistory());
    final DataTable<DisplayHistoryEntry, String> dataTable = new DataTable<DisplayHistoryEntry, String>("historyTable",
        columns,
        dataProvider, 100)
    {
      @Override
      protected Item<DisplayHistoryEntry> newRowItem(final String id, final int index,
          final IModel<DisplayHistoryEntry> model)
      {
        return new OddEvenItem<DisplayHistoryEntry>(id, index, model);
      }

      @Override
      public boolean isVisible()
      {
        return showHistory;
      }
    };
    final HeadersToolbar<String> headersToolbar = new HeadersToolbar<String>(dataTable, null);
    dataTable.addTopToolbar(headersToolbar);
    body.add(dataTable);
    final Label timeOfCreationLabel = new Label("timeOfCreation",
        dateTimeFormatter.getFormattedDateTime(data.getCreated()));
    timeOfCreationLabel.setRenderBodyOnly(true);
    body.add(timeOfCreationLabel);
    final Label timeOfLastUpdateLabel = new Label("timeOfLastUpdate",
        dateTimeFormatter.getFormattedDateTime(data.getLastUpdate()));
    timeOfLastUpdateLabel.setRenderBodyOnly(true);
    body.add(timeOfLastUpdateLabel);
    onPreEdit();
    evaluateInitialPageParameters(getPageParameters());
    this.editPageSupport = new EditPageSupport<>(this, getBaseDao());
  }

  protected List<DisplayHistoryEntry> getHistory()
  {
    if (historyEntries == null) {
      historyEntries = getBaseDao().getDisplayHistoryEntries(getData());
    }
    return historyEntries;
  }

  /**
   * Override this method if some initial data or fields have to be set. onPreEdit will be called on both, on adding new
   * data objects and on updating existing data objects. The decision on adding or updating depends on getData().getId()
   * != null.
   */
  protected void onPreEdit()
  {
  }

  /**
   * Will be called before the data object will be stored. Does nothing at default. Any return value is not yet
   * supported.
   */
  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    // Do nothing at default
    return null;
  }

  /**
   * Will be called before the data object will be deleted or marked as deleted. Here you can add validation errors
   * manually. If this method returns a resolution then a redirect to this resolution without calling the baseDao
   * methods will done. <br/>
   * Here you can do validations with add(Global)Error or manipulate the data object before storing to the database etc.
   */
  @Override
  public AbstractSecuredBasePage onDelete()
  {
    // Do nothing at default
    return null;
  }

  /**
   * Will be called before the data object will be restored (undeleted). Here you can add validation errors manually. If
   * this method returns a resolution then a redirect to this resolution without calling the baseDao methods will done.
   * <br/>
   * Here you can do validations with add(Global)Error or manipulate the data object before storing to the database etc.
   */
  @Override
  public AbstractSecuredBasePage onUndelete()
  {
    // Do nothing at default
    return null;
  }

  /**
   * Will be called directly after storing the data object (insert, update, delete). If any page is returned then
   * proceed a redirect to this given page.
   */
  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    // Do nothing at default.
    return null;
  }

  /**
   * Will be called directly after storing the data object (insert). Any return value is not yet supported.
   */
  @Override
  public AbstractSecuredBasePage afterSave()
  {
    // Do nothing at default.
    return null;
  }

  /**
   * Will be called directly after storing the data object (update).
   * 
   * @param modificationStatus MINOR or MAJOR, if the object was modified, otherwise NONE. If a not null web page is
   *          returned, then the web page will be set as response page.
   * @see BaseDao#update(ExtendedBaseDO)
   */
  @Override
  public AbstractSecuredBasePage afterUpdate(final ModificationStatus modificationStatus)
  {
    // Do nothing at default.
    return null;
  }

  /**
   * Will be called directly after deleting the data object (delete or update deleted=true). Any return value is not yet
   * supported.
   */
  @Override
  public WebPage afterDelete()
  {
    // Do nothing at default.
    return null;
  }

  /**
   * Will be called directly after un-deleting the data object (update deleted=false). Any return value is not yet
   * supported.
   */
  @Override
  public WebPage afterUndelete()
  {
    // Do nothing at default.
    return null;
  }

  /**
   * Will be called by clone button. Sets the id of the form data object to null and deleted to false.
   */
  protected void cloneData()
  {
    final O data = getData();
    getLogger().info("Clone of data chosen: " + data);
    data.setId(null);
    data.setDeleted(false);
  }

  /**
   * If user tried to add a new object and an error was occurred the edit page is shown again and the object id is
   * cleared (set to null).
   */
  @Override
  public void clearIds()
  {
    getData().setId(null);
  }

  @Override
  public void setResponsePageAndHighlightedRow(final WebPage page)
  {
    if (getData().getId() != null) {
      if (page instanceof AbstractListPage<?, ?, ?>) {
        // Force reload/refresh of calling AbstractListPage, otherwise the data object will not be updated.
        ((AbstractListPage<?, ?, ?>) page).setHighlightedRowId(getHighlightedRowId());
        ((AbstractListPage<?, ?, ?>) page).refresh();
      } else if (returnToPage instanceof TaskTreePage) {
        // Force reload/refresh of calling AbstractListPage, otherwise the data object will not be updated.
        ((TaskTreePage) page).setHighlightedRowId((Integer) getHighlightedRowId());
        ((TaskTreePage) page).refresh();
      } else if (returnToPage instanceof WizardPage) {
        ((WizardPage) returnToPage).setCreatedObject(getData());
      }
    }
    setResponsePage(page);
  }

  /**
   * Overwrite this, if getData().getId() should not be used.
   */
  protected Serializable getHighlightedRowId()
  {
    return getData().getId();
  }

  protected void cancel()
  {
    getLogger().debug("onCancel");
    setResponsePage();
  }

  /**
   * User has clicked the save button for storing a new item.
   */
  protected void create()
  {
    this.editPageSupport.create();
  }

  /**
   * User has clicked the update button for updating an existing item.
   */
  protected void update()
  {
    this.editPageSupport.update();
  }

  /**
   * User has clicked the update button for updating an existing item.
   */
  protected void updateAndNext()
  {
    this.editPageSupport.updateAndNext();
  }

  /**
   * @see EditPageSupport#updateAndStay()
   */
  protected void updateAndStay()
  {
    this.editPageSupport.updateAndStay();
  }

  protected void undelete()
  {
    this.editPageSupport.undelete();
  }

  protected void markAsDeleted()
  {
    this.editPageSupport.markAsDeleted();
  }

  protected void delete()
  {
    this.editPageSupport.delete();
  }

  protected void reset()
  {
    getLogger().debug("onReset");
    // Later: Clearing all fields and restoring data base object.
    throw new UnsupportedOperationException("Reset button not supported.");
  }

  /**
   * Sets the list page (declared as annotation) as response or, if given, the returnToPage.
   */
  @Override
  public void setResponsePage()
  {
    if (this.returnToPage != null) {
      setResponsePageAndHighlightedRow(this.returnToPage);
    } else {
      final EditPage ann = getClass().getAnnotation(EditPage.class);
      final Class<? extends WebPage> redirectPage;
      if (ann != null && ann.defaultReturnPage() != null) {
        redirectPage = getClass().getAnnotation(EditPage.class).defaultReturnPage();
      } else {
        redirectPage = WicketUtils.getDefaultPage();
      }
      final PageParameters params = new PageParameters();
      if (getData().getId() != null) {
        params.add(AbstractListPage.PARAMETER_HIGHLIGHTED_ROW, getData().getId());
      }
      setResponsePage(redirectPage, params);
    }
  }

  /**
   * @return false, if not overridden.
   */
  public boolean isUpdateAndNextSupported()
  {
    return false;
  }

  /**
   * Convenience method.
   * 
   * @see AbstractEditForm#getData()
   */
  @Override
  public O getData()
  {
    if (form == null || form.getData() == null) {
      getLogger()
          .error("Data of form is null. Maybe you have forgotten to call AbstractEditPage.init() in constructor.");
    }
    return form.getData();
  }

  /**
   * Checks weather the id of the data object is given or not.
   * 
   * @return true if the user wants to create a new data object or false for an already existing object.
   */
  @Override
  public boolean isNew()
  {
    if (form == null) {
      getLogger()
          .error("Data of form is null. Maybe you have forgotten to call AbstractEditPage.init() in constructor.");
    }
    return (getData() == null || getData().getId() == null);
  }

  /**
   * Calls getString(key) with key "[i18nPrefix].title.edit" or "[i18nPrefix].title.add" dependent weather the data
   * object is already existing or new.
   * 
   * @see org.projectforge.web.wicket.AbstractUnsecurePage#getTitle()
   */
  @Override
  protected String getTitle()
  {
    return getString(getTitleKey(i18nPrefix, isNew()));
  }

  /**
   * @param i18nPrefix
   * @param isNew
   * @return i18nPrefix + ".title.add" if isNew is true or i18nPrefix + ".title.edit" otherwise.
   */
  public static String getTitleKey(final String i18nPrefix, final boolean isNew)
  {
    if (isNew == true) {
      return i18nPrefix + ".title.add";
    } else {
      return i18nPrefix + ".title.edit";
    }
  }

  /**
   * Removes id from the initial parameters set.
   * 
   * @see org.projectforge.web.wicket.AbstractSecuredPage#getBookmarkableInitialParameters()
   */
  @Override
  public PageParameters getBookmarkableInitialParameters()
  {
    if (isNew() == true) {
      return new PageParameters();
    }
    final PageParameters pageParameters = super.getBookmarkableInitialParameters();
    pageParameters.remove("id"); // Don't show id if other extended parameters are given.
    return pageParameters;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredPage#getDataObjectForInitialParameters()
   */
  @Override
  protected Object getDataObjectForInitialParameters()
  {
    return getData();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSecuredPage#getTitleKey4BookmarkableInitialParameters()
   */
  @Override
  public String getTitleKey4BookmarkableInitialParameters()
  {
    return "bookmark.directPageExtendedLink.editPage";
  }

  @Override
  public boolean isAlreadySubmitted()
  {
    return alreadySubmitted;
  }

  @Override
  public void setAlreadySubmitted(final boolean alreadySubmitted)
  {
    this.alreadySubmitted = alreadySubmitted;
  }

  /**
   * @return the form
   */
  public F getForm()
  {
    return form;
  }

  protected abstract D getBaseDao();

  protected abstract Logger getLogger();

  protected abstract F newEditForm(AbstractEditPage<?, ?, ?> parentPage, O data);
}
