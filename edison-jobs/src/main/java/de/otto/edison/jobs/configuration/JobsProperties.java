package de.otto.edison.jobs.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

/**
 * Configuration properties used to configure the behaviour of module edison-jobs.
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "edison.jobs")
public class JobsProperties {
    /** Enables / disabled the support for external triggers (->Edison JobTrigger). If false, the job controllers are not available. */
    private boolean externalTrigger = true;
    /** Number of threads available to run jobs. */
    private int threadCount = 10;
    /** Properties used to configure clean-up strategies. */
    private Cleanup cleanup = new Cleanup();
    /** Properties used to configure the reporting of job status using StatusDetailIndicators. */
    private Status status = new Status();

    public boolean isExternalTrigger() {
        return externalTrigger;
    }

    public void setExternalTrigger(boolean externalTrigger) {
        this.externalTrigger = externalTrigger;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public Cleanup getCleanup() {
        return cleanup;
    }

    public void setCleanup(Cleanup cleanup) {
        this.cleanup = cleanup;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public static class Cleanup {
        int numberOfToKeep = 100;
        int markDeadAfter = 30;

        public int getNumberOfToKeep() {
            return numberOfToKeep;
        }

        public void setNumberOfToKeep(int numberOfToKeep) {
            this.numberOfToKeep = numberOfToKeep;
        }

        public int getMarkDeadAfter() {
            return markDeadAfter;
        }

        public void setMarkDeadAfter(int markDeadAfter) {
            this.markDeadAfter = markDeadAfter;
        }
    }

    public static class Status {
        /** Enable / disable StatusDetailIndicators for jobs. */
        private boolean enabled = true;

        /** Configuration of the strategy used to map job state to StatusDetails. edison.jobs.status.calculator.default
         * is used to configure the default strategy. */
        private Map<String,String> calculator = new HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, String> getCalculator() {
            return calculator;
        }

        public void setCalculator(final Map<String, String> calculator) {
            final Map<String, String> normalized = new HashMap<>();
            calculator.entrySet().forEach(entry -> {
                String key = entry.getKey().toLowerCase().replace(" ", "-");
                normalized.put(key, entry.getValue());
            });
            this.calculator = normalized;
        }
    }
}
