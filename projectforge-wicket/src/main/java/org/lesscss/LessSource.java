/* Copyright 2011-2012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lesscss;

// If less.js 1.3.3 is supported again, remove this package and re-activate the pom.xml entry.

import static java.util.regex.Pattern.MULTILINE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jfree.util.Log;

/**
 * Represents the metadata and content of a LESS source.
 * 
 * @author Marcel Overdijk
 */
public class LessSource
{

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(LessSource.class);

  /**
   * The <code>Pattern</code> used to match imported files.
   */
  private static final Pattern IMPORT_PATTERN = Pattern
      .compile("^(?!\\s*//\\s*)@import\\s+(url\\()?\\s*\"(.+)\\s*\"(\\))?\\s*;.*$", MULTILINE);

  private final File file;
  private final String content;
  private String normalizedContent;
  private final Map<String, LessSource> imports = new LinkedHashMap<String, LessSource>();

  /**
   * Constructs a new <code>LessSource</code>.
   * <p>
   * This will read the metadata and content of the LESS source, and will automatically resolve the imports.
   * </p>
   * 
   * @param file The <code>File</code> reference to the LESS source to read.
   * @throws FileNotFoundException If the LESS source (or one of its imports) could not be found.
   * @throws IOException If the LESS source cannot be read.
   */
  public LessSource(final File file, final String folder) throws FileNotFoundException, IOException
  {
    if (file == null) {
      throw new IllegalArgumentException("File must not be null.");
    }
    if (!file.exists()) {
      throw new FileNotFoundException("File " + file.getAbsolutePath() + " not found.");
    }
    this.file = file;
    this.content = this.normalizedContent = FileUtils.readFileToString(file);
    resolveImports(folder);
  }

  /**
   * Constructs a new <code>LessSource</code>.
   * <p>
   * This will read the metadata and content of the LESS source, and will automatically resolve the imports.
   * </p>
   * 
   * @param file The <code>InputStream</code> reference to the LESS source to read.
   * @throws IOException If the LESS source cannot be read.
   */
  public LessSource(final InputStream stream, final File file, final String folder) throws IOException
  {
    if (stream == null) {
      throw new IllegalArgumentException("Stream must not be null.");
    }
    this.file = file;
    this.content = this.normalizedContent = IOUtils.toString(stream, "UTF-8");
    Log.info("Less content: " + this.content);
    resolveImports(folder);
  }

  /**
   * Returns the absolute pathname of the LESS source.
   * 
   * @return The absolute pathname of the LESS source.
   */
  public String getAbsolutePath()
  {
    return file.getAbsolutePath();
  }

  /**
   * Returns the content of the LESS source.
   * 
   * @return The content of the LESS source.
   */
  public String getContent()
  {
    return content;
  }

  /**
   * Returns the normalized content of the LESS source.
   * <p>
   * The normalized content represents the LESS source as a flattened source where import statements have been resolved
   * and replaced by the actual content.
   * </p>
   * 
   * @return The normalized content of the LESS source.
   */
  public String getNormalizedContent()
  {
    return normalizedContent;
  }

  /**
   * Returns the time that the LESS source was last modified.
   * 
   * @return A <code>long</code> value representing the time the file was last modified, measured in milliseconds since
   *         the epoch (00:00:00 GMT, January 1, 1970).
   */
  public long getLastModified()
  {
    return file.lastModified();
  }

  /**
   * Returns the time that the LESS source, or one of its imports, was last modified.
   * 
   * @return A <code>long</code> value representing the time the file was last modified, measured in milliseconds since
   *         the epoch (00:00:00 GMT, January 1, 1970).
   */
  public long getLastModifiedIncludingImports()
  {
    long lastModified = getLastModified();
    for (final Map.Entry<String, LessSource> entry : imports.entrySet()) {
      final LessSource importedLessSource = entry.getValue();
      final long importedLessSourceLastModified = importedLessSource.getLastModifiedIncludingImports();
      if (importedLessSourceLastModified > lastModified) {
        lastModified = importedLessSourceLastModified;
      }
    }
    return lastModified;
  }

  /**
   * Returns the LESS sources imported by this LESS source.
   * <p>
   * The returned imports are represented by a <code>Map&lt;String, LessSource&gt;</code> which contains the filename
   * and the <code>LessSource</code>.
   * </p>
   * 
   * @return The LESS sources imported by this LESS source.
   */
  public Map<String, LessSource> getImports()
  {
    return imports;
  }

  private void resolveImports(String folder) throws FileNotFoundException, IOException
  {
    Matcher importMatcher = IMPORT_PATTERN.matcher(normalizedContent);
    while (importMatcher.find()) {
      String importedFile = importMatcher.group(2);
      log.info("Less file to import: " + importedFile);
      importedFile = importedFile.matches(".*\\.(le?|c)ss$") ? importedFile : importedFile + ".less";
      final boolean css = importedFile.matches(".*css$");
      if (!css) {
        LessSource importedLessSource = null;
        try {
          importedLessSource = new LessSource(new File(file.getParentFile(), importedFile), folder);
        } catch (FileNotFoundException e) {
          String pathToLessFile = folder + "/" + importedFile;
          String subpath = "";
          if (importedFile.contains("/")) {
            subpath = "/" + (importedFile.split("/"))[0];
          }
          log.info("Path to less file: " + pathToLessFile);
          importedLessSource = new LessSource(
              getClass().getClassLoader().getResourceAsStream(pathToLessFile),
              new File(getClass().getClassLoader().getResource(pathToLessFile).toExternalForm()), folder + subpath);
        }
        imports.put(importedFile, importedLessSource);
        normalizedContent = normalizedContent.substring(0, importMatcher.start())
            + importedLessSource.getNormalizedContent() + normalizedContent.substring(importMatcher.end());
        importMatcher = IMPORT_PATTERN.matcher(normalizedContent);
      }
    }
  }
}
