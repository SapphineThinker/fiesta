package de.otto.edison.jobs.status;

import de.otto.edison.jobs.domain.JobInfo;
import de.otto.edison.jobs.repository.JobRepository;
import de.otto.edison.status.domain.Status;
import de.otto.edison.status.domain.StatusDetail;
import de.otto.edison.status.indicator.StatusDetailIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.otto.edison.status.domain.Status.ERROR;
import static de.otto.edison.status.domain.Status.OK;
import static de.otto.edison.status.domain.Status.WARNING;
import static java.time.OffsetDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;


public class JobStatusDetailIndicator implements StatusDetailIndicator {
    private static final Logger LOG = LoggerFactory.getLogger(JobStatusDetailIndicator.class);

    public static final String SUCCESS_MESSAGE = "Last job was successful";
    public static final String ERROR_MESSAGE = "Job had an error";
    public static final String JOB_TOO_OLD_MESSAGE = "Job didn't run in the past ";

    private final JobRepository jobRepository;
    private final String name;
    private final String jobType;
    private final Optional<Duration> maxAge;
    private final Status jobErrorMapping;

    public JobStatusDetailIndicator(final JobRepository jobRepository,
                                    final String name,
                                    final String jobType,
                                    final Optional<Duration> maxAge,
                                    final Status jobErrorMapping) {
        this.jobRepository = jobRepository;
        this.name = name;
        this.jobType = jobType;
        this.maxAge = maxAge;
        this.jobErrorMapping = jobErrorMapping;
    }

    @Override
    public StatusDetail statusDetail() {
        try {
            List<JobInfo> jobs = jobRepository.findLatestBy(jobType, 1);
            return jobs.isEmpty() ? statusDetailWhenNoJobAvailable() : toStatusDetail(jobs.get(0));
        } catch (final Exception e) {
            LOG.error("could not retrieve job status");
            return StatusDetail.statusDetail(name, ERROR, "could not retrieve job status");
        }
    }

    private StatusDetail toStatusDetail(final JobInfo jobInfo) {
        Status status;
        String message;

        switch(jobInfo.getStatus()) {
            case OK:
                if(jobTooOld(jobInfo)) {
                    status = WARNING;
                    message = JOB_TOO_OLD_MESSAGE + (maxAge.isPresent() ? maxAge.get() : "N/A");
                } else {
                    status = OK;
                    message = SUCCESS_MESSAGE;
                }
                break;

            case ERROR:
                status = jobErrorMapping;
                message = ERROR_MESSAGE;
                break;

            case DEAD:
            default:
                status = WARNING;
                message = ERROR_MESSAGE;
        }
        return StatusDetail.statusDetail(name, status, message, runningDetailsFor(jobInfo));
    }

    private StatusDetail statusDetailWhenNoJobAvailable() {
        return StatusDetail.statusDetail(name, Status.OK, SUCCESS_MESSAGE);
    }

    private Map<String, String> runningDetailsFor(final JobInfo jobInfo) {
        Map<String, String> details = new HashMap<>();
        String uri = jobInfo.getJobId();
        details.put("uri", uri);
        if (!jobInfo.getStopped().isPresent()) {
            details.put("running", uri);
        } else {
            details.put("stopped", ISO_DATE_TIME.format(jobInfo.getStopped().get()));
        }
        details.put("started", ISO_DATE_TIME.format(jobInfo.getStarted()));

        return details;
    }

    private boolean jobTooOld(final JobInfo jobInfo) {
        final Optional<OffsetDateTime> stopped = jobInfo.getStopped();
        if (stopped.isPresent() && maxAge.isPresent()) {
            OffsetDateTime deadlineToRerun = stopped.get().plus(maxAge.get());
            return deadlineToRerun.isBefore(now());
        }

        return false;
    }
}
