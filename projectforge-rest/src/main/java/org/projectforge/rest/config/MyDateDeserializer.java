package org.projectforge.rest.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyDateDeserializer extends JsonDeserializer<Date> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MyDateDeserializer.class);

  private static final String ISO_DATE_PATTERN = "[0-9]{4}-[0-9]{2}-[0-9]{2}";
  private static final String ISO_TIME_PATTERN = "[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}";
  // 2003-01-25 08:39:30.000
  private static final String[][] patterns = new String[][]{
          {"yyyy-MM-dd HH:mm:ss.SSS", ISO_DATE_PATTERN  + " " + ISO_TIME_PATTERN},
          {"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", ISO_DATE_PATTERN + "T" + ISO_TIME_PATTERN + "Z"},
          {"yyyy-MM-dd", ISO_DATE_PATTERN},
  };

  @Override
  public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    String date = jsonParser.getText();
    if (StringUtils.isBlank(date)) {
      return null;
    }
    try {
      for (String[] pattern : patterns) {
        if (date.matches(pattern[1])) {
          return new SimpleDateFormat(pattern[0]).parse(date);
        }
      }
      log.error("Unsupported date format: " + date);
      return null;
    } catch (ParseException ex) {
      log.error(ex.getMessage(), ex);
      return null;
    }
  }
}
