package de.otto.edison.status.configuration;

import de.otto.edison.status.domain.ApplicationInfo;
import de.otto.edison.status.domain.SystemInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

@Configuration
public class SystemInfoConfiguration {

    private static final String defaultHostname;

    static {
        String localHost = null;
        try {
            localHost = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException ignored) { }
        defaultHostname = localHost;
    }

    @Value("${HOSTNAME:}")
    private String hostname;
    @Value("${server.hostname:}")
    private String envhostname;
    @Value("${server.port:}")
    private int port;

    @Bean
    @ConditionalOnMissingBean(SystemInfo.class)
    public SystemInfo systemInfo() {
        return SystemInfo.systemInfo(hostname(), port);
    }

    private String hostname() {
        if (envhostname != null && !envhostname.isEmpty()) {
            return envhostname;
        }
        if(hostname != null && !hostname.isEmpty()) {
            return hostname;
        }
        return defaultHostname;
    }
}
