package org.projectforge.web.wicket;

import java.util.Date;

import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.TimePeriod;
import org.projectforge.web.CSSColor;
import org.projectforge.web.calendar.QuickSelectPanel;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.HtmlCommentPanel;
import org.projectforge.web.wicket.flowlayout.IconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Panel consists of two date pickers (start and end date) and a quick select for month and week.
 * For validation you can call the getConvertedInput method to get a TimePeriod which contains the start and end date.
 * <p>
 * It extends FormComponentPanel to have the validate() and getConvertedInput() method.
 */
public class TimePeriodPanel extends FormComponentPanel<TimePeriod> implements ISelectCallerPage
{
  private static final Logger log = LoggerFactory.getLogger(TimePeriodPanel.class);

  private final IModel<Date> startDateModel;

  private final IModel<Date> endDateModel;

  private final AbstractListPage<?, ?, ?> parentPage;

  private final DatePanel startDatePanel;

  private final DatePanel endDatePanel;

  public TimePeriodPanel(final String id, final IModel<Date> startDateModel, final IModel<Date> endDateModel, final AbstractListPage<?, ?, ?> parentPage)
  {
    // We have to pass a model just to satisfy the needs of the FormComponentPanel. The model is actually not used.
    super(id, new Model<>());

    this.startDateModel = startDateModel;
    this.endDateModel = endDateModel;
    this.parentPage = parentPage;

    startDatePanel = new DatePanel("startDate", startDateModel, DatePanelSettings.get().withSelectPeriodMode(true));
    add(startDatePanel);

    endDatePanel = new DatePanel("endDate", endDateModel, DatePanelSettings.get().withSelectPeriodMode(true));
    add(endDatePanel);

    // clear button
    final SubmitLink unselectPeriodLink = new SubmitLink(IconLinkPanel.LINK_ID)
    {
      @Override
      public void onSubmit()
      {
        startDateModel.setObject(null);
        endDateModel.setObject(null);
        refreshPage();
      }
    };
    unselectPeriodLink.setDefaultFormProcessing(false);

    final IconLinkPanel unselectLinkPanel = new IconLinkPanel("unselectLink", IconType.REMOVE_SIGN,
        new ResourceModel("calendar.tooltip.unselectPeriod"), unselectPeriodLink);
    unselectLinkPanel.setColor(CSSColor.RED);
    add(unselectLinkPanel);

    // quick select buttons
    final QuickSelectPanel quickSelectPanel = new QuickSelectPanel("quickSelect", this, "quickSelect", startDatePanel);
    add(quickSelectPanel);
    quickSelectPanel.init();

    // calendar week
    add(new DivTextPanel("calendarWeek",
        LambdaModel.of(() -> WicketUtils.getCalendarWeeks(this, startDateModel.getObject(), endDateModel.getObject()))
    ));

    // html comment
    add(new HtmlCommentPanel("htmlComment",
        LambdaModel.of(() -> WicketUtils.getUTCDates(startDateModel.getObject(), endDateModel.getObject()))
    ));
  }

  private void refreshPage()
  {
    startDatePanel.markModelAsChanged();
    endDatePanel.markModelAsChanged();
    parentPage.refresh();
  }

  @Override
  public void convertInput()
  {
    setConvertedInput(new TimePeriod(
        startDatePanel.getConvertedInput(),
        endDatePanel.getConvertedInput()
    ));
  }

  @Override
  public void validate()
  {
    super.validate(); // calls convertInput

    final Date start = startDatePanel.getConvertedInput();
    final Date end = endDatePanel.getConvertedInput();
    if (start != null && end != null && start.after(end)) {
      error(getString("timePeriodPanel.startTimeAfterStopTime"));
    }
  }

  /**
   * Returns the markup ID of the start date field.
   *
   * @return The markup ID of the start date field.
   */
  @Override
  public String getMarkupId()
  {
    return startDatePanel.getDateField().getMarkupId();
  }

  /**
   * This is called from the QuickSelectPanel, when the user clicks one of its buttons.
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if (property.startsWith("quickSelect.")) {
      final Date startDate = (Date) selectedValue;
      startDateModel.setObject(startDate);

      final DateHolder endDateHolder = new DateHolder(startDate);
      if (property.endsWith(".month") == true) {
        endDateHolder.setEndOfMonth();
      } else if (property.endsWith(".week") == true) {
        endDateHolder.setEndOfWeek();
      } else {
        log.error("Property '" + property + "' not supported for selection.");
      }
      endDateModel.setObject(endDateHolder.getDate());

      refreshPage();
    }
  }

  @Override
  public void unselect(final String property)
  {
    // unused
  }

  @Override
  public void cancelSelection(final String property)
  {
    // unused
  }
}
