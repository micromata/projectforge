/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.ftlines.wicket.fullcalendar;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.WicketAjaxJQueryResourceReference;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.ISODateTimeFormat;
import org.projectforge.framework.ToStringUtil;

import java.io.IOException;

abstract class AbstractFullCalendar extends MarkupContainer implements IHeaderContributor {
  public AbstractFullCalendar(String id) {
    super(id);
  }

  // TODO see if it makes sense to switch these to Css/JavaScriptResourceReference
  private static final ResourceReference CSS = new PackageResourceReference(AbstractFullCalendar.class,
          "res/fullcalendar.css");
  private static final ResourceReference JS = new PackageResourceReference(AbstractFullCalendar.class,
          "res/fullcalendar.js");
  private static final ResourceReference JS_EXT = new PackageResourceReference(AbstractFullCalendar.class,
          "res/fullcalendar.ext.js");
  private static final ResourceReference JS_MIN = new PackageResourceReference(AbstractFullCalendar.class,
          "res/fullcalendar.min.js");

  private static ToStringUtil.Serializer dateTimeSerializer = new ToStringUtil.Serializer(DateTime.class, new DateTimeSerializer());

  private static ToStringUtil.Serializer localTimeSerializer = new ToStringUtil.Serializer(LocalTime.class, new LocalTimeSerializer());

  @Override
  public void renderHead(IHeaderResponse response) {

    response.render(JavaScriptHeaderItem.forReference(WicketAjaxJQueryResourceReference.get()));

    response.render(CssReferenceHeaderItem.forReference(CSS));

    if (getApplication().usesDeploymentConfig()) {
      response.render(JavaScriptReferenceHeaderItem.forReference(JS_MIN));
    } else {
      response.render(JavaScriptReferenceHeaderItem.forReference(JS));
    }
    response.render(JavaScriptReferenceHeaderItem.forReference(JS_EXT));

  }

  public final String toJson(Object value) {
    return ToStringUtil.toJsonStringExtended(value, dateTimeSerializer);//, localTimeSerializer);
  }


  static class DateTimeSerializer extends StdSerializer<DateTime> {
    public DateTimeSerializer() {
      super(DateTime.class);
    }

    @Override
    public void serialize(DateTime value, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
      jgen.writeString(ISODateTimeFormat.dateTime().print(value));
    }
  }

  static class LocalTimeSerializer extends StdSerializer<LocalTime> {
    public LocalTimeSerializer() {
      super(LocalTime.class);
    }

    @Override
    public void serialize(LocalTime value, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
      jgen.writeString(value.toString("h:mmaa"));
    }
  }
}
