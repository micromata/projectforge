package org.projectforge.business.teamcal.event.ical.converter;

import java.net.URISyntaxException;

import org.projectforge.business.teamcal.event.ical.VEventComponentConverter;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.ParameterFactoryImpl;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

public class PropertyConverter implements VEventComponentConverter
{
  public Property convert(final TeamEventDO event)
  {
    return null;
  }

  @Override
  public boolean convert(TeamEventDO event, VEvent vEvent)
  {
    Property property = this.convert(event);

    if (property == null) {
      return false;
    }

    vEvent.getProperties().add(property);

    return true;
  }

  protected void parseAdditionalParameters(final ParameterList list, final String additonalParams)
  {
    if (list == null || additonalParams == null) {
      return;
    }

    ParameterFactoryImpl parameterFactory = ParameterFactoryImpl.getInstance();
    StringBuilder sb = new StringBuilder();
    boolean escaped = false;
    char[] chars = additonalParams.toCharArray();
    String name = null;

    for (char c : chars) {
      switch (c) {
        case ';':
          if (escaped == false && name != null && sb.length() > 0) {
            try {
              list.add(parameterFactory.createParameter(name, sb.toString().replaceAll("\"", "")));
            } catch (URISyntaxException e) {
              // TODO
              e.printStackTrace();
            }
            name = null;
            sb.setLength(0);
          }
          break;
        case '"':
          escaped = (escaped == false);
          break;
        case '=':
          if (escaped == false && sb.length() > 0) {
            name = sb.toString();
            sb.setLength(0);
          }
          break;
        default:
          sb.append(c);
          break;
      }
    }

    if (escaped == false && name != null && sb.length() > 0) {
      try {
        list.add(parameterFactory.createParameter(name, sb.toString().replaceAll("\"", "")));
      } catch (URISyntaxException e) {
        // TODO
        e.printStackTrace();
      }
    }
  }
}
