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

package org.projectforge.framework.renderer;

import java.io.ByteArrayOutputStream;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.projectforge.framework.xstream.XmlHelper;
import org.w3c.dom.Document;

public class BatikImageRenderer
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BatikImageRenderer.class);

  /**
   * Renders a png image from the SVG document.
   * @param document
   * @param width
   * @param imageFormat The image file format of the output.
   * @return
   */
  public static byte[] getByteArray(final Document document, final int width, final ImageFormat imageFormat)
  {
    if (imageFormat.isIn(ImageFormat.PNG, ImageFormat.JPEG) == true) {
      return getRasterImageByteArray(document, width, imageFormat);
    }
    if (imageFormat == ImageFormat.PDF == true) {
      return getPDFByteArray(document, width);
    }
    if (imageFormat == ImageFormat.SVG == true) {
      return getSVGByteArray(document, width);
    }
    throw new UnsupportedOperationException("Image type '" + imageFormat + "' not yet supported.");
  }

  private static byte[] getRasterImageByteArray(final Document document, final int width, final ImageFormat imageFormat)
  {
    // Create a image transcoder
    final ImageTranscoder t;
    if (imageFormat == ImageFormat.JPEG) {
      t = new JPEGTranscoder();
      t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(.8));
      t.addTranscodingHint(JPEGTranscoder.KEY_WIDTH, new Float(width));
      // } else if (imageFormat == ImageFormat.TIFF) {
      // t = new TIFFTranscoder();
      // t.addTranscodingHint(TIFFTranscoder.KEY_WIDTH, new Float(width));
    } else {
      t = new PNGTranscoder();
      t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(width));
    }
    // Set the transcoding hints.
    TranscoderInput input = new TranscoderInput(document);
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final TranscoderOutput output = new TranscoderOutput(baos);
    // Save the image.
    try {
      t.transcode(input, output);
    } catch (TranscoderException ex) {
      log.fatal("Exception encountered " + ex, ex);
    }
    return baos.toByteArray();
  }

  private static byte[] getSVGByteArray(final Document document, final int width)
  {
    final String result = XmlHelper.toString(document, true);
    return result.getBytes();
  }

  private static byte[] getPDFByteArray(final Document document, final int width)
  {
    // Create a pdf transcoder
    final PDFTranscoder t = new PDFTranscoder();
    t.addTranscodingHint(PDFTranscoder.KEY_AUTO_FONTS, false);
    t.addTranscodingHint(PDFTranscoder.KEY_WIDTH, new Float(width));
    TranscoderInput input = new TranscoderInput(document);
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final TranscoderOutput output = new TranscoderOutput(baos);
    // Save the image.
    try {
      t.transcode(input, output);
    } catch (TranscoderException ex) {
      log.fatal("Exception encountered " + ex, ex);
    }
    return baos.toByteArray();
  }
}
