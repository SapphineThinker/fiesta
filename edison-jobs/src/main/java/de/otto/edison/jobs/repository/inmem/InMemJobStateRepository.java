package de.otto.edison.jobs.repository.inmem;

import de.otto.edison.jobs.repository.JobStateRepository;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemJobStateRepository implements JobStateRepository {

    private final Map<String, Map<String, String>> map = new ConcurrentHashMap<>();

    @Override
    public void setValue(String jobType, String key, String value) {
        map.putIfAbsent(jobType, new ConcurrentHashMap<>());
        map.get(jobType).put(key, value);
    }

    @Override
    public String getValue(String jobType, String key) {
        return map.getOrDefault(jobType, Collections.emptyMap()).get(key);
    }

    @Override
    public void deleteAll() {
        map.clear();
    }

    @Override
    public boolean createValue(String jobType, String key, String value) {
        if (getValue(jobType, key) == null) {
            setValue(jobType, key, value);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "InMemJobStateRepository";
    }

}
