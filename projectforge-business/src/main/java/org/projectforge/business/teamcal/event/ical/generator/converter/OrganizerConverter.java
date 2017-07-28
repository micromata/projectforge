package org.projectforge.business.teamcal.event.ical.generator.converter;

import java.net.URISyntaxException;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.Organizer;

public class OrganizerConverter extends PropertyConverter
{
  private boolean useBlankOrganizer;

  public OrganizerConverter(boolean useBlankOrganizer)
  {
    this.useBlankOrganizer = useBlankOrganizer;
  }

  @Override
  public Property convert(final TeamEventDO event)
  {
    // TODO improve ownership handling
    try {
      if (event.isOwnership() != null && event.isOwnership()) {
        ParameterList param = new ParameterList();
        param.add(new Cn(event.getCreator().getFullname()));
        param.add(CuType.INDIVIDUAL);
        param.add(Role.CHAIR);
        param.add(PartStat.ACCEPTED);

        if (this.useBlankOrganizer) {
          return new Organizer(param, "mailto:null");
        } else {
          return new Organizer(param, "mailto:" + event.getCreator().getEmail());
        }
      } else if (event.getOrganizer() != null) {
        // read owner from
        ParameterList param = new ParameterList();
        this.parseAdditionalParameters(param, event.getOrganizerAdditionalParams());
        if (param.getParameter(Parameter.CUTYPE) == null) {
          param.add(CuType.INDIVIDUAL);
        }
        if (param.getParameter(Parameter.ROLE) == null) {
          param.add(Role.CHAIR);
        }
        if (param.getParameter(Parameter.PARTSTAT) == null) {
          param.add(PartStat.ACCEPTED);
        }
        return new Organizer(param, event.getOrganizer());
      } else {
        // TODO use better default value here
        return new Organizer("mailto:null");
      }
    } catch (URISyntaxException e) {
      // TODO handle exception, write default?
      // e.printStackTrace();
    }

    return null;
  }
}
