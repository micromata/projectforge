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

package org.projectforge.plugins.skillmatrix;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.user.service.UserPreferencesHelper;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.utils.RecentQueue;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;

import java.util.List;
import java.util.Locale;

/**
 * This panel shows the actual user and buttons for select/unselect training.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TrainingSelectPanel extends AbstractSelectPanel<TrainingDO> implements ComponentWrapperPanel
{

  private static final long serialVersionUID = 5388613518793987520L;

  private static final String USER_PREF_KEY_RECENT_TRAININGS = "TrainingSelectPanel:recentTrainings";

  private boolean defaultFormProcessing = false;

  @SpringBean
  private TrainingDao trainingDao;

  private RecentQueue<String> recentTrainings;

  private final PFAutoCompleteTextField<TrainingDO> trainingTextField;

  // Only used for detecting changes:
  private TrainingDO currentTraining;

  /**
   * @param id
   * @param model
   * @param caller
   * @param selectProperty
   */
  @SuppressWarnings("serial")
  public TrainingSelectPanel(final String id, final IModel<TrainingDO> model, final ISelectCallerPage caller,
      final String selectProperty)
  {
    super(id, model, caller, selectProperty);
    trainingTextField = new PFAutoCompleteTextField<TrainingDO>("trainingField", getModel())
    {
      @Override
      protected List<TrainingDO> getChoices(final String input)
      {
        final BaseSearchFilter filter = new BaseSearchFilter();
        filter.setSearchFields("title", "description", "skill.title", "skill.description", "skill.comment");
        filter.setSearchString(input);
        final List<TrainingDO> list = trainingDao.getList(filter);
        return list;
      }

      @Override
      protected List<String> getRecentUserInputs()
      {
        return getRecentTrainings().getRecents();
      }

      @Override
      protected String formatLabel(final TrainingDO training)
      {
        if (training == null) {
          return "";
        }
        return formatTraining(training);
      }

      @Override
      protected String formatValue(final TrainingDO training)
      {
        if (training == null) {
          return "";
        }
        return formatTraining(training);
      }

      @Override
      protected String getTooltip()
      {
        final TrainingDO training = getModel().getObject();
        if (training == null || training.getSkill() == null || training.getSkill().getTitle() == null) {
          return null;
        }
        return training.getSkill().getTitle() + ", " + training.getTitle();
      }

      @Override
      public void convertInput()
      {
        final TrainingDO training = getConverter(getType()).convertToObject(getInput(), getLocale());
        setConvertedInput(training);
        if (training != null && (currentTraining == null || training.getId() != currentTraining.getId())) {
          getRecentTrainings().append(formatTraining(training));
        }
        currentTraining = training;
      }

      /**
       * @see org.apache.wicket.Component#getConverter(java.lang.Class)
       */
      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public <C> IConverter<C> getConverter(final Class<C> type)
      {
        return new IConverter()
        {
          @Override
          public Object convertToObject(final String value, final Locale locale)
          {
            if (StringUtils.isEmpty(value)) {
              getModel().setObject(null);
              return null;
            }
            // ### FORMAT ###
            final int ind = value.indexOf(" (");
            final String title = ind >= 0 ? value.substring(0, ind) : value;
            final TrainingDO training = trainingDao.getTraining(title);
            if (training == null) {
              trainingTextField.error(getString("plugins.skillmatrix.skilltraining.panel.error.trainingNotFound"));
            }
            getModel().setObject(training);
            return training;
          }

          @Override
          public String convertToString(final Object value, final Locale locale)
          {
            if (value == null) {
              return "";
            }
            final TrainingDO training = (TrainingDO) value;
            return training.getTitle();
          }

        };
      }
    };
    currentTraining = getModelObject();
    trainingTextField.enableTooltips().withLabelValue(true).withMatchContains(true).withMinChars(2).withAutoSubmit(true)
        .withWidth(400);
  }

  /**
   * @see org.apache.wicket.markup.html.form.FormComponent#setLabel(org.apache.wicket.model.IModel)
   */
  @Override
  public TrainingSelectPanel setLabel(final IModel<String> labelModel)
  {
    trainingTextField.setLabel(labelModel);
    super.setLabel(labelModel);
    return this;
  }

  @Override
  public TrainingSelectPanel init()
  {
    super.init();
    add(trainingTextField);
    return this;
  }

  public void markTextFieldModelAsChanged()
  {
    trainingTextField.modelChanged();
    final TrainingDO training = getModelObject();
    if (training != null) {
      getRecentTrainings().append(formatTraining(training));
    }
  }

  public TrainingSelectPanel withAutoSubmit(final boolean autoSubmit)
  {
    trainingTextField.withAutoSubmit(autoSubmit);
    return this;
  }

  @Override
  public Component getWrappedComponent()
  {
    return trainingTextField;
  }

  @Override
  public void convertInput()
  {
    setConvertedInput(getModelObject());
  }

  @SuppressWarnings("unchecked")
  private RecentQueue<String> getRecentTrainings()
  {
    if (this.recentTrainings == null) {
      this.recentTrainings = (RecentQueue<String>) UserPreferencesHelper.getEntry(USER_PREF_KEY_RECENT_TRAININGS);
    }
    if (this.recentTrainings == null) {
      this.recentTrainings = new RecentQueue<>();
      UserPreferencesHelper.putEntry(USER_PREF_KEY_RECENT_TRAININGS, this.recentTrainings, true);
    }
    return this.recentTrainings;
  }

  private String formatTraining(final TrainingDO training)
  {
    if (training == null) {
      return "";
    }
    // PLEASE NOTE: If you change the format don't forget to change the format above (search ### FORMAT ###)
    String s = "";
    if (training.getSkill() != null && training.getSkill().getTitle() != null) {
      s = training.getSkill().getTitle();
    }
    return training.getTitle() + " (" + s + ")";
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    trainingTextField.setOutputMarkupId(true);
    return trainingTextField.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return trainingTextField;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSelectPanel#setFocus()
   */
  @Override
  public AbstractSelectPanel<TrainingDO> setFocus()
  {
    WicketUtils.setFocus(this.trainingTextField);
    return this;
  }

  /**
   * Should be called before init() method. If true, then the validation will be done after submitting.
   *
   * @param defaultFormProcessing
   */
  public void setDefaultFormProcessing(final boolean defaultFormProcessing)
  {
    this.defaultFormProcessing = defaultFormProcessing;
  }

}
