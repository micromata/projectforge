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

package org.projectforge.web.gantt;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.gantt.ExportMSProject;
import org.projectforge.business.gantt.GanttChart;
import org.projectforge.business.gantt.GanttChartDO;
import org.projectforge.business.gantt.GanttChartDao;
import org.projectforge.business.gantt.GanttChartData;
import org.projectforge.business.gantt.GanttChartSettings;
import org.projectforge.business.gantt.GanttChartStyle;
import org.projectforge.business.gantt.GanttTask;
import org.projectforge.common.MimeType;
import org.projectforge.framework.renderer.BatikImageRenderer;
import org.projectforge.framework.renderer.ImageFormat;
import org.projectforge.framework.utils.FileHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.BatikImage;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.flowlayout.ImagePanel;
import org.w3c.dom.Document;

@EditPage(defaultReturnPage = GanttChartListPage.class)
public class GanttChartEditPage extends AbstractEditPage<GanttChartDO, GanttChartEditForm, GanttChartDao> implements ISelectCallerPage
{
  private static final long serialVersionUID = 6994391085420314366L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GanttChartEditPage.class);

  public static final String PARAM_KEY_TASK = "task";

  @SpringBean
  private GanttChartDao ganttChartDao;

  GanttChartData ganttChartData;

  private Component ganttImage;

  public GanttChartEditPage(final PageParameters parameters)
  {
    super(parameters, "gantt");
    init();
    if (isNew() == true) {
      final Integer taskId = WicketUtils.getAsInteger(parameters, PARAM_KEY_TASK);
      if (taskId != null) {
        getBaseDao().setTask(getData(), taskId);
      }
    }
    refresh();
  }

  void export(final String exportFormat)
  {
    final GanttChart ganttChart = createGanttChart();
    if (ganttChart == null) {
      return;
    }
    ImageFormat imageFormat = null;
    final String suffix;
    if (GanttChartEditForm.EXPORT_JPG.equals(exportFormat) == true) {
      suffix = ".jpg";
      imageFormat = ImageFormat.JPEG;
    } else if (GanttChartEditForm.EXPORT_MS_PROJECT_MPX.equals(exportFormat) == true) {
      suffix = ".mpx";
    } else if (GanttChartEditForm.EXPORT_MS_PROJECT_XML.equals(exportFormat) == true) {
      suffix = ".xml";
    } else if (GanttChartEditForm.EXPORT_PDF.equals(exportFormat) == true) {
      suffix = ".pdf";
      imageFormat = ImageFormat.PDF;
    } else if (GanttChartEditForm.EXPORT_PNG.equals(exportFormat) == true) {
      suffix = ".png";
      imageFormat = ImageFormat.PNG;
    } else if (GanttChartEditForm.EXPORT_PROJECTFORGE.equals(exportFormat) == true) {
      suffix = ".xml";
    } else if (GanttChartEditForm.EXPORT_SVG.equals(exportFormat) == true) {
      suffix = ".svg";
      imageFormat = ImageFormat.SVG;
    } else {
      log.error("Oups, exportFormat '" + exportFormat + "' not supported. Assuming png format.");
      suffix = ".png";
      imageFormat = ImageFormat.PNG;
    }
    final String filename = FileHelper.createSafeFilename(getData().getName(), suffix, 50, true);
    final byte[] content;
    if (imageFormat != null) {
      final Document document = ganttChart.create();
      content = BatikImageRenderer.getByteArray(document, ganttChart.getWidth(), imageFormat);
      DownloadUtils.setDownloadTarget(content, filename);
    } else {
      final MimeType type;
      if (GanttChartEditForm.EXPORT_MS_PROJECT_MPX.equals(exportFormat) == true) {
        content = ExportMSProject.exportMpx(ganttChart);
        type = MimeType.MS_PROJECT;
      } else if (GanttChartEditForm.EXPORT_MS_PROJECT_XML.equals(exportFormat) == true) {
        content = ExportMSProject.exportXml(ganttChart);
        type = MimeType.MS_PROJECT;
      } else {
        content = ganttChartDao.exportAsXml(ganttChart, true).getBytes();
        type = MimeType.XML;
      }
      DownloadUtils.setDownloadTarget(content, filename, type);
    }
  }

  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    getSettings().setOpenNodes(form.ganttChartEditTreeTablePanel.getOpenNodes());
    getBaseDao().writeGanttObjects(getData(), ganttChartData.getRootObject());
    return null;
  }

  private GanttChart createGanttChart()
  {
    if (ganttChartData == null) {
      return null;
    }
    ganttChartData.getRootObject().sortChildren();
    final GanttChart ganttChart = new GanttChart(ganttChartData.getRootObject(), getGanttChartStyle(), getSettings(), getData().getName());
    // chart.getRootObject().recalculate();
    return ganttChart;
  }

  protected void redraw()
  {
    final GanttChart ganttChart = createGanttChart();
    final Component oldGanttImage = ganttImage;
    if (ganttChart != null) {
      final Document document = ganttChart.create();
      if (document != null) {
        ganttImage = new ImagePanel(form.imagePanel.newChildId(), new BatikImage(ImagePanel.IMAGE_ID, document, getGanttChartStyle()
            .getWidth()));
      } else {
        ganttImage = null;
      }
    } else {
      ganttImage = null;
    }
    if (oldGanttImage != null) {
      form.imagePanel.remove(oldGanttImage);
    }
    if (ganttImage != null) {
      form.imagePanel.add(ganttImage);
    }
  }

  private GanttChartStyle getGanttChartStyle()
  {
    return getData().getStyle();
  }

  private GanttChartSettings getSettings()
  {
    return getData().getSettings();
  }

  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("taskId".equals(property) == true) {
      final Integer id;
      if (selectedValue instanceof String) {
        id = NumberHelper.parseInteger((String) selectedValue);
      } else {
        id = (Integer) selectedValue;
      }
      if (ganttChartData == null || ObjectUtils.equals(id, ganttChartData.getRootObject().getId()) == false) {
        ganttChartData = null; // Force refresh.
        form.ganttChartEditTreeTablePanel.refreshTreeTable();
      }
      getBaseDao().setTask(getData(), id);
      refresh();
    } else if ("ownerId".equals(property) == true) {
      final Integer id;
      if (selectedValue instanceof String) {
        id = NumberHelper.parseInteger((String) selectedValue);
      } else {
        id = (Integer) selectedValue;
      }
      getBaseDao().setOwner(getData(), id);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    throw new UnsupportedOperationException();
  }

  protected void refresh()
  {
    if (ganttChartData == null && getData().getTaskId() != null) {
      ganttChartData = getBaseDao().readGanttObjects(getData());
      final GanttTask rootObject = ganttChartData.getRootObject();
      if (rootObject != null && CollectionUtils.isNotEmpty(rootObject.getChildren()) == true && isNew() == true) {
        // For new charts set all children on level one as visible.
        for (final GanttTask child : rootObject.getChildren()) {
          child.setVisible(true);
        }
      }
    }
    form.ganttChartEditTreeTablePanel.setGanttChartData(ganttChartData).refresh();
    redraw();
  }

  @Override
  protected GanttChartDao getBaseDao()
  {
    return ganttChartDao;
  }

  @Override
  protected GanttChartEditForm newEditForm(final AbstractEditPage< ? , ? , ? > parentPage, final GanttChartDO data)
  {
    return new GanttChartEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
