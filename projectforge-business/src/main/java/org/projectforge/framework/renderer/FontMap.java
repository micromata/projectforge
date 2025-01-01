/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.renderer;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BaseFont;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains all additional load fonts from the font base directory (FOP). Original code from Wolfgang Jung from Micromata's SvgCombine.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class FontMap
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FontMap.class);

  private Map<String, BaseFont> fontMap = new HashMap<>();

  public void loadFonts(File fontDir)
  {
    @SuppressWarnings("unchecked")
    final Collection<File> files = FileUtils.listFiles(fontDir, new String[] { "afm"}, true); // Read all afm files recursively.
    if (CollectionUtils.isNotEmpty(files)) {
      for (File file : files) {
        BaseFont font;
        try {
          font = BaseFont.createFont(file.getAbsolutePath(), BaseFont.CP1252, BaseFont.EMBEDDED);
        } catch (DocumentException | IOException ex) {
          log.error("Error while loading font '" + file.getAbsolutePath() + "': " + ex.getMessage(), ex);
          continue;
        }
        final String fontName = font.getPostscriptFontName();
        fontMap.put(fontName, font);
      }
    }
  }
}
