package net.bhl.matsim.uam.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

import net.bhl.matsim.uam.passenger.UAMRequest;

import java.util.Map;

/**
 * Event that is generated when the drop off occurs,
 * in order to know which person was dropped off.
 * 
 * @author balacm
 *
 */
public class UAMTransitEvent extends Event implements HasPersonId {
    final private UAMRequest request;

    public UAMTransitEvent(UAMRequest request, double time) {
        super(time);
        this.request = request;
    }

    public UAMRequest getRequest() {
        return request;
    }

    @Override
    public String getEventType() {
        return "UAMTransit";
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put("person", request.getPassenger().getId().toString());
       // attr.put("distance", String.valueOf(request.getRoute().getDistance()));
        return attr;
    }

    @Override
    public Id<Person> getPersonId() {
        return request.getPassenger().getId();
    }
}
