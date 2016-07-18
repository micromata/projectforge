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

import org.apache.wicket.AttributeModifier;

public class WebConstants
{
  public static final String FILE_IMAGE_DIMENSIONS = "imageDimensions.xml";

  public static final String FILE_I18N_KEYS = "i18nKeys.properties";

  public static final String HTML_TEXT_DIVIDER = " | ";

  /**
   * The access key for adding new entries in list view.
   */
  public static final char ACCESS_KEY_ADD = 'n';

  public static final String ACCESS_KEY_ADD_TOOLTIP = "tooltip.accesskey.addEntry";

  public static final String ACCESS_KEY_ADD_TOOLTIP_TITLE = "tooltip.accesskey.addEntry.title";

  /** For setting the caller action page as parameter for the callee. */
  public static final String PARAMETER_CALLER = "caller";

  public static final String PARAMETER_USER_ID = "uid";

  public static final String PARAMETER_DATE = "date";

  private static final String DIR = "images/";

  public static final String DOC_LINK_HANDBUCH_LUCENE = "secure/doc/UserGuide.html#label_fullindexsearch";

  public static final String I18N_KEY_FIELD_REQUIRED = "validation.error.fieldRequired";

  public static final String IMAGE_ADD = DIR + "add.png";

  public static final String IMAGE_ARROW_DOWN = DIR + "arrow-down.png";

  public static final String IMAGE_BUTTON_CANCEL = DIR + "button_cancel.png";

  public static final String IMAGE_COG = DIR + "cog.png";

  public static final String IMAGE_DELETE = DIR + "trash.png";

  public static final String IMAGE_EYE = DIR + "eye.png";

  public static final String IMAGE_FIND = DIR + "find.png";

  public static final String IMAGE_GROUP_SELECT = DIR + "button_selectGroup.png";

  public static final String IMAGE_GROUP_UNSELECT = DIR + "button_unselectGroup.png";

  public static final String IMAGE_HELP_KEYBOARD = DIR + "keyboard.png";

  public static final String IMAGE_KOST2_SELECT = DIR + "coins_add.png";

  public static final String IMAGE_KOST2_UNSELECT = DIR + "coins_delete.png";

  public static final String IMAGE_KUNDE_SELECT = DIR + "button_selectCustomer.png";

  public static final String IMAGE_KUNDE_UNSELECT = DIR + "button_unselectCustomer.png";

  public static final String IMAGE_PHONE = DIR + "telephone.png";

  public static final String IMAGE_PHONE_MOBILE = DIR + "phone.png";

  public static final String IMAGE_PHONE_HOME = DIR + "house.png";

  public static final String IMAGE_PROJEKT_SELECT = DIR + "button_selectProjekt.png";

  public static final String IMAGE_PROJEKT_UNSELECT = DIR + "button_unselectProjekt.png";

  public static final String IMAGE_PRINTER = DIR + "printer.png";

  public static final String IMAGE_QUICKSELECT_CURRENT_MONTH = DIR + "button_calendar_month.png";

  public static final String IMAGE_QUICKSELECT_CURRENT_WEEK = DIR + "button_calendar_week.png";

  public static final String IMAGE_QUICKSELECT_FOLLOWING_MONTH = DIR + "button_next_month.png";

  public static final String IMAGE_QUICKSELECT_FOLLOWING_WEEK = DIR + "button_next_week.png";

  public static final String IMAGE_QUICKSELECT_PREVIOUS_MONTH = DIR + "button_previous_month.png";

  public static final String IMAGE_QUICKSELECT_PREVIOUS_WEEK = DIR + "button_previous_week.png";

  public static final String IMAGE_TASK_SELECT = DIR + "button_selectTask.png";

  public static final String IMAGE_TASK_UNSELECT = DIR + "button_unselectTask.png";

  public static final String IMAGE_UNDELETE = DIR + "arrow_undo.png";

  public static final String IMAGE_SPACER = DIR + "spacer.gif";

  public static final String IMAGE_TREE_ICON_LEAF = DIR + "leaf.gif";

  public static final String IMAGE_TREE_ICON_FOLDER = DIR + "folder.gif";

  public static final String IMAGE_TREE_ICON_FOLDER_OPEN = DIR + "folder_open.gif";

  public static final String IMAGE_TREE_ICON_EXPLOSION = DIR + "explosion.gif";

  public static final int IMAGE_TREE_ICON_HEIGHT = 15;

  public static final int IMAGE_TREE_ICON_WIDTH = 19;

  public static final String CSS_BACKGROUND_COLOR_RED = "background-color: #eeaaaa;";

  /**
   * Used as class attribute for input fields.
   */
  public static final String CSS_INPUT_STDTEXT = "stdtext";

  public static final AttributeModifier HELP_CLASS = AttributeModifier.replace("class", "help");
}
