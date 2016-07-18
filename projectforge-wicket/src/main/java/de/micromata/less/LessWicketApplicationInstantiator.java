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

package de.micromata.less;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.util.listener.IChangeListener;
import org.apache.wicket.util.watch.IModificationWatcher;
import org.lesscss.LessCompiler;
import org.lesscss.LessSource;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.wicket.WicketApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

/**
 * Compiler utility class for less resource files
 * 
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * 
 */
public class LessWicketApplicationInstantiator implements Serializable
{
  private static final long serialVersionUID = 243362344112860106L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(LessWicketApplicationInstantiator.class);

  private static LessWicketApplicationInstantiator instance;

  private LessResourceReference reference;

  private final WicketApplication application;

  private final String baseDir;

  private final String folder;

  private final String lessPath;

  private final String cssPath;

  private final String relativeCssPath;

  private InputStream lessTargetStream;

  private File lessTargetFile;

  private File cssTargetFile;

  /**
   * 
   * @param application
   * @param folder
   * @param lessPath
   * @param cssPath
   */
  public LessWicketApplicationInstantiator(final WicketApplication application, final String folder,
      final String lessPath,
      final String cssPath, final String baseDir)
  {
    this.application = application;
    this.baseDir = baseDir;
    this.folder = folder;
    this.lessPath = lessPath;
    this.cssPath = cssPath;
    this.relativeCssPath = folder + "/" + cssPath;
    instance = this;
  }

  private void instantiateFiles() throws Exception
  {
    lessTargetFile = new File(
        new ClassPathResource(folder).getPath() + "/" + lessPath);
    lessTargetStream = new ClassPathResource(folder + "/" + lessPath).getInputStream();
    cssTargetFile = new File(baseDir + "/styles/" + cssPath);
  }

  /**
   * compiles the saved .less to the wanted .css file
   * 
   * @return
   * @throws Exception
   */
  private LessSource compile() throws Exception
  {
    // compile file
    final LessCompiler lessCompiler = new LessCompiler();

    // create new source
    LessSource mainLessSource = null;
    try {
      mainLessSource = new LessSource(lessTargetFile, folder);
    } catch (FileNotFoundException e) {
      log.info("LessSource by file not working. Trying with Stream.");
      mainLessSource = new LessSource(lessTargetStream, lessTargetFile, folder);
    }

    log.info("compiling " + lessTargetFile.getAbsolutePath() + " to " + cssTargetFile.getAbsolutePath());
    lessCompiler.compile(mainLessSource, cssTargetFile, true);
    return mainLessSource;
  }

  /**
   * instantiates the actual less compilement
   * 
   * @throws Exception
   */
  public void instantiate() throws Exception
  {
    instantiateFiles();
    final LessSource mainLessSource = compile();

    if (WebConfiguration.isDevelopmentMode() == true) {
      // only add this fancy resource watcher in dev mode
      final IModificationWatcher resourceWatcher = application.getResourceSettings().getResourceWatcher(true);
      // add watchers
      addWatcher(resourceWatcher, mainLessSource);
      for (final LessSource importedSource : mainLessSource.getImports().values()) {
        addWatcher(resourceWatcher, importedSource);
      }

    }

    // mount compiled css file
    reference = new LessResourceReference(relativeCssPath, cssTargetFile);
    application.mountResource(encodePathWithCachingStrategy(relativeCssPath), reference);
  }

  public String encodePathWithCachingStrategy(final String path)
  {
    // wicket uses by default -ver-, so we need to use another prefix
    return StringUtils.replace(path, ".css", "-version-" + WicketApplication.getStartTime() + ".css");
  }

  /**
   * adds a resource watcher entry to the given less source
   * 
   * @param resourceWatcher
   * @param importedSource
   */
  private void addWatcher(final IModificationWatcher resourceWatcher, final LessSource importedSource)
  {
    log.info("adding watcher to less file " + importedSource.getAbsolutePath());
    resourceWatcher.add(new org.apache.wicket.util.file.File(new File(importedSource.getAbsolutePath())),
        new IChangeListener()
        {

          @Override
          public void onChange()
          {
            try {
              compile();
            } catch (final Exception e) {
              final String message = "Unable to compile less source during watcher for file "
                  + importedSource.getAbsolutePath();
              log.error(message, e);
            }
          }
        });
  }

  /**
   * Renders the compiled css reference to the given response
   * 
   * @param response
   */
  public static void renderCompiledCssResource(final IHeaderResponse response)
  {
    if (instance != null && instance.reference != null) {
      response.render(CssReferenceHeaderItem.forReference(instance.reference));
    } else {
      final String message = "Unable to find compiled main projectforge.css less file";
      log.error(message);
    }
  }
}
