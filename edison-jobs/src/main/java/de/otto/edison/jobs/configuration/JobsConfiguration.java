package de.otto.edison.jobs.configuration;

import de.otto.edison.jobs.definition.JobDefinition;
import de.otto.edison.jobs.repository.JobRepository;
import de.otto.edison.jobs.repository.JobMetaRepository;
import de.otto.edison.jobs.repository.cleanup.DeleteSkippedJobs;
import de.otto.edison.jobs.repository.cleanup.KeepLastJobs;
import de.otto.edison.jobs.repository.cleanup.StopDeadJobs;
import de.otto.edison.jobs.repository.inmem.InMemJobRepository;
import de.otto.edison.jobs.repository.inmem.InMemJobMetaRepository;
import de.otto.edison.jobs.service.JobDefinitionService;
import de.otto.edison.jobs.service.JobMutexGroups;
import de.otto.edison.jobs.service.JobService;
import de.otto.edison.jobs.status.JobStatusCalculator;
import de.otto.edison.jobs.status.JobStatusDetailIndicator;
import de.otto.edison.status.domain.Status;
import de.otto.edison.status.indicator.CompositeStatusDetailIndicator;
import de.otto.edison.status.indicator.StatusDetailIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static de.otto.edison.status.domain.StatusDetail.statusDetail;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.stream.Collectors.toList;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(JobsProperties.class)
public class JobsConfiguration {

    public static final Logger LOG = LoggerFactory.getLogger(JobsConfiguration.class);

    private final JobMutexGroups mutexGroups;
    private final JobsProperties jobsProperties;

    @Autowired
    public JobsConfiguration(final JobsProperties jobsProperties, final JobMutexGroups jobMutexGroups) {
        this.mutexGroups = jobMutexGroups;
        this.jobsProperties = jobsProperties;
        final Map<String, String> calculator = this.jobsProperties.getStatus().getCalculator();
        if (!calculator.containsKey("default")) {
            this.jobsProperties.getStatus().setCalculator(
                    new HashMap<String,String>() {{
                        putAll(calculator);
                        put("default", "warningOnLastJobFailed");
                    }}
            );
        }
    }

    @Bean
    @ConditionalOnMissingBean(ScheduledExecutorService.class)
    public ScheduledExecutorService scheduledExecutorService() {
        return newScheduledThreadPool(jobsProperties.getThreadCount());
    }

    @Bean
    @ConditionalOnMissingBean(JobMetaRepository.class)
    public JobMetaRepository jobMetaRepository() {
        return new InMemJobMetaRepository();
    }

    @Bean
    @ConditionalOnMissingBean(JobRepository.class)
    public JobRepository jobRepository() {
        LOG.warn("===============================");
        LOG.warn("Using in-memory JobRepository");
        LOG.warn("===============================");
        return new InMemJobRepository();
    }

    @Bean
    @ConditionalOnMissingBean(KeepLastJobs.class)
    public KeepLastJobs keepLastJobsStrategy(final JobRepository jobRepository) {
        return new KeepLastJobs(jobRepository, jobsProperties.getCleanup().getNumberOfJobsToKeep());
    }

    @Bean
    @ConditionalOnMissingBean(StopDeadJobs.class)
    public StopDeadJobs deadJobStrategy(final JobService jobService) {
        return new StopDeadJobs(jobService, jobsProperties.getCleanup().getMarkDeadAfter());
    }

    @Bean
    @ConditionalOnMissingBean(DeleteSkippedJobs.class)
    public DeleteSkippedJobs deleteSkippedJobsStrategy(final JobRepository jobRepository) {
        return new DeleteSkippedJobs(jobRepository, jobsProperties.getCleanup().getNumberOfSkippedJobsToKeep());
    }

    @Bean
    public JobStatusCalculator warningOnLastJobFailed(final JobRepository jobRepository) {
        return JobStatusCalculator.warningOnLastJobFailed("warningOnLastJobFailed", jobRepository);
    }

    @Bean
    public JobStatusCalculator errorOnLastJobFailed(final JobRepository jobRepository) {
        return JobStatusCalculator.errorOnLastJobFailed("errorOnLastJobFailed", jobRepository);
    }

    @Bean
    public JobStatusCalculator errorOnLastThreeJobsFailed(final JobRepository jobRepository) {
        return JobStatusCalculator.errorOnLastNumJobsFailed("errorOnLastThreeJobsFailed", 3, jobRepository);
    }

    @Bean
    public JobStatusCalculator errorOnLastTenJobsFailed(final JobRepository jobRepository) {
        return JobStatusCalculator.errorOnLastNumJobsFailed("errorOnLastTenJobsFailed", 10, jobRepository);
    }

    @Bean
    @ConditionalOnProperty(name = "edison.jobs.status.enabled", havingValue = "true", matchIfMissing = true)
    public StatusDetailIndicator jobStatusDetailIndicator(final JobDefinitionService service,
                                                          final List<JobStatusCalculator> calculators) {
        final List<JobDefinition> jobDefinitions = service.getJobDefinitions();

        if (jobDefinitions.isEmpty()) {
            return () -> statusDetail("Jobs", Status.OK, "No job definitions configured in application.");
        } else {
            return new CompositeStatusDetailIndicator("Jobs",
                    jobDefinitions
                            .stream()
                            .map(d -> new JobStatusDetailIndicator(d, findJobStatusCalculator(d.jobType(), calculators)))
                            .collect(toList())
            );
        }
    }

    private JobStatusCalculator findJobStatusCalculator(final String jobType,
                                                        final List<JobStatusCalculator> calculators) {
        final Map<String, String> statusCalculators = jobsProperties.getStatus().getCalculator();
        final String calculator;
        final String normalizedJobType = jobType.toLowerCase().replace(" ", "-");
        if (statusCalculators.containsKey(normalizedJobType)) {
            calculator = statusCalculators.get(normalizedJobType);
        } else {
            calculator = statusCalculators.get("default");
        }
        return calculators
                .stream()
                .filter(c -> calculator.equalsIgnoreCase(c.getKey()))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Unable to find JobStatusCalculator " + calculator));
    }

}
