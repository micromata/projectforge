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

import org.projectforge.business.group.service.GroupService;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.web.plugin.PluginWicketRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Billy Duong (b.duong@micromata.de)
 */
@Component
public class SkillMatrixPlugin extends AbstractPlugin {
  public static final String ID_SKILL_TOP_LEVEL = "skillTopLevel";

  public static final String ID_SKILL_RATING = "skillRating";

  public static final String ID_SKILL = "skill";

  public static final String ID_SKILL_TREE = "skillTree";

  public static final String ID_SKILL_TRAINING = "skillTraining";

  public static final String ID_SKILL_TRAINING_ATTENDEE = "skillTrainingAttendee";

  public static final String RESOURCE_BUNDLE_NAME = "SkillMatrixI18nResources";

  static UserPrefArea USER_PREF_AREA;

  // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
  // each test).
  // The entities are inserted in ascending order and deleted in descending order.
  private static final Class<?>[] PERSISTENT_ENTITIES = new Class<?>[]{SkillDO.class, SkillRatingDO.class,
          TrainingDO.class, TrainingAttendeeDO.class};

  public static final String I18N_KEY_SKILLMATRIX_PREFIX = "plugins.skillmatrix";

  public static final String I18N_KEY_SKILLRATING_MENU_ENTRY = "plugins.skillmatrix.skillrating.menu";

  public static final String I18N_KEY_SKILL_MENU_ENTRY = "plugins.skillmatrix.skill.menu";

  public static final String I18N_KEY_SKILLTREE_MENU_ENTRY = "plugins.skillmatrix.skilltree.menu";

  public static final String I18N_KEY_SKILLTRAINING_MENU_ENTRY = "plugins.skillmatrix.skilltraining.menu";

  public static final String I18N_KEY_SKILLTRAINING_ATTENDEE_MENU_ENTRY = "plugins.skillmatrix.skilltraining.attendee.menu";

  @Autowired
  private PluginWicketRegistrationService pluginWicketRegistrationService;

  /**
   * This dao should be defined in pluginContext.xml (as resources) for proper initialization.
   */
  @Autowired
  private SkillDao skillDao;

  @Autowired
  private SkillRatingDao skillRatingDao;

  @Autowired
  private TrainingDao trainingDao;

  @Autowired
  private TrainingAttendeeDao trainingAttendeeDao;

  @Autowired
  private GroupService groupService;

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#initialize()
   */
  @Override
  protected void initialize() {
    // DatabaseUpdateDao is needed by the updater:
    SkillMatrixPluginUpdates.dao = myDatabaseUpdater;
    register(ID_SKILL_RATING, SkillRatingDao.class, skillRatingDao, I18N_KEY_SKILLMATRIX_PREFIX);
    register(ID_SKILL, SkillDao.class, skillDao, I18N_KEY_SKILLMATRIX_PREFIX);
    register(ID_SKILL_TRAINING, TrainingDao.class, trainingDao, I18N_KEY_SKILLMATRIX_PREFIX);
    register(ID_SKILL_TRAINING_ATTENDEE, TrainingAttendeeDao.class, trainingAttendeeDao, I18N_KEY_SKILLMATRIX_PREFIX);

    // Register the web part:
    pluginWicketRegistrationService.registerWeb(ID_SKILL_RATING, SkillRatingListPage.class, SkillRatingEditPage.class);
    pluginWicketRegistrationService.registerWeb(ID_SKILL, SkillListPage.class, SkillEditPage.class);
    pluginWicketRegistrationService.registerWeb(ID_SKILL_TRAINING, TrainingListPage.class, TrainingEditPage.class);
    pluginWicketRegistrationService.registerWeb(ID_SKILL_TRAINING_ATTENDEE, TrainingAttendeeListPage.class,
            TrainingAttendeeEditPage.class);

    // Register the menu entry as sub menu entry of the misc menu:
    MenuItemDef skillMenu = MenuItemDef.create(ID_SKILL_TOP_LEVEL, I18N_KEY_SKILL_MENU_ENTRY);
    pluginWicketRegistrationService.registerTopLevelMenuItem(skillMenu, SkillTreePage.class);

    pluginWicketRegistrationService.registerMenuItem(skillMenu.getId(),
            MenuItemDef.create(ID_SKILL_TREE, I18N_KEY_SKILLTREE_MENU_ENTRY), SkillTreePage.class);
    pluginWicketRegistrationService.registerMenuItem(skillMenu.getId(),
            MenuItemDef.create(ID_SKILL_RATING, I18N_KEY_SKILLRATING_MENU_ENTRY), SkillRatingListPage.class);
    pluginWicketRegistrationService.registerMenuItem(skillMenu.getId(),
            MenuItemDef.create(ID_SKILL, I18N_KEY_SKILL_MENU_ENTRY), SkillListPage.class);
    pluginWicketRegistrationService.registerMenuItem(skillMenu.getId(),
            MenuItemDef.create(ID_SKILL_TRAINING, I18N_KEY_SKILLTRAINING_MENU_ENTRY), TrainingListPage.class);
    pluginWicketRegistrationService.registerMenuItem(skillMenu.getId(),
            MenuItemDef.create(ID_SKILL_TRAINING_ATTENDEE, I18N_KEY_SKILLTRAINING_ATTENDEE_MENU_ENTRY), TrainingAttendeeListPage.class);

    // .setMobileMenu(SkillRatingMobileListPage.class, 10));

    // Define the access management:
    registerRight(new SkillRight(accessChecker, groupService));
    registerRight(new SkillRatingRight(accessChecker));
    registerRight(new TrainingRight(accessChecker));
    registerRight(new TrainingAttendeeRight(accessChecker));

    // All the i18n stuff:
    addResourceBundle(RESOURCE_BUNDLE_NAME);
  }

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#getInitializationUpdateEntry()
   */
  @Override
  public UpdateEntry getInitializationUpdateEntry() {
    return SkillMatrixPluginUpdates.getInitializationUpdateEntry();
  }

}
