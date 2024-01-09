/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.todo;

import org.junit.jupiter.api.Test;
import org.projectforge.ProjectForgeVersion;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.scripting.GroovyEngine;
import org.projectforge.common.i18n.Priority;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

public class GroovyEngineTest extends AbstractTestBase {
  @Autowired
  private ConfigurationService configurationService;

  @Test
  public void renderTest() {
    final GroovyEngine engine = new GroovyEngine(configurationService, Locale.GERMAN, TimeZone.getTimeZone("UTC"));
    engine.putVariable("name", "Kai");

    final String res = engine.executeTemplate("Hallo $name, your locale is '<%= pf.getI18nString(\"locale.de\") %>'.");
    assertEquals("Hallo Kai, your locale is 'Deutsch'.", res);
    assertEquals("Hallo Kai, your locale is 'Deutsch'. " + ProjectForgeVersion.APP_ID + "\n Finished: Englisch", engine
            .executeTemplateFile("scripting/template.txt"));
  }

  @Test
  public void mailTemplateTest() {
    final GroovyEngine engine = new GroovyEngine(configurationService, Locale.GERMAN, TimeZone.getTimeZone("UTC"));
    PFUserDO user = new PFUserDO();
    user.setFirstname("Kai");
    user.setLastname("Reinhard");
    user.setLocale(Locale.GERMAN);
    engine.putVariable("recipient", user);
    ToDoDO todo = new ToDoDO();
    todo.setType(ToDoType.IMPROVEMENT);
    todo.setPriority(Priority.HIGH);
    todo.setReporter(user);
    todo.setAssignee(user);
    engine.putVariable("todo", todo);
    engine.putVariable("title", "ToDo");
    engine.putVariable("history", new ArrayList<DisplayHistoryEntry>());
    engine.putVariable("requestUrl", "https://localhost:8443/wa/toDoEditPage/id/42");
    final String result = engine.executeTemplateFile("mail/todoChangeNotification.html");
    assertTrue(result.contains("hoch"), "I18n priority expected.");
    assertTrue(result.contains("Verbesserung"), "I18n key for type improvement expected.");
  }

  @Test
  public void preprocesTest() {
    final GroovyEngine engine = new GroovyEngine(configurationService, Locale.GERMAN, TimeZone.getTimeZone("UTC"));
    assertNull(engine.preprocessGroovyXml(null));
    assertEquals("", engine.preprocessGroovyXml(""));
    assertEquals("<% if (value != null) { %>", engine.preprocessGroovyXml("<groovy>if (value != null) {</groovy>"));
    assertEquals("<%= value %>", engine.preprocessGroovyXml("<groovy-out>value</groovy-out>"));
  }
}
