package de.otto.edison.jobs.repository;

import de.otto.edison.jobs.domain.JobInfo;
import de.otto.edison.jobs.domain.JobMessage;
import de.otto.edison.jobs.domain.RunningJobs;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @since 1.0.0
 */
public interface JobLockRepository {

    /**
     * Marks a job as running or throws JobBlockException if it is either disabled, was marked running before or is
     * blocked by some other job from the mutex group. This operation must be implemented atomically on the persistent
     * datastore (i. e. test and set) to make sure a job is never marked as running twice.
     *
     * @param job the job to be marked
     * @param jobTypesMutex a list of job types that must not be marked running in order to mark this job.
     *                      The jobType to be marked will be contained in this set.
     * @throws JobBlockedException if at least one of the jobTypes in the jobTypesMutex set is already marked running.
     */
    void markJobAsRunningIfPossible(JobInfo job) throws JobBlockedException;

    /**
     * Clears the job running mark of the jobType. Does nothing if not mark exists.
     *
     * @param jobType the job type
     */
    void clearRunningMark(String jobType);

    /**
     * @return All Running Jobs as specified by the markJobAsRunningIfPossible method.
     */
    RunningJobs runningJobs();

    /**
     * Disables a job type, i.e. prevents it from being started
     *
     * @param jobType the disabled job type
     */
    void disableJobType(String jobType);

    /**
     * Reenables a job type that was disabled
     *
     * @param jobType the enabled job type
     */
    void enableJobType(String jobType);

    /**
     * @return a list of all job types that are currently disabled
     */
    List<String> findDisabledJobTypes();

    void deleteAll();

    long size();
}
