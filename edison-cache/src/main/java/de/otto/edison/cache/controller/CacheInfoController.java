package de.otto.edison.cache.controller;

import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import de.otto.edison.cache.configuration.CaffeineCacheConfig;
import de.otto.edison.navigation.NavBar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.CachePublicMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static de.otto.edison.navigation.NavBarItem.bottom;
import static de.otto.edison.navigation.NavBarItem.navBarItem;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Controller that is responsible for serving cache metrics in JSON or HTML format.
 *
 * @since 0.76.0
 */
@Controller
@ConditionalOnBean(CaffeineCacheConfig.class)
public class CacheInfoController {

    @Autowired
    CachePublicMetrics cacheMetrics;

    @Autowired(required = false)
    List<CaffeineCacheConfig> cacheConfigs;

    @Autowired(required = false)
    List<CaffeineCache> caffeineCaches;

    @Autowired
    NavBar rightNavBar;

    @PostConstruct
    public void postConstruct() {
        rightNavBar.register(navBarItem(bottom(), "Cache Statistics", "/internal/cacheinfos"));
        if (cacheConfigs == null) {
            cacheConfigs = emptyList();
        }
        if (caffeineCaches == null) {
            caffeineCaches = emptyList();
        }
    }

    @RequestMapping(value = "/internal/cacheinfos", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String,CacheInfo> getCacheMetricsJson() {
        return enrichWithCacheSpecification(toCacheInfos(cacheMetrics));
    }

    @RequestMapping(value = "/internal/cacheinfos", method = GET, produces = MediaType.ALL_VALUE)
    @ResponseBody
    public ModelAndView getCacheMetricsHtml() {
        final Map<String, CacheInfo> metrics = getCacheMetricsJson();
        final List<CacheInfo> cacheInfos = metrics.values()
                .stream()
                .sorted(comparing(CacheInfo::getName))
                .collect(toList());
        return new ModelAndView(
                "internal/cacheinfos", "cacheInfos",
                cacheInfos);
    }

    private Map<String, CacheInfo> enrichWithCacheSpecification(final Map<String, CacheInfo> cacheInfos) {
        for (final String cacheName : cacheInfos.keySet()) {
            final Optional<CaffeineCacheConfig> cacheConfig = cacheConfigs
                    .stream()
                    .filter(c -> c.cacheName.equals(cacheName))
                    .findAny();
            if (cacheConfig.isPresent()) {
                cacheInfos.get(cacheName).setSpecification(cacheConfig.get().toMap());
            } else {
                Optional<CaffeineCache> cache = caffeineCaches
                        .stream()
                        .filter(c -> c.getName().equals(cacheName))
                        .findAny();
                if (cache.isPresent()) {
                    Policy<Object, Object> policy = cache.get().getNativeCache().policy();
                    cacheInfos.get(cacheName).setSpecification(
                        new LinkedHashMap<String,String>() {{
                            policy.eviction().ifPresent(eviction -> {
                                if (eviction.isWeighted()) {
                                    put("maximumWeight", valueOf(eviction.getMaximum()));
                                } else {
                                    put("maximumSize", valueOf(eviction.getMaximum()));
                                }
                            });
                            put("recordStats", valueOf(policy.isRecordingStats()));
                            policy.expireAfterAccess().ifPresent(expire -> {
                                put("expireAfterAccess", expire.getExpiresAfter(SECONDS) + "s");
                            });
                            policy.expireAfterWrite().ifPresent(expire -> {
                                put("expireAfterWrite", expire.getExpiresAfter(SECONDS) + "s");
                            });
                            policy.refreshAfterWrite().ifPresent(refresh -> {
                                put("refreshAfterWrite", refresh.getExpiresAfter(SECONDS) + "s");
                            });
                        }}
                    );
                }
            }
        }
        return cacheInfos;
    }

    private Map<String, CacheInfo> toCacheInfos(final CachePublicMetrics cacheMetrics) {
        final Map<String, CacheInfo> cacheInfos = new LinkedHashMap<>();
        cacheMetrics.metrics().forEach(m->{
            final String name = m.getName().substring("cache.".length());
            int pos = name.indexOf('.');
            final String cacheName = name.substring(0, pos);
            final String metricName = toCamelHumps(normalizeDotsAndDashes(name, pos)) ;
            if (!cacheInfos.containsKey(cacheName)) {
                cacheInfos.put(cacheName, new CacheInfo(cacheName));
            }
            cacheInfos.get(cacheName).setMetric(metricName, m.getValue());
        });
        return cacheInfos;
    }

    private String normalizeDotsAndDashes(final String name, final int pos) {
        return name.substring(pos+1).replace(".", "_").replace("-", "_");
    }

    private String toCamelHumps(final String input) {
        final StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for( String word : input.split("_") )
        {
            if (isFirst) {
                sb.append(word.substring(0, 1));
                isFirst = false;
            } else {
                sb.append(word.substring(0, 1).toUpperCase());
            }
            sb.append( word.substring(1).toLowerCase() );
        }
        return sb.toString();
    }
}
