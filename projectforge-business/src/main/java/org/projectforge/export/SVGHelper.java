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

package org.projectforge.export;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.projectforge.business.gantt.GanttRelationType;
import org.projectforge.common.StringHelper;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SVGHelper
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SVGHelper.class);

  private static final String SVG_NS = SVGDOMImplementation.SVG_NAMESPACE_URI;

  private static final String XML_NS = "http://www.w3.org/1999/xlink";

  public enum ArrowDirection
  {
    LEFT, RIGHT;
  }

  public static Document createDocument(final double width, final double height)
  {
    final DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
    final Document doc = impl.createDocument(SVG_NS, "svg", null);
    final Element root = doc.getDocumentElement();
    root.setAttributeNS(null, "xmlns:xlink", XML_NS);
    setAttribute(root, "width", width);
    setAttribute(root, "height", height);
    return doc;
  }

  public static void setAttribute(final Element element, final String attrName, final Object value)
  {
    if (value == null) {
      element.setAttribute(attrName, "");
    } else {
      element.setAttribute(attrName, String.valueOf(value));
    }
  }

  public static String drawHorizontalConnectionLine(final GanttRelationType type, final double x1, final double y1, final double x2,
      final double y2, final double minXDist)
  {
    checkNonNegativeValues("x1, y1, x2, y2", x1, y1, x2, y2);
    checkPositiveValues("mixXDist", minXDist);
    final double xHalf = (x2 - x1) / 2;
    final double yHalf = (y2 - y1) / 2;
    final StringBuffer buf = new StringBuffer();
    buf.append("M ").append(round(x1)).append(" ").append(round(y1)); // (x1, y1)
    if (type == GanttRelationType.FINISH_START || type == null) {
      if (xHalf > minXDist) {
        buf.append(" L ").append(round(x1 + xHalf)).append(" ").append(round(y1)); // (x1 + x_half, y1), xHalf may be negative.
        buf.append(" L ").append(round(x1 + xHalf)).append(" ").append(round(y2)); // (x1 + x_half, y2), xHalf may be negative.
      } else {
        buf.append(" L ").append(round(x1 + minXDist)).append(" ").append(round(y1)); // (x1 + dist, y1)
        buf.append(" L ").append(round(x1 + minXDist)).append(" ").append(round(y1 + yHalf)); // (x1 + dist, y1 + yHalf), yHalf may be
        // negative.
        buf.append(" L ").append(round(x2 - minXDist)).append(" ").append(round(y1 + yHalf)); // (x1 - dist, y1 + yHalf), yHalf may be
        // negative.
        buf.append(" L ").append(round(x2 - minXDist)).append(" ").append(round(y2)); // (x1 - dist, y2), yHalf may be negative.
      }
    } else if (type == GanttRelationType.START_FINISH) {
      if (xHalf > minXDist && x2 < x1) {
        buf.append(" L ").append(round(x1 + xHalf)).append(" ").append(round(y1)); // (x1 + x_half, y1)
        buf.append(" L ").append(round(x1 + xHalf)).append(" ").append(round(y2)); // (x1 + x_half, y2)
      } else {
        buf.append(" L ").append(round(x1 - minXDist)).append(" ").append(round(y1)); // (x1 - dist, y1)
        buf.append(" L ").append(round(x1 - minXDist)).append(" ").append(round(y1 + yHalf)); // (x1 - dist, y1 + yHalf), yHalf may be
        // negative.
        buf.append(" L ").append(round(x2 + minXDist)).append(" ").append(round(y1 + yHalf)); // (x1 + dist, y1 + yHalf), yHalf may be
        // negative.
        buf.append(" L ").append(round(x2 + minXDist)).append(" ").append(round(y2)); // (x1 + dist, y2), yHalf may be negative.
      }
    } else if (type.isIn(GanttRelationType.START_START, GanttRelationType.FINISH_FINISH)) {
      final double x;
      if (type == GanttRelationType.START_START) {
        x = x1 < x2 ? x1 - minXDist : x2 - minXDist;
      } else {
        x = x1 < x2 ? x2 + minXDist : x1 + minXDist;
      }
      buf.append(" L ").append(round(x)).append(" ").append(round(y1)); // (x1 + x_half, y1), xHalf may be negative.
      buf.append(" L ").append(round(x)).append(" ").append(round(y2)); // (x1 + x_half, y2), xHalf may be negative.
    }
    buf.append(" L ").append(round(x2)).append(" ").append(round(y2)); // (x2, y2).
    return buf.toString();
  }

  public static String drawArrow(final ArrowDirection direction, final double x, final double y, final double size)
  {
    checkNonNegativeValues("x, y", x, y);
    checkPositiveValues("size", size);
    final StringBuffer buf = new StringBuffer();
    if (direction == ArrowDirection.LEFT) {
      buf.append("M ").append(round(x)).append(" ").append(round(y)); // (x, y)
      buf.append(" L ").append(round(x + size)).append(" ").append(round(y + size)); // (x + size, y + size)
      buf.append(" L ").append(round(x + size)).append(" ").append(round(y - size)); // (x + size, y - size)
    } else if (direction == ArrowDirection.RIGHT) {
      buf.append("M ").append(round(x)).append(" ").append(round(y)); // (x, y)
      buf.append(" L ").append(round(x - size)).append(" ").append(round(y - size)); // (x - size, y - size)
      buf.append(" L ").append(round(x - size)).append(" ").append(round(y + size)); // (x - size, y + size)
    } else {
      log.error("Unsupported arrow direction: " + direction);
    }
    buf.append(" z");
    return buf.toString();
  }

  public static Element createText(final Document document, final double x, final double y, final String text, final String... attributes)
  {
    if (text == null) {
      throw new IllegalArgumentException("text shouldn't be null.");
    }
    checkNonNegativeValues("x, y", x, y);
    if (log.isDebugEnabled() == true) {
      log.debug("createText: x=" + x + ", y=" + y + ", text=" + text);
    }
    final Element el = createElement(document, "text", prepend(attributes, "x", round(x), "y", round(y)));
    el.appendChild(document.createTextNode(text));
    return el;
  }

  public static Element createPath(final Document document, final String fill, final double strokeWidth, final String stroke,
      final String path, final String... attributes)
  {
    checkPositiveValues("strokeWidth", strokeWidth);
    final Element el = createElement(document, "path", prepend(attributes, "fill", fill, "stroke-width", strokeWidth, "stroke", stroke,
        "d", path));
    return el;
  }

  public static Element createPath(final Document document, final SVGColor fillColor, final double strokeWidth, final SVGColor strokeColor,
      final String path, final String... attributes)
  {
    return createPath(document, fillColor.getName(), strokeWidth, strokeColor.getName(), path, attributes);
  }

  public static Element createRect(final Document document, final double x, final double y, final double width, final double height,
      final String fill, final String... attributes)
  {
    checkPositiveValues("width, height", width, height);
    checkNonNegativeValues("x, y", x, y);
    if (log.isDebugEnabled() == true) {
      log.debug("createRect: x="
          + x
          + ", y="
          + y
          + ", width="
          + width
          + ", height="
          + height
          + ", fill="
          + fill
          + ", attributes="
          + StringHelper.listToString(",", StringHelper.listToString(",", attributes)));
    }
    final Element el = createElement(document, "rect", prepend(attributes, "x", round(x), "y", round(y), "width", round(width), "height",
        round(height), "fill", fill));
    return el;
  }

  public static Element createRect(final Document document, final double x, final double y, final double width, final double height,
      final SVGColor fillColor, final String... attributes)
  {
    return createRect(document, x, y, width, height, fillColor.getName(), attributes);
  }

  public static Element createRect(final Document document, final double x, final double y, final double width, final double height,
      final SVGColor fillColor, final SVGColor strokeColor, final String... attributes)
  {
    return createRect(document, x, y, width, height, fillColor.getName(), prepend(attributes, "stroke", strokeColor.getName()));
  }

  public static Element createLine(final Document document, final double x1, final double y1, final double x2, final double y2,
      final SVGColor strokeColor, final String... attributes)
  {
    return createLine(document, x1, y1, x2, y2, prepend(attributes, "stroke", strokeColor.getName()));
  }

  public static Element createLine(final Document document, final double x1, final double y1, final double x2, final double y2,
      final String... attributes)
  {
    checkNonNegativeValues("x1, y1, x2, y2", x1, y1, x2, y2);
    if (log.isDebugEnabled() == true) {
      log.debug("createLine: x1="
          + x1
          + ", y1="
          + y1
          + ", x2="
          + x2
          + ", y2="
          + y2
          + ", attributes="
          + StringHelper.listToString(",", attributes));
    }
    final Element el = createElement(document, "line", prepend(attributes, "x1", round(x1), "y1", round(y1), "x2", round(x2), "y2",
        round(y2)));
    return el;
  }

  public static Element createUse(final Document document, final String id, final double x, final double y, final String... attributes)
  {
    checkNonNegativeValues("x, y", x, y);
    final Element el = createElement(document, "use", prepend(attributes, "xlink:href", id, "x", round(x), "y", round(y)));
    return el;
  }

  public static Element createElement(final Document document, final String tag, final SVGColor fillColor, final String... attributes)
  {
    return createElement(document, tag, prepend(attributes, "fill", fillColor.getName()));
  }

  public static Element createElement(final Document document, final String tag, final String... attributes)
  {
    final Element el = document.createElementNS(SVG_NS, tag);
    if (attributes != null) {
      for (int i = 0; i < attributes.length - 1; i += 2) {
        final String attr = attributes[i];
        final String value = attributes[i + 1];
        final String ns;
        if ("xlink:href".equals(attr) == true) {
          ns = XML_NS;
        } else {
          ns = null;
        }
        el.setAttributeNS(ns, attr, value);
      }
    }
    return el;
  }

  static String[] prepend(final String[] array, final Object... values)
  {
    if (values == null) {
      return array;
    }
    final String[] joinedArray = new String[values.length + array.length];
    for (int i = 0; i < values.length; i++) {
      joinedArray[i] = String.valueOf(values[i]);
    }
    System.arraycopy(array, 0, joinedArray, values.length, array.length);
    return joinedArray;
  }

  static String[] append(final String[] array, final Object... values)
  {
    if (values == null) {
      return array;
    }
    final String[] joinedArray = new String[values.length + array.length];
    System.arraycopy(array, 0, joinedArray, 0, array.length);
    for (int i = 0; i < values.length; i++) {
      joinedArray[array.length + i] = String.valueOf(values[i]);
    }
    return joinedArray;
  }

  static void checkNonNegativeValues(final String varnames, final double... values)
  {
    for (final double value : values) {
      if (value < 0 || Double.isNaN(value) == true || Double.isInfinite(value) == true) {
        throw new IllegalArgumentException("Values should be positive and valid: {"
            + varnames
            + "}="
            + StringHelper.doublesToString(", ", values));
      }
    }
  }

  static void checkPositiveValues(final String varnames, final double... values)
  {
    for (final double value : values) {
      if (value <= 0 || Double.isNaN(value) == true || Double.isInfinite(value) == true) {
        throw new IllegalArgumentException("Values should be positive or zero and valid: {"
            + varnames
            + "}="
            + StringHelper.doublesToString(", ", values));
      }
    }
  }

  public static String round(final double value)
  {
    return FORMAT_PRECISION_2.get().format(value);
  }

  /**
   * yyyy-MM-dd HH:mm:ss.S in UTC. Thread safe usage: FOR_TESTCASE_OUTPUT_FORMATTER.get().format(date)
   */
  private static final ThreadLocal<DecimalFormat> FORMAT_PRECISION_2 = new ThreadLocal<DecimalFormat>() {
    @Override
    protected DecimalFormat initialValue()
    {
      final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
      symbols.setDecimalSeparator('.');
      return new DecimalFormat("###.##", symbols);
    }
  };
}
