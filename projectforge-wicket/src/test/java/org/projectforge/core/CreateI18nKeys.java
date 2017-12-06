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

package org.projectforge.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.projectforge.business.address.FormOfAddress;
import org.projectforge.business.fibu.AuftragsStatus;
import org.projectforge.common.i18n.Priority;
import org.projectforge.framework.calendar.MonthHolder;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.web.MenuItemDef;
import org.projectforge.web.MenuItemDefId;
import org.projectforge.web.wicket.WebConstants;

/**
 * Tries to get all used i18n keys from the sources (java and html). As result a file is written which will be checked
 * by AdminAction.checkI18nProperties. Unused i18n keys should be detected.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class CreateI18nKeys
{
  // TODO: I18nEnums

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateI18nKeys.class);

  private static final String PATH = "src/main/";

  private static final String PATH_DAYHOLDER = getPathForClass(DayHolder.class);

  private static final String PATH_MONTHHOLDER = getPathForClass(MonthHolder.class);

  private static final String PATH_FORM_OF_ADDRESS = getPathForClass(FormOfAddress.class);

  private static final String PATH_MENU_ITEM_DEF = getPathForClass(MenuItemDef.class);

  private static final String PATH_AUFTRAG_STATUS = getPathForClass(AuftragsStatus.class);

  private static final String PATH_PRIORITY = getPathForClass(Priority.class);

  private static final String I18N_KEYS_FILE = "src/main/resources/" + WebConstants.FILE_I18N_KEYS;

  public static void main(final String[] args) throws IOException
  {
    new CreateI18nKeys().run();
  }

  public void run() throws IOException
  {
    log.info("Create file with all detected i18n keys.");
    final Map<String, Set<String>> i18nKeyUsage = new HashMap<String, Set<String>>();
    parseHtml(i18nKeyUsage);
    parseJava(i18nKeyUsage);
    final FileWriter writer = new FileWriter(I18N_KEYS_FILE);
    writer
        .append(
            "# Don't edit this file. This file is only for developers for checking i18n keys and detecting missed and unused ones.\n");
    final Set<String> i18nKeys = new TreeSet<String>(i18nKeyUsage.keySet());
    for (final String i18nKey : i18nKeys) {
      writer.append(i18nKey).append("=");
      final Set<String> set = i18nKeyUsage.get(i18nKey);
      boolean first = true;
      for (final String filename : set) {
        if (first == false) {
          writer.append(',');
        } else {
          first = false;
        }
        writer.append(filename);
      }
      writer.append("\n");
    }
    IOUtils.closeQuietly(writer);
    log.info("Creation of file of found i18n keys done: " + I18N_KEYS_FILE);
  }

  private void parseHtml(final Map<String, Set<String>> i18nKeyUsage) throws IOException
  {
    final Collection<File> files = listFiles(PATH + "java", "html");
    for (final File file : files) {
      final String content = getContent(file);
      find(file, i18nKeyUsage, content, "<wicket:message\\s+key=\"([a-zA-Z0-9\\.]+)\"\\s/>");
    }
  }

  private void parseJava(final Map<String, Set<String>> i18nKeyUsage) throws IOException
  {
    final Collection<File> files = listFiles(PATH, "java");
    for (final File file : files) {
      final String content = getContent(file);
      find(file, i18nKeyUsage, content, "getString\\(\"([a-zA-Z0-9\\.]+)\"\\)"); // getString("i18nKey")
      find(file, i18nKeyUsage, content, "getLocalizedString\\(\"([a-zA-Z0-9\\.]+)\"\\)"); // getLocalizedString("i18nkey")
      find(file, i18nKeyUsage, content, "getLocalizedMessage\\(\"([a-zA-Z0-9\\.]+)\""); // getLocalizedMessage("i18nkey"
      find(file, i18nKeyUsage, content, "addError\\(\"([a-zA-Z0-9\\.]+)\"\\)"); // addError("i18nkey")
      find(file, i18nKeyUsage, content, "addError\\([a-zA-Z0-9_\"\\.]+,\\s+\"([a-zA-Z0-9\\.]+)\""); // addError(..., "i18nkey"
      find(file, i18nKeyUsage, content, "addGlobalError\\(\"([a-zA-Z0-9\\.]+)\"\\)"); // addGlobalError("i18nkey")
      find(file, i18nKeyUsage, content, "resolveMessage\\(\"([a-zA-Z0-9\\.]+)\"\\)"); // resolveMessage("i18nkey")
      find(file, i18nKeyUsage, content, "throw new UserException\\(\"([a-zA-Z0-9\\.]+)\""); // throw new UserException("i18nkey"
      find(file, i18nKeyUsage, content, "throw new AccessException\\(\"([a-zA-Z0-9\\.]+)\""); // throw new AccessException("i18nkey"
      find(file, i18nKeyUsage, content, "I18N_KEY_[A-Z0-9_]+ = \"([a-zA-Z0-9\\.]+)\""); // I18N_KEY_... = "i18nKey"
      find(file, i18nKeyUsage, content, "new Holiday\\(\"([a-zA-Z0-9\\.]+)\""); // new Holiday("i18nKey"
      find(file, i18nKeyUsage, content,
          "MessageAction.getForwardResolution\\([a-zA-Z0-9_\"\\.]+,\\s+\"([a-zA-Z0-9\\.]+)\"\\)"); // MessageAction.getForwardResolution(...,
      // "i18nkey");
      if (file.getPath().endsWith(PATH_DAYHOLDER) == true) {
        for (final String key : DayHolder.DAY_KEYS) {
          add(i18nKeyUsage, "calendar.day." + key, file);
          add(i18nKeyUsage, "calendar.shortday." + key, file);
        }
      } else if (file.getPath().endsWith(PATH_MONTHHOLDER) == true) {
        for (final String key : MonthHolder.MONTH_KEYS) {
          add(i18nKeyUsage, "calendar.month." + key, file);
        }
      } else if (file.getPath().endsWith(PATH_FORM_OF_ADDRESS) == true) {
        for (final FormOfAddress form : FormOfAddress.values()) {
          add(i18nKeyUsage, form.getI18nKey(), file);
        }
      } else if (file.getPath().endsWith(PATH_AUFTRAG_STATUS) == true) {
        for (final AuftragsStatus status : AuftragsStatus.values()) {
          add(i18nKeyUsage, status.getI18nKey(), file);
        }
      } else if (file.getPath().endsWith(PATH_PRIORITY) == true) {
        for (final Priority priority : Priority.values()) {
          add(i18nKeyUsage, "priority." + priority.getKey(), file);
        }
      } else if (file.getPath().endsWith(PATH_MENU_ITEM_DEF) == true) {
        for (final MenuItemDefId menuItem : MenuItemDefId.values()) {
          add(i18nKeyUsage, menuItem.getI18nKey(), file);
        }
      } else if (file.getName().endsWith("Page.java") == true
          && (content.contains("extends AbstractListPage") == true
          || content.contains("extends AbstractEditPage") == true)) { // Wicket
        // Page
        List<String> list = find(file, content, "super\\(parameters, \"([a-zA-Z0-9\\.]+)\"\\);"); // super(parameters, "i18nKey");
        for (final String entry : list) {
          add(i18nKeyUsage, entry + ".title.edit", file);
          add(i18nKeyUsage, entry + ".title.list", file);
          add(i18nKeyUsage, entry + ".title.list.select", file);
        }
        list = find(file, content, "super\\(caller, selectProperty, \"([a-zA-Z0-9\\.]+)\"\\);"); // super(caller, selectProperty,
        // "i18nKey");
        for (final String entry : list) {
          add(i18nKeyUsage, entry + ".title.edit", file);
          add(i18nKeyUsage, entry + ".title.list", file);
          add(i18nKeyUsage, entry + ".title.list.select", file);
        }
      }
    }
  }

  private static String getPathForClass(final Class<?> clazz)
  {
    return clazz.getName().replace(".", "/") + ".java";
  }

  private void find(final File file, final Map<String, Set<String>> i18nKeyUsage, final String content,
      final String regexp)
  {
    find(file, i18nKeyUsage, content, regexp, null);
  }

  private void find(final File file, final Map<String, Set<String>> i18nKeyUsage, final String content,
      final String regexp,
      final String prefix)
  {
    final List<String> list = find(file, content, regexp);
    for (final String entry : list) {
      final String key = prefix != null ? prefix + entry : entry;
      add(i18nKeyUsage, key, file);
    }
  }

  private List<String> find(final File file, final String content, final String regexp)
  {
    final List<String> result = new ArrayList<String>();
    final Pattern p = Pattern.compile(regexp, Pattern.MULTILINE); // Compiles regular expression into Pattern.
    final Matcher m = p.matcher(content);
    while (m.find()) {
      result.add(m.group(1));
    }
    return result;
  }

  private void add(final Map<String, Set<String>> i18nKeyUsage, final String key, final File file)
  {
    Set<String> set = i18nKeyUsage.get(key);
    if (set == null) {
      set = new TreeSet<String>();
      i18nKeyUsage.put(key, set);
    }
    set.add(file.getPath());
  }

  private Collection<File> listFiles(final String path, final String suffix)
  {
    return FileUtils.listFiles(new File(path), new String[] { suffix }, true);
  }

  private String getContent(final File file) throws IOException
  {
    return FileUtils.readFileToString(file, "UTF-8");
  }
}
