package de.otto.edison.jobs.domain;

import de.otto.edison.jobs.definition.JobDefinition;
import net.jcip.annotations.ThreadSafe;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.otto.edison.jobs.domain.JobInfo.JobStatus.*;
import static de.otto.edison.jobs.domain.JobMessage.jobMessage;
import static de.otto.edison.jobs.domain.Level.INFO;
import static de.otto.edison.jobs.domain.Level.WARNING;
import static java.lang.String.format;
import static java.time.OffsetDateTime.now;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * Information about a single job execution.
 * <p>
 * A JobInfo instance is created for every job execution. It is constantly updated by the background job and
 * persisted in the JobRepository.
 */
@ThreadSafe
public class JobInfo {
    private static final String JOB_DEAD_MESSAGE = "Job didn't receive updates for a while, considering it dead";

    private final Clock clock;
    private final URI jobUri;
    private final String jobType;
    private final OffsetDateTime started;
    private final List<JobMessage> messages = new ArrayList<>();
    private Optional<OffsetDateTime> stopped;
    private JobStatus status;
    private OffsetDateTime lastUpdated;
    private String hostname;

    public enum JobStatus {OK, ERROR, DEAD}

    public static JobInfo newJobInfo(final URI jobUri, final String jobType,
                                     final Clock clock, final String hostname) {
        return new JobInfo(jobType, jobUri, clock, hostname);
    }

    public static JobInfo newJobInfo(final URI jobUri,
                                     final String jobType,
                                     final OffsetDateTime started,
                                     final OffsetDateTime lastUpdated,
                                     final Optional<OffsetDateTime> stopped,
                                     final JobStatus status,
                                     final List<JobMessage> messages,
                                     final Clock clock,
                                     final String hostname) {
        return new JobInfo(jobUri, jobType, started, lastUpdated, stopped, status, messages, clock, hostname);
    }

    private JobInfo(final String jobType, final URI jobUri, final Clock clock, final String hostname) {
        this.clock = clock;
        this.jobUri = jobUri;
        this.jobType = jobType;
        this.started = now(clock);
        this.stopped = empty();
        this.status = OK;
        this.lastUpdated = started;
        this.hostname = hostname;
    }

    private JobInfo(final URI jobUri,
                    final String jobType,
                    final OffsetDateTime started,
                    final OffsetDateTime lastUpdated,
                    final Optional<OffsetDateTime> stopped,
                    final JobStatus status,
                    final List<JobMessage> messages,
                    final Clock clock,
                    final String hostname) {
        this.clock = clock;
        this.jobUri = jobUri;
        this.jobType = jobType;
        this.started = started;
        this.lastUpdated = lastUpdated;
        this.stopped = stopped;
        this.status = status;
        this.messages.addAll(messages);
        this.hostname = hostname;
    }

    /**
     * @return true if the job is finished, false, if it is still in execution.
     */
    public synchronized boolean isStopped() {
        return stopped.isPresent();
    }

    /**
     * @return the URI of the job
     */
    public URI getJobUri() {
        return jobUri;
    }

    /**
     * @return the job type
     */
    public String getJobType() {
        return jobType;
    }

    /**
     * @return the name of the server this job is executed on
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @return timestamp when the job was started
     */
    public OffsetDateTime getStarted() {
        return started;
    }

    /**
     * @return the current status of the job: OK, ERROR or DEAD
     */
    public synchronized JobStatus getStatus() {
        return status;
    }

    /**
     * @return the timestamp when the job was stopped, of empty, if the job is still running.
     */
    public synchronized Optional<OffsetDateTime> getStopped() {
        return stopped;
    }

    /**
     * @return list of job messages, containing human-readable information about what happened during execution.
     */
    public synchronized List<JobMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * @return last updated timestamp
     */
    public synchronized OffsetDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Send a ping to the job and update the lastUpdated timestamp.
     * <p>
     * This is used to determine whether or not a job is still running of a different server.
     */
    public synchronized void ping() {
        lastUpdated = now(clock);
    }

    /**
     * Add an INFO message to the job messages.
     * <p>
     * Updates the lastUpdated timestamp
     *
     * @param message a message string
     * @return the updated JobInfo
     */
    public synchronized JobInfo info(final String message) {
        messages.add(jobMessage(INFO, message));
        lastUpdated = now(clock);
        return this;
    }

    public synchronized JobInfo warn(String message) {
        messages.add(jobMessage(WARNING, message));
        lastUpdated = now(clock);
        return this;
    }

    /**
     * Add an ERROR message to the job messages.
     * <p>
     * Updates the lastUpdated timestamp. The
     * Status of the job is set to ERROR.
     *
     * @param message a message string
     * @return the updated JobInfo
     */
    public synchronized JobInfo error(final String message) {
        messages.add(jobMessage(Level.ERROR, message));
        lastUpdated = now(clock);
        status = ERROR;
        return this;
    }

    /**
     * Jobs can be restarted after an ERROR or if an Exception occured during execution if
     * the {@link JobDefinition#restarts()} is greater 0.
     *
     * @return the updated JobInfo
     */
    public synchronized JobInfo restart() {
        messages.add(jobMessage(WARNING, format("Restarting job ..")));
        lastUpdated = now(clock);
        status = OK;
        return this;
    }

    /**
     * This is called if the job was finished.
     * <p>
     * Updates the lastUpdated and stopped timestamp
     *
     * @return the updated JobInfo
     */
    public synchronized JobInfo stop() {
        lastUpdated = now(clock);
        stopped = of(lastUpdated);
        return this;
    }

    /**
     * This is called if the job was identified to be dead.
     * <p>
     * Updates the lastUpdated and stopped timestamp.
     * The job status is set to DEAD
     *
     * @return the updated JobInfo
     */
    public synchronized JobInfo dead() {
        messages.add(jobMessage(WARNING, JOB_DEAD_MESSAGE));
        lastUpdated = now(clock);
        stopped = of(lastUpdated);
        status = DEAD;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobInfo jobInfo = (JobInfo) o;

        if (jobType != null ? !jobType.equals(jobInfo.jobType) : jobInfo.jobType != null) return false;
        if (jobUri != null ? !jobUri.equals(jobInfo.jobUri) : jobInfo.jobUri != null) return false;
        if (lastUpdated != null ? !lastUpdated.equals(jobInfo.lastUpdated) : jobInfo.lastUpdated != null) return false;
        if (messages != null ? !messages.equals(jobInfo.messages) : jobInfo.messages != null) return false;
        if (started != null ? !started.equals(jobInfo.started) : jobInfo.started != null) return false;
        if (status != jobInfo.status) return false;
        if (stopped != null ? !stopped.equals(jobInfo.stopped) : jobInfo.stopped != null) return false;
        if (hostname != null ? !hostname.equals(jobInfo.hostname) : jobInfo.hostname != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = jobUri != null ? jobUri.hashCode() : 0;
        result = 31 * result + (jobType != null ? jobType.hashCode() : 0);
        result = 31 * result + (started != null ? started.hashCode() : 0);
        result = 31 * result + (stopped != null ? stopped.hashCode() : 0);
        result = 31 * result + (messages != null ? messages.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (lastUpdated != null ? lastUpdated.hashCode() : 0);
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JobInfo{" +
                "jobUri=" + jobUri +
                ", jobType=" + jobType +
                ", started=" + started +
                ", hostname=" + hostname +
                ", stopped=" + stopped +
                ", messages=" + messages +
                ", status=" + status +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
