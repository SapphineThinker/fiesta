package de.otto.edison.status.domain;

import de.otto.edison.annotations.Beta;
import net.jcip.annotations.Immutable;

import static de.otto.edison.status.domain.Criticality.NOT_SPECIFIED;

/**
 * Specifies the type of a service, including the business criticality and impact.
 */
@Beta
@Immutable
public class ServiceType {

    public static final String TYPE_REST_SERVICE = "service/rest";
    public static final String TYPE_DATA_IMPORT = "data/import/full";
    public static final String TYPE_DATA_FEED = "data/import/delta";

    /** The kind of service. One of the TYPE_* constants, or other predefined values. */
    public final String type;
    /** Criticality of the specified service for the operation of this service. */
    public final Criticality criticality;
    /** Short description of the impact of outages: what would happen if the system is not operational? */
    public final String disasterImpact;

    /**
     * Creates a ServiceType.
     *
     * @param type The type of the service dependency.
     * @param criticality The criticality of the required service for the operation of this service.
     * @param disasterImpact Short description of the impact of outages: what would happen if the system is not operational?
     *
     * @return ServiceType
     */
    public static ServiceType serviceType(final String type, final Criticality criticality, final String disasterImpact) {
        return new ServiceType(type, criticality, disasterImpact);
    }

    public static ServiceType unspecifiedService() {
        return new ServiceType("not specified", NOT_SPECIFIED, "not specified");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceType that = (ServiceType) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (criticality != that.criticality) return false;
        return !(disasterImpact != null ? !disasterImpact.equals(that.disasterImpact) : that.disasterImpact != null);

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (criticality != null ? criticality.hashCode() : 0);
        result = 31 * result + (disasterImpact != null ? disasterImpact.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ServiceType{" +
                "type='" + type + '\'' +
                ", criticality=" + criticality +
                ", disasterImpact='" + disasterImpact + '\'' +
                '}';
    }

    private ServiceType(final String type, final Criticality criticality, final String disasterImpact) {
        this.type = type;
        this.criticality = criticality;
        this.disasterImpact = disasterImpact;
    }
}
