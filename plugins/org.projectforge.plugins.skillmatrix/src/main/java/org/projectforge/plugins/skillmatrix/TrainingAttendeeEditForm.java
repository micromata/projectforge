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

import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.Hibernate;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This is the edit formular page.
 * 
 * @author Werner Feder (werner.feder@t-online.de)
 */
public class TrainingAttendeeEditForm extends AbstractEditForm<TrainingAttendeeDO, TrainingAttendeeEditPage>
{

  private static final long serialVersionUID = 6814668114853472909L;

  private static final Logger log = LoggerFactory.getLogger(TrainingAttendeeEditForm.class);

  private FieldsetPanel ratingFs, certificateFs;

  private LabelValueChoiceRenderer<String> ratingChoiceRenderer, certificateChoiceRenderer;

  @SpringBean
  private TrainingAttendeeDao trainingAttendeeDao;

  @SpringBean
  private SkillDao skillDao;

  /**
   * @param parentPage
   * @param data
   */
  public TrainingAttendeeEditForm(final TrainingAttendeeEditPage parentPage, final TrainingAttendeeDO data)
  {
    super(parentPage, data);
  }

  @Override
  public void init()
  {
    super.init();

    gridBuilder.newSplitPanel(GridSize.COL50);

    // Training
    FieldsetPanel fs = gridBuilder.newFieldset(TrainingAttendeeDO.class, "training");
    TrainingDO training = data.getTraining();
    if (!Hibernate.isInitialized(training)) {
      training = trainingAttendeeDao.getTraingDao().getOrLoad(training.getId());
      data.setTraining(training);
    }
    final TrainingSelectPanel trainingSelectPanel = new TrainingSelectPanel(fs.newChildId(),
        new PropertyModel<>(data, "training"), parentPage, "trainingId");
    trainingSelectPanel.setDefaultFormProcessing(false);
    trainingSelectPanel.init().withAutoSubmit(true).setRequired(true);
    fs.add(trainingSelectPanel);

    // Attendee
    fs = gridBuilder.newFieldset(TrainingAttendeeDO.class, "attendee");
    PFUserDO attendee = data.getAttendee();
    if (!Hibernate.isInitialized(attendee)) {
      attendee = trainingAttendeeDao.getUserDao().getOrLoad(attendee.getId());
      data.setAttendee(attendee);
    }
    final UserSelectPanel attendeeSelectPanel = new UserSelectPanel(fs.newChildId(),
        new PropertyModel<>(data, "attendee"),
        parentPage, "attendeeId");
    fs.add(attendeeSelectPanel.setRequired(true));
    attendeeSelectPanel.init();

    if (isNew()) {
      trainingSelectPanel.setFocus();
    } else {
      attendeeSelectPanel.setFocus();
    }
    { // Rating
      ratingFs = gridBuilder.newFieldset(TrainingAttendeeDO.class, "rating");
      ratingChoiceRenderer = new LabelValueChoiceRenderer<>();
      ratingFs.addDropDownChoice(new PropertyModel<>(data, "rating"), ratingChoiceRenderer.getValues(),
          ratingChoiceRenderer)
          .setNullValid(true);
    }
    { // Certificate
      certificateFs = gridBuilder.newFieldset(TrainingAttendeeDO.class, "certificate");
      certificateChoiceRenderer = new LabelValueChoiceRenderer<>();
      certificateFs
          .addDropDownChoice(new PropertyModel<>(data, "certificate"), certificateChoiceRenderer.getValues(),
              certificateChoiceRenderer)
          .setNullValid(true);
    }
    {
      // startDate
      fs = gridBuilder.newFieldset(TrainingDO.class, "startDate");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<>(data, "startDate"),
          DatePanelSettings.get().withTargetType(
              java.sql.Date.class)));
    }
    {
      // EndDate
      fs = gridBuilder.newFieldset(TrainingDO.class, "endDate");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<>(data, "endDate"),
          DatePanelSettings.get().withTargetType(
              java.sql.Date.class)));
    }
    { // Description
      fs = gridBuilder.newFieldset(TrainingAttendeeDO.class, "description");
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<>(data, "description"))).setAutogrow();
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#onBeforeRender()
   */
  @Override
  public void onBeforeRender()
  {
    super.onBeforeRender();
    final TrainingDO training = data.getTraining();

    if (training != null) {
      if (training.getRatingArray() != null) {
        ratingChoiceRenderer.clear().setValueArray(training.getRatingArray());
        ratingFs.setVisible(true);
      } else
        ratingFs.setVisible(false);

      if (training.getCertificateArray() != null) {
        certificateChoiceRenderer.clear().setValueArray(training.getCertificateArray());
        certificateFs.setVisible(true);
      } else
        certificateFs.setVisible(false);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

}
