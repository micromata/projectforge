package org.projectforge.business.teamcal.event.ical.converter;

import java.net.URISyntaxException;
import java.util.Iterator;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
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
  public Property toVEvent(final TeamEventDO event)
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

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    boolean ownership = false;

    Organizer organizer = vEvent.getOrganizer();
    if (organizer != null) {
      Parameter organizerCNParam = organizer.getParameter(Parameter.CN);
      Parameter organizerMailParam = organizer.getParameter("EMAIL");

      String organizerCN = organizerCNParam != null ? organizerCNParam.getValue() : null;
      String organizerEMail = organizerMailParam != null ? organizerMailParam.getValue() : null;
      String organizerValue = organizer.getValue();

      // determine ownership
      if ("mailto:null".equals(organizerValue)) {
        // owner mail to is missing (apple calender tool)
        ownership = true;
      } else if (organizerCN != null && organizerCN.equals(event.getCreator().getUsername())) {
        // organizer name is user name
        ownership = true;
      } else if (organizerEMail != null && organizerEMail.equals(event.getCreator().getEmail())) {
        // organizer email is user email
        ownership = true;
      }

      // further parameters
      StringBuilder sb = new StringBuilder();
      Iterator<Parameter> iter = organizer.getParameters().iterator();

      while (iter.hasNext()) {
        final Parameter param = iter.next();
        if (param.getName() == null) {
          continue;
        }

        sb.append(";");
        sb.append(param.toString());
      }

      if (sb.length() > 0) {
        // remove first ';'
        event.setOrganizerAdditionalParams(sb.substring(1));
      }

      if ("mailto:null".equals(organizerValue) == false) {
        event.setOrganizer(organizer.getValue());
      }
    } else {
      // some clients, such as thunderbird lightning, do not send an organizer -> pf has ownership
      ownership = true;
    }

    event.setOwnership(ownership);
    return false;
  }
}
