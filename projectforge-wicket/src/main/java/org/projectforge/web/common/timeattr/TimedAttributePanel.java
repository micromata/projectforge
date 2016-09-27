package org.projectforge.web.common.timeattr;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.web.dialog.ModalQuestionDialog;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.converter.MyDateConverter;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import de.micromata.genome.db.jpa.tabattr.api.AttrGroup.DayMonthGranularity;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;
import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import de.micromata.genome.util.types.DateUtils;

public class TimedAttributePanel<PK extends Serializable, T extends TimeableAttrRow<PK>> extends BaseAttributePanel
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TimedAttributePanel.class);

  private static final MyDateConverter dateConverter = new MyDateConverter("M-"); // same style as the date panel

  @SpringBean
  private TimeableService timeableService;

  private final EntityWithTimeableAttr<PK, T> entity;
  private final Function<AttrGroup, T> addNewEntryFunction;
  private final Model<T> selectedAttrRowModel = new Model<>();
  private T oldSelectedAttrRow = null;
  private T newSelectedAttrRow = null;
  private T backupOfDeletedAttrRow = null;
  private boolean saveChangesDialogExitedWithYes = false;
  private boolean isDirty = false;

  private final AbstractEditPage<?, ?, ?> parentPage;
  private final ModalQuestionDialog saveChangesDialog;
  private final ModalQuestionDialog deleteDialog;
  private final DropDownChoice<T> dateDropDown;

  public TimedAttributePanel(final String id, final AttrGroup attrGroup, final EntityWithTimeableAttr<PK, T> entity,
      final AbstractEditPage<?, ?, ?> parentPage,
      final Function<AttrGroup, T> addNewEntryFunction)
  {
    super(id, attrGroup);
    this.entity = entity;
    this.parentPage = parentPage;
    this.addNewEntryFunction = addNewEntryFunction;

    saveChangesDialog = createSaveChangesDialog(parentPage);
    deleteDialog = createDeleteDialog(parentPage);

    dateDropDown = createDateDropDown();
    container.add(dateDropDown);
    container.add(createAddButton());
    container.add(createDeleteButton());
  }

  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    final boolean valid = isWholePageValid();

    if (valid) {
      if (saveChangesDialogExitedWithYes) {
        if (newSelectedAttrRow != null) {
          // the user selected an other attr row from the dropdown
          selectedAttrRowModel.setObject(newSelectedAttrRow);
        } else {
          // the user clicked the add button
          addAndSelectNewEntry();
        }
      }

      // if a row was deleted, we have to select a new entry in the dropdown
      final boolean selectRowForCurrentDate = (backupOfDeletedAttrRow != null);
      // ensure that the order of the dates in the drop down is correct after adding a new or changing an existing entry
      updateChoicesOfDateDropDown(selectRowForCurrentDate);

      // this ensures that the content is always synced with the selected value in the dropdown
      container.addOrReplace(createContentWithDatepicker());
    } else if (backupOfDeletedAttrRow != null) {
      // not valid and we deleted a row:
      // restore the backup of this row and create the content again
      entity.getTimeableAttributes().add(backupOfDeletedAttrRow);
      container.addOrReplace(createContentWithDatepicker());
    }

    final T selectedAttrRow = selectedAttrRowModel.getObject();
    if (valid && (selectedAttrRow == null || selectedAttrRow.getPk() != null)) {
      // reset the dirty flag only if the whole page is valid and the row is not new (pk != null)
      isDirty = false;
    }

    // always reset this flag, whether the data is valid or not, otherwise this could lead to problems if the user refreshes the page after a submit with invalid data
    saveChangesDialogExitedWithYes = false;

    // whether the backup was used or not, at this this point we have to delete it, otherwise it could be restored next time by mistake
    backupOfDeletedAttrRow = null;
  }

  private void updateChoicesOfDateDropDown(final boolean selectRowForCurrentDate)
  {
    final List<T> attrRowsSorted = getTimeableAttrRowsOfThisGroupSorted();
    if (selectRowForCurrentDate) {
      final T attrRowForCurrentDate = getAttrRowForCurrentDate(attrRowsSorted);
      selectedAttrRowModel.setObject(attrRowForCurrentDate);
    }
    dateDropDown.setChoices(attrRowsSorted);
  }

  private DropDownChoice<T> createDateDropDown()
  {
    final List<T> attrRowsSorted = getTimeableAttrRowsOfThisGroupSorted();
    final T attrRowForCurrentDate = getAttrRowForCurrentDate(attrRowsSorted);
    selectedAttrRowModel.setObject(attrRowForCurrentDate);

    final DropDownChoice<T> dropDown = new DropDownChoice<>(
        "dateSelect",
        selectedAttrRowModel,
        attrRowsSorted,
        new IChoiceRenderer<T>()
        {
          @Override
          public Object getDisplayValue(T attrRow)
          {
            final Date startTime = attrRow.getStartTime();
            return dateConverter.convertToString(startTime, null);
          }

          @Override
          public String getIdValue(T attrRow, int index)
          {
            return String.valueOf(index);
          }
        });

    dropDown.add(new AjaxFormComponentUpdatingBehavior("change")
    {
      @Override
      protected void onUpdate(AjaxRequestTarget target)
      {
        if (isDirty) {
          // set date drop down to old value and save new value in variable for later use in onBeforeRender method if the user accepts the modal
          newSelectedAttrRow = selectedAttrRowModel.getObject();
          selectedAttrRowModel.setObject(oldSelectedAttrRow);
          target.add(dropDown);
          saveChangesDialog.open(target);
        } else {
          container.addOrReplace(createContentWithDatepicker());
          target.add(container);
        }
      }
    });

    dropDown.setMarkupId(attrGroup.getName() + "-dateDropDown").setOutputMarkupId(true);
    return dropDown;
  }

  private List<T> getTimeableAttrRowsOfThisGroup()
  {
    return timeableService.getTimeableAttrRowsForGroup(entity, attrGroup);
  }

  private List<T> getTimeableAttrRowsOfThisGroupSorted()
  {
    return timeableService.sortTimeableAttrRowsByDateDescending(getTimeableAttrRowsOfThisGroup());
  }

  private T getAttrRowForCurrentDate(final List<T> attrRows)
  {
    return timeableService.getAttrRowForDate(attrRows, attrGroup, new Date());
  }

  private Button createAddButton()
  {
    final Button addButton = new Button("addButton");
    addButton.add(new AjaxEventBehavior("click")
    {
      @Override
      protected void onEvent(AjaxRequestTarget target)
      {
        if (!isDirty) {
          addAndSelectNewEntry();
          updateChoicesOfDateDropDown(false);
          container.addOrReplace(createContentWithDatepicker());
          target.add(container);
          // a new attr row must be dirty because it is new and must be saved
          isDirty = true;
        } else {
          // this indicates that the add button was clicked in onBeforeRender
          newSelectedAttrRow = null;
          saveChangesDialog.open(target);
        }
      }
    });

    addButton.setMarkupId(attrGroup.getName() + "-addButton").setOutputMarkupId(true);
    return addButton;
  }

  private void addAndSelectNewEntry()
  {
    T newSelectedAttrRow = addNewEntryFunction.apply(attrGroup);
    selectedAttrRowModel.setObject(newSelectedAttrRow);
  }

  private Button createDeleteButton()
  {
    final Button deleteButton = new Button("deleteButton");
    deleteButton.add(new AjaxEventBehavior("click")
    {
      @Override
      protected void onEvent(AjaxRequestTarget target)
      {
        // open dialog only if an attr row is selected
        if (selectedAttrRowModel.getObject() != null) {
          deleteDialog.open(target);
        }
      }
    });

    deleteButton.setMarkupId(attrGroup.getName() + "-deleteButton").setOutputMarkupId(true);
    return deleteButton;
  }

  private Component createContentWithDatepicker()
  {
    final T selectedAttrRow = selectedAttrRowModel.getObject();

    // save the new select value in variable to restore it in case the form is dirty next time
    oldSelectedAttrRow = selectedAttrRow;

    if (selectedAttrRow == null) {
      // create an empty content area, just to satisfy wicket id, this is filled later with the real content.
      return new WebMarkupContainer("content");
    }

    final Consumer<GridBuilder> datePickerCreator = gridBuilder -> createDatepicker(selectedAttrRow, gridBuilder);
    final DivPanel mainContainer = createContent(selectedAttrRow, datePickerCreator);
    addDirtyCheckToFormComponents(mainContainer);

    return mainContainer;
  }

  private void createDatepicker(T attrRow, GridBuilder gridBuilder)
  {
    final String startTimeLabel = getString(attrGroup.getI18nKeyStartTime());
    final FieldsetPanel dateFs = gridBuilder.newFieldset(startTimeLabel);
    final PropertyModel<Date> dateModel = new PropertyModel<>(attrRow, "startTime");
    final DatePanel dp = new DatePanel(dateFs.newChildId(), dateModel,
        DatePanelSettings.get().withTargetType(java.sql.Date.class));
    dp.setRequired(true);
    dp.add(this::validateDate);
    dateFs.add(dp);

    final DateTextField dateField = dp.getDateField();
    dateField.setMarkupId(attrGroup.getName() + "-startTime").setOutputMarkupId(true);
  }

  private void validateDate(IValidatable<Date> iValidatable)
  {
    final Date dateToValidate = iValidatable.getValue();
    final T selectedAttrRow = selectedAttrRowModel.getObject();

    DayMonthGranularity gran = attrGroup.getDayMonthGranularity();
    if (gran == null) {
      gran = DayMonthGranularity.DAY;
    }

    Predicate<T> predicate;
    String errorKey;

    switch (gran) {
      case MONTH:
        predicate = row -> DateUtils.isSameMonth(row.getStartTime(), dateToValidate);
        errorKey = "attr.starttime.alreadyexists.month";
        break;

      default:
        predicate = row -> DateHelper.isSameDay(row.getStartTime(), dateToValidate);
        errorKey = "attr.starttime.alreadyexists.day";
    }

    final boolean thereIsAlreadyAnEntryWithTheSameDate = getTimeableAttrRowsOfThisGroup()
        .stream()
        // remove the currently selected entry from the stream, otherwise this will always be true if you don't change the date
        .filter(row -> !row.equals(selectedAttrRow))
        .anyMatch(predicate);

    if (thereIsAlreadyAnEntryWithTheSameDate) {
      iValidatable.error(new ValidationError().addKey(errorKey));
    }
  }

  /**
   * Visits all children of type ComponentWrapperPanel, get their inner FormComponent and add a change listener which
   * sets the dirty flag.
   *
   * @param markupContainer The MarkupContainer whose children should be visited.
   */
  private void addDirtyCheckToFormComponents(final MarkupContainer markupContainer)
  {
    markupContainer.visitChildren(ComponentWrapperPanel.class, (component, iVisit) -> {
      FormComponent<?> formComponent = ((ComponentWrapperPanel) component).getFormComponent();
      formComponent.add(new AjaxEventBehavior("change")
      {
        @Override
        protected void onEvent(AjaxRequestTarget target)
        {
          isDirty = true;
        }
      });
    });
  }

  private ModalQuestionDialog createSaveChangesDialog(final AbstractEditPage<?, ?, ?> parentPage)
  {
    ModalQuestionDialog modal = new ModalQuestionDialog(
        parentPage.newModalDialogId(),
        new ResourceModel("attr.savemodal.heading"),
        new ResourceModel("attr.savemodal.question"))
    {
      @Override
      protected boolean onCloseButtonSubmit(final AjaxRequestTarget target)
      {
        final boolean result = super.onCloseButtonSubmit(target);

        // this flag is read after the submit
        saveChangesDialogExitedWithYes = true;

        // click the update and stay button which is invisible for the user
        target.appendJavaScript("$('#" + AbstractEditForm.UPDATE_AND_STAY_BUTTON_MARKUP_ID + "').click();");

        return result;
      }
    };
    parentPage.add(modal);
    modal.init();
    modal.setEscapeModelStringsInQuestion(false);
    return modal;
  }

  private ModalQuestionDialog createDeleteDialog(final AbstractEditPage<?, ?, ?> parentPage)
  {
    ModalQuestionDialog modal = new ModalQuestionDialog(
        parentPage.newModalDialogId(),
        new ResourceModel("attr.deletemodal.heading"),
        new ResourceModel("attr.deletemodal.question"))
    {
      @Override
      protected boolean onCloseButtonSubmit(final AjaxRequestTarget target)
      {
        final boolean result = super.onCloseButtonSubmit(target);

        // create a backup of the deleted attr row to restore it in case there is a validation error on submit
        backupOfDeletedAttrRow = selectedAttrRowModel.getObject();
        entity.getTimeableAttributes().remove(backupOfDeletedAttrRow);

        // clear the content area to avoid the validation of input which will be deleted
        container.addOrReplace(new WebMarkupContainer("content"));

        // click the update and stay button which is invisible for the user
        target.appendJavaScript("$('#" + AbstractEditForm.UPDATE_AND_STAY_BUTTON_MARKUP_ID + "').click();");

        return result;
      }
    };
    parentPage.add(modal);
    modal.init();
    modal.setEscapeModelStringsInQuestion(false);
    return modal;
  }

  private boolean isWholePageValid()
  {
    Boolean invalid = parentPage.visitChildren(FieldsetPanel.class, (FieldsetPanel fs, IVisit<Boolean> visit) -> {
      if (!fs.isValid()) {
        visit.stop(true);
      }
    });

    // returns true: invalid == false || invalid == null
    // returns false: invalid == true
    return !Boolean.TRUE.equals(invalid);
  }

}
