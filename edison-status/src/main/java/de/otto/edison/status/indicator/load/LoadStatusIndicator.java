package de.otto.edison.status.indicator.load;

import de.otto.edison.status.domain.StatusDetail;
import de.otto.edison.status.indicator.StatusDetailIndicator;
import de.otto.edison.status.indicator.load.LoadDetector.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

import static de.otto.edison.status.domain.Status.OK;
import static de.otto.edison.status.domain.Status.WARNING;

@Component
public class LoadStatusIndicator implements StatusDetailIndicator {

    private final LoadDetector loadDetector;

    @Autowired
    public LoadStatusIndicator(LoadDetector loadDetector) {
        this.loadDetector = loadDetector;
    }

    @Override
    public StatusDetail statusDetail() {
        Status status = loadDetector.getStatus();
        return StatusDetail.statusDetail("load",
                status == Status.OVERLOAD ? WARNING : OK,
                "detects whether application is under heavy load, ok or idling.",
                new LinkedHashMap<String, String>() {{
                    put("detail", status.name());
                }});
    }

}
