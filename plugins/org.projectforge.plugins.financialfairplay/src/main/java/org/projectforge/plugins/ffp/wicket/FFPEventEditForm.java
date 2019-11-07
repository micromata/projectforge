/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.ffp.wicket;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.PFUserFilter;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.IdObject;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.MyBeanComparator;
import org.projectforge.plugins.ffp.model.FFPAccountingDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.projectforge.plugins.ffp.repository.FFPEventService;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.user.UsersProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.*;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;
import org.wicketstuff.select2.Select2MultiChoice;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public class FFPEventEditForm extends AbstractEditForm<FFPEventDO, FFPEventEditPage>
{
  private static final long serialVersionUID = 8746545908106124484L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FFPEventEditForm.class);

  @SpringBean
  private UserDao userDao;

  @SpringBean
  private FFPEventService eventService;

  protected MultiChoiceListHelper<PFUserDO> assignAttendeesListHelper;

  public FFPEventEditForm(final FFPEventEditPage parentPage, final FFPEventDO data)
  {
    super(parentPage, data);
  }

  private TablePanel tablePanel;

  private DataTable<FFPAccountingDO, String> dataTable;

  protected Set<FFPAccountingDO> accountingList = new HashSet<>();

  private SingleButtonPanel finishButtonPanel;

  private PFUserDO currentUser;

  @Override
  protected void init()
  {
    super.init();
    currentUser = ThreadLocalUserContext.getUser();
    if (data.getOrganizer() == null) {
      if (currentUser == null) {
        error(I18nHelper.getLocalizedMessage("plugins.ffp.validate.noUser"));
        return;
      } else {
        data.setOrganizer(currentUser);
      }
    }

    if (isNew()) {
      if (currentUser != null) {
        this.accountingList.add(getNewFfpAccountingDO(currentUser));
      }
    }

    IFormValidator formValidator = new IFormValidator()
    {

      // Components for form validation.
      private final FormComponent<?>[] dependentFormComponents = new FormComponent[1];

      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return dependentFormComponents;
      }

      @Override
      public void validate(Form<?> form)
      {
        Select2MultiChoice<PFUserDO> attendeesSelect2 = (Select2MultiChoice<PFUserDO>) dependentFormComponents[0];
        Collection<PFUserDO> attendeeList = attendeesSelect2.getConvertedInput();
        if (attendeeList != null && attendeeList.size() < 2) {
          error(I18nHelper.getLocalizedMessage("plugins.ffp.validate.minAttendees"));
        }
      }
    };

    add(formValidator);

    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL100);
    {
      // Organizer
      final FieldsetPanel fs = gridBuilder.newFieldset(FFPEventDO.class, "organizer");
      fs.add(new DivTextPanel(fs.newChildId(), data.getOrganizer().getFullname()));
    }
    {
      // Event date
      final FieldsetPanel fs = gridBuilder.newFieldset(FFPEventDO.class, "eventDate");
      DatePanel eventDate = new DatePanel(fs.newChildId(), new PropertyModel<>(data, "eventDate"),
          DatePanelSettings.get().withTargetType(java.sql.Date.class), true);
      eventDate.setRequired(true);
      eventDate.setMarkupId("eventDate").setOutputMarkupId(true);
      eventDate.setEnabled(!getData().getFinished());
      fs.add(eventDate);
    }
    {
      // Division
      final FieldsetPanel fs = gridBuilder.newFieldset(FFPEventDO.class, "title");
      MaxLengthTextField titel = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "title"));
      titel.setRequired(true);
      titel.setMarkupId("eventTitel").setOutputMarkupId(true);
      titel.setEnabled(!getData().getFinished());
      fs.add(titel);
    }
    {
      // ATTENDEES
      final FieldsetPanel fieldSet = gridBuilder.newFieldset(getString("plugins.ffp.attendees"));
      assignAttendeesListHelper = new MultiChoiceListHelper<PFUserDO>()
          .setComparator(new Comparator<PFUserDO>()
          {

            @Override
            public int compare(PFUserDO o1, PFUserDO o2)
            {
              return o1.getPk().compareTo(o2.getPk());
            }

          }).setFullList(userDao.getList(new PFUserFilter().setDeactivatedUser(false)));

      if (this.data.getAttendeeList() != null && this.data.getAttendeeList().size() > 0) {
        for (final PFUserDO attendee : this.data.getAttendeeList()) {
          assignAttendeesListHelper.addOriginalAssignedItem(attendee).assignItem(attendee);
        }
      }
      if (this.accountingList != null && this.accountingList.size() > 0) {
        for (final FFPAccountingDO accounting : this.accountingList) {
          assignAttendeesListHelper.assignItem(accounting.getAttendee());
        }
      }

      final Select2MultiChoice<PFUserDO> attendees = new Select2MultiChoice<>(
          fieldSet.getSelect2MultiChoiceId(),
          new PropertyModel<>(this.assignAttendeesListHelper, "assignedItems"),
          new UsersProvider(userDao));
      attendees.setRequired(true).setMarkupId("attendees").setOutputMarkupId(true);
      attendees.add(new AjaxEventBehavior(OnChangeAjaxBehavior.EVENT_NAME)
      {
        protected final FormComponent<?> getFormComponent()
        {
          return (FormComponent<?>) getComponent();
        }

        @Override
        protected void onEvent(AjaxRequestTarget target)
        {
          final FormComponent<?> formComponent = getFormComponent();
          try {
            formComponent.inputChanged();
            formComponent.validate();
            if (formComponent.hasErrorMessage()) {
              formComponent.invalid();
              accountingList.clear();
              assignAttendeesListHelper.getAssignedItems().clear();
            } else {
              formComponent.valid();
              formComponent.updateModel();
            }
            dataTable = createDataTable(createColumns(), "attendee.fullname", SortOrder.ASCENDING, getData());
            tablePanel.addOrReplace(dataTable);
            target.add(dataTable);
          } catch (RuntimeException e) {
            throw e;
          }
        }
      });
      attendees.setEnabled(!getData().getFinished());
      formValidator.getDependentFormComponents()[0] = attendees;
      fieldSet.add(attendees);
    }

    //Transactions
    createDataTable(gridBuilder);

    {
      Button finishButton = new Button("button", new Model<>("plugins.ffp.finishEvent"))
      {
        @Override
        public final void onSubmit()
        {
          try {
            getData().setFinished(true);
            parentPage.createOrUpdate();
          } catch (final UserException ex) {
            error(parentPage.translateParams(ex));
          }
        }
      };
      finishButton.setMarkupId("finishEvent").setOutputMarkupId(true);
      finishButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), finishButton, getString("plugins.ffp.finishEvent"),
          SingleButtonPanel.SUCCESS);
      finishButtonPanel.setVisible(false);
      actionButtons.add(finishButtonPanel);
    }
  }

  @Override
  protected void updateButtonVisibility()
  {
    super.updateButtonVisibility();
    if (getData().getFinished() || getData().getOrganizer() == null) {
      markAsDeletedButtonPanel.setVisible(false);
      deleteButtonPanel.setVisible(false);
      updateButtonPanel.setVisible(false);
      updateAndStayButtonPanel.setVisible(false);
      updateAndNextButtonPanel.setVisible(false);
      createButtonPanel.setVisible(false);
      undeleteButtonPanel.setVisible(false);
      if (finishButtonPanel != null) {
        finishButtonPanel.setVisible(false);
      }
    }
    if (!getData().getFinished() && getData().getOrganizer() != null && currentUser != null
        && getData().getOrganizer().getId().equals(currentUser.getId())) {
      finishButtonPanel.setVisible(true);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  public void createDataTable(GridBuilder gridBuilder)
  {
    DivPanel section = gridBuilder.getPanel();
    this.tablePanel = new TablePanel(section.newChildId());
    this.tablePanel.setOutputMarkupId(true);
    section.add(tablePanel);
    this.dataTable = createDataTable(createColumns(), "attendee.fullname", SortOrder.ASCENDING, getData());
    tablePanel.add(this.dataTable);
  }

  private DataTable<FFPAccountingDO, String> createDataTable(final List<IColumn<FFPAccountingDO, String>> columns,
      final String sortProperty, final SortOrder sortOrder, final FFPEventDO event)
  {
    final SortParam<String> sortParam = sortProperty != null
        ? new SortParam<>(sortProperty, sortOrder == SortOrder.ASCENDING) : null;
    DefaultDataTable<FFPAccountingDO, String> localDataTable = new DefaultDataTable<>(TablePanel.TABLE_ID, columns,
        createSortableDataProvider(sortParam, event), 50);
    localDataTable.setOutputMarkupId(true);
    localDataTable.setMarkupId("attendeeDataTable");
    return localDataTable;
  }

  private ISortableDataProvider<FFPAccountingDO, String> createSortableDataProvider(final SortParam<String> sortParam,
      FFPEventDO event)
  {
    return new FFPAccountingPageSortableDataProvider<FFPAccountingDO>(sortParam, getAccountings(), event);
  }

  public Set<FFPAccountingDO> getAccountings()
  {
    //Existing attendee data
    if (getData().getAccountingList() != null && getData().getAccountingList().size() > 0) {
      getData().getAccountingList().forEach(acc -> {
        this.accountingList.add(acc);
      });
    }
    //New added attendee data
    if (assignAttendeesListHelper != null && assignAttendeesListHelper.getItemsToAssign() != null) {
      assignAttendeesListHelper.getItemsToAssign().forEach(emp -> {
        this.accountingList.add(getNewFfpAccountingDO(emp));
      });
    }
    //Removed attendee data
    if (assignAttendeesListHelper != null && assignAttendeesListHelper.getItemsToUnassign() != null) {
      assignAttendeesListHelper.getItemsToUnassign().forEach(emp -> {
        Set<FFPAccountingDO> toRemove = new HashSet<>();
        this.accountingList.forEach(acc -> {
          if (acc.getAttendee().getPk().equals(emp.getPk())) {
            toRemove.add(acc);
          }
        });
        this.accountingList.removeAll(toRemove);
      });
    }
    if (assignAttendeesListHelper != null && assignAttendeesListHelper.getAssignedItems() != null) {
      Set<FFPAccountingDO> toRemove = new HashSet<>();
      this.accountingList.forEach(acc -> {
        boolean found = false;
        for (PFUserDO user : assignAttendeesListHelper.getAssignedItems()) {
          if (user.getId().equals(acc.getAttendee().getId())) {
            found = true;
          }
        }
        if (!found) {
          toRemove.add(acc);
        }
      });
      this.accountingList.removeAll(toRemove);
    }
    return this.accountingList;
  }

  private FFPAccountingDO getNewFfpAccountingDO(PFUserDO user)
  {
    FFPAccountingDO accounting = new FFPAccountingDO();
    accounting.setEvent(getData());
    accounting.setAttendee(user);
    accounting.setValue(BigDecimal.ZERO);
    accounting.setWeighting(BigDecimal.ONE);
    return accounting;
  }

  private List<IColumn<FFPAccountingDO, String>> createColumns()
  {
    final List<IColumn<FFPAccountingDO, String>> columns = new ArrayList<>();
    columns.add(new PropertyColumn<>(new ResourceModel("name"), "attendee.fullname"));
    columns.add(new CellItemListenerPropertyColumn<FFPAccountingDO>(FFPAccountingDO.class, "plugins.ffp.value", "value", null)
    {
      private static final long serialVersionUID = 3672950740712610620L;

      @Override
      public void populateItem(Item<ICellPopulator<FFPAccountingDO>> item, String componentId,
          IModel<FFPAccountingDO> rowModel)
      {
        MinMaxNumberField<BigDecimal> field = new MinMaxNumberField<>(InputPanel.WICKET_ID,
            new PropertyModel<>(rowModel.getObject(), "value"),
            new BigDecimal(0), new BigDecimal(Integer.MAX_VALUE));
        field.setRequired(true);
        InputPanel input = new InputPanel(componentId, field);
        input.setEnabled(!rowModel.getObject().getEvent().getFinished());
        item.add(input);
      }

    });
    columns.add(new CellItemListenerPropertyColumn<FFPAccountingDO>(FFPAccountingDO.class, "plugins.ffp.weighting", "weighting", null)
    {
      private static final long serialVersionUID = 367295074123610620L;

      @Override
      public void populateItem(Item<ICellPopulator<FFPAccountingDO>> item, String componentId,
          IModel<FFPAccountingDO> rowModel)
      {
        MinMaxNumberField field = new MinMaxNumberField<>(InputPanel.WICKET_ID,
            new PropertyModel<>(rowModel.getObject(), "weighting"),
            new BigDecimal(0), new BigDecimal(Integer.MAX_VALUE));
        field.setRequired(true);
        InputPanel input = new InputPanel(componentId, field);
        input.setEnabled(!rowModel.getObject().getEvent().getFinished());
        item.add(input);
      }

    });
    columns.add(new CellItemListenerPropertyColumn<FFPAccountingDO>(FFPAccountingDO.class, "plugins.ffp.comment", "comment", null)
    {
      private static final long serialVersionUID = 367295012323610620L;

      @Override
      public void populateItem(Item<ICellPopulator<FFPAccountingDO>> item, String componentId,
          IModel<FFPAccountingDO> rowModel)
      {
        InputPanel input = new InputPanel(componentId,
            new MaxLengthTextField(InputPanel.WICKET_ID,
                new PropertyModel<>(rowModel.getObject(), "comment")));
        input.setEnabled(!rowModel.getObject().getEvent().getFinished());
        item.add(input);
      }

    });
    return columns;
  }

  private class FFPAccountingPageSortableDataProvider<T extends IdObject<?>>
      extends SortableDataProvider<FFPAccountingDO, String>
  {
    private static final long serialVersionUID = 1517715512369991765L;

    /**
     * Complete list is needed every time the sort parameters or filter settings were changed.
     */
    private List<FFPAccountingDO> completeList;

    private List<FFPAccountingDO> eventAttendeeList;

    /**
     * Stores only the id's of the result set.
     */
    private List<Serializable> idList;

    private Long first, count;

    private SortParam<String> sortParam;

    private SortParam<String> secondSortParam;

    private FFPEventDO event;

    public FFPAccountingPageSortableDataProvider(final SortParam<String> sortParam, Set<FFPAccountingDO> eventAttendeeList, FFPEventDO event)
    {
      this.eventAttendeeList = new ArrayList<>(eventAttendeeList);
      this.event = event;
      // set default sort
      if (sortParam != null) {
        setSort(sortParam);
      } else {
        setSort("NOSORT", SortOrder.ASCENDING);
      }
    }

    public FFPAccountingPageSortableDataProvider<T> setCompleteList(final List<FFPAccountingDO> completeList)
    {
      this.completeList = completeList;
      this.idList = new LinkedList<>();
      if (this.completeList != null) {
        sortList(this.completeList);
        for (final FFPAccountingDO entry : completeList) {
          this.idList.add(entry.getId());
        }
      }
      return this;
    }

    @Override
    public Iterator<FFPAccountingDO> iterator(final long first, final long count)
    {
      if ((this.first != null && this.first != first) || (this.count != null && this.count != count)) {
        this.completeList = null; // Force to load all elements from data-base (avoid lazy initialization exceptions).
      }
      final SortParam<String> sp = getSort();
      if (!Objects.equals(sortParam, sp)) {
        // The sort parameters were changed, force reload from data-base:
        reloadList();
      }
      this.first = first;
      this.count = count;
      if (idList == null) {
        return null;
      }
      List<FFPAccountingDO> result;
      int fromIndex = (int) first;
      if (fromIndex < 0) {
        fromIndex = 0;
      }
      int toIndex = (int) (first + count);
      if (this.completeList != null) {
        // The completeList is already load, don't need to load objects from data-base:
        result = completeList;
        if (toIndex > idList.size()) {
          toIndex = idList.size();
        }
        result = new LinkedList<>();
        for (final FFPAccountingDO entry : completeList.subList(fromIndex, toIndex)) {
          result.add(entry);
        }
        this.completeList = null; // Don't store the complete list on the server anymore.
        return result.iterator();
      } else {
        if (toIndex > idList.size()) {
          toIndex = idList.size();
        }

        final List<FFPAccountingDO> list = this.eventAttendeeList;
        sortList(list);
        return list.iterator();
      }
    }

    protected Comparator<FFPAccountingDO> getComparator(final SortParam<String> sortParam,
        final SortParam<String> secondSortParam)
    {
      final String sortProperty = sortParam != null ? sortParam.getProperty() : null;
      final boolean ascending = sortParam != null ? sortParam.isAscending() : true;
      final String secondSortProperty = secondSortParam != null ? secondSortParam.getProperty() : null;
      final boolean secondAscending = secondSortParam != null ? secondSortParam.isAscending() : true;
      return new MyBeanComparator<>(sortProperty, ascending, secondSortProperty, secondAscending);
    }

    /**
     * @see org.apache.wicket.markup.repeater.data.IDataProvider#size()
     */
    @Override
    public long size()
    {
      if (idList == null) {
        reloadList();
      }
      return this.idList != null ? this.idList.size() : 0;
    }

    private void reloadList()
    {
      setCompleteList(this.eventAttendeeList);
    }

    private void sortList(final List<FFPAccountingDO> list)
    {
      final SortParam<String> sp = getSort();
      if (sp != null && !"NOSORT".equals(sp.getProperty())) {
        if (this.sortParam != null && !StringUtils.equals(this.sortParam.getProperty(), sp.getProperty())) {
          this.secondSortParam = this.sortParam;
        }
        final Comparator<FFPAccountingDO> comp = getComparator(sp, secondSortParam);
        list.sort(comp);
      }
      this.sortParam = sp;
    }

    @Override
    public IModel<FFPAccountingDO> model(final FFPAccountingDO object)
    {
      return new Model<>(object);
    }

    /**
     * @see ISortableDataProvider#detach()
     */
    @Override
    public void detach()
    {
      this.completeList = null;
    }

  }
}
