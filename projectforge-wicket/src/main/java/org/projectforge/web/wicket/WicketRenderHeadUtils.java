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

import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.projectforge.web.WebConfiguration;

import de.micromata.less.LessWicketApplicationInstantiator;

public class WicketRenderHeadUtils
{
  private static final String[][] JAVASCRIPT_FILES_DEF = { //
    // { "scripts/jquery/1.8.2/jquery-1.8.2", ".min"}, // Wicket delivers jQuery version
    // Needed: core, widget, mouse, position, draggable, droppable, resizable, selectable, sortable, datepicker (smoothness):
    { "scripts/jqueryui/jquery-ui-1.10.4.custom", ".min"}, //
    { "include/bootstrap/js/bootstrap", ""}, // ".min" bootstrap.js is modified!
    { "scripts/contextmenu/jquery.contextmenu", ""} //
  };

  private static final String[] JAVASCRIPT_FILES;

  private static final String[][] JAVASCRIPT_FILES_JAVA_DEF = { //

    { "scripts/projectforge", ""} //
  };

  private static final String[] JAVASCRIPT_FILES_JAVA;

  private static final String[][] CSS_FILES_DEF = { //
    { "styles/google-fonts/google-fonts", ""}, //
    // "http://fonts.googleapis.com/css?family=Droid+Sans:regular&amp;subset=latin", //
    { "styles/jqueryui/1.10.4/smoothness/jquery-ui-1.10.4.custom", ".min"} //
  };

  private static final String[] CSS_FILES;

  private static final String[][] AUTOGROW_JAVASCRIPT_FILES_DEF = { //
    { "scripts/autogrow/jquery.autogrowtextarea", ""}};

  private static final String[] AUTOGROW_JAVASCRIPT_FILES;

  static {
    JAVASCRIPT_FILES = new String[JAVASCRIPT_FILES_DEF.length];
    JAVASCRIPT_FILES_JAVA = new String[JAVASCRIPT_FILES_JAVA_DEF.length];
    CSS_FILES = new String[CSS_FILES_DEF.length];
    AUTOGROW_JAVASCRIPT_FILES = new String[AUTOGROW_JAVASCRIPT_FILES_DEF.length];
    if (WebConfiguration.isDevelopmentMode() == true) {
      for (int i = 0; i < JAVASCRIPT_FILES_DEF.length; i++) {
        JAVASCRIPT_FILES[i] = JAVASCRIPT_FILES_DEF[i][0] + ".js";
      }

      handleWicketResourceHandledJavascript();

      for (int i = 0; i < CSS_FILES_DEF.length; i++) {
        CSS_FILES[i] = CSS_FILES_DEF[i][0] + ".css";
      }
      for (int i = 0; i < AUTOGROW_JAVASCRIPT_FILES_DEF.length; i++) {
        AUTOGROW_JAVASCRIPT_FILES[i] = AUTOGROW_JAVASCRIPT_FILES_DEF[i][0] + ".js";
      }
    } else {
      for (int i = 0; i < JAVASCRIPT_FILES_DEF.length; i++) {
        JAVASCRIPT_FILES[i] = JAVASCRIPT_FILES_DEF[i][0] + JAVASCRIPT_FILES_DEF[i][1] + ".js";
      }

      handleWicketResourceHandledJavascript();

      for (int i = 0; i < CSS_FILES_DEF.length; i++) {
        CSS_FILES[i] = CSS_FILES_DEF[i][0] + CSS_FILES_DEF[i][1] + ".css";
      }
      for (int i = 0; i < AUTOGROW_JAVASCRIPT_FILES_DEF.length; i++) {
        AUTOGROW_JAVASCRIPT_FILES[i] = AUTOGROW_JAVASCRIPT_FILES_DEF[i][0] + AUTOGROW_JAVASCRIPT_FILES_DEF[i][1] + ".js";
      }
    }
  }

  private static void handleWicketResourceHandledJavascript() {
    // handle wicket resource handled javascript files
    final long startTime = WicketApplication.getStartTime();
    String name = null;
    String versionName = null;
    for (int i = 0; i < JAVASCRIPT_FILES_JAVA_DEF.length; i++) {
      name = JAVASCRIPT_FILES_JAVA_DEF[i][0] + ".js";
      versionName = JAVASCRIPT_FILES_JAVA_DEF[i][0] + "-version-" + startTime + ".js";
      WicketApplication.get().mountResource(versionName,
          new PackageResourceReference(WicketApplication.class, name));
      JAVASCRIPT_FILES_JAVA[i] = "wa/" + versionName;
    }
  }

  /**
   * Bbootstrap, jqueryui and uquery.contextmenu.js.
   */
  public static void renderMainJavaScriptIncludes(final IHeaderResponse response)
  {
    for (final String url : JAVASCRIPT_FILES) {
      response.render(JavaScriptReferenceHeaderItem.forUrl(url));
    }
    for (final String url : JAVASCRIPT_FILES_JAVA) {
      response.render(JavaScriptReferenceHeaderItem.forUrl(url));
    }
  }

  /**
   * Bootstrap, jqueryui and uquery.contextmenu.js.
   */
  public static void renderMainCSSIncludes(final IHeaderResponse response)
  {
    for (final String url : CSS_FILES) {
      response.render(CssReferenceHeaderItem.forUrl(url));
    }
    LessWicketApplicationInstantiator.renderCompiledCssResource(response);
  }

  /**
   * Renders all main JavaScript files and "select2.js".
   */
  public static void renderSelect2JavaScriptIncludes(final IHeaderResponse response)
  {
    renderMainJavaScriptIncludes(response);
    // for (final String url : SELECT2_JAVASCRIPT_FILES) {
    // response.render(JavaScriptReferenceHeaderItem.forUrl(url));
    // }
  }

  /**
   * Bootstrap, jqueryui and uquery.contextmenu.js.
   */
  public static void renderAutogrowJavaScriptIncludes(final IHeaderResponse response)
  {
    renderMainJavaScriptIncludes(response);
    for (final String url : AUTOGROW_JAVASCRIPT_FILES) {
      response.render(JavaScriptReferenceHeaderItem.forUrl(url));
    }
  }


}
