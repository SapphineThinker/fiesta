package de.otto.edison.status.domain;

import org.junit.Test;

import static de.otto.edison.status.domain.Datasource.datasource;
import static de.otto.edison.status.domain.DatasourceDependencyBuilder.*;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class DatasourceDependencyBuilderTest {

    @Test
    public void shouldBuildDatasourceDependency() {
        final DatasourceDependency dependency = datasourceDependency(datasource("foo:42/bar"))
                .withType("test")
                .withSubtype("unittest")
                .withName("name")
                .withDescription("description")
                .build();
        assertThat(dependency.getName()).isEqualTo("name");
        assertThat(dependency.getDescription()).isEqualTo("description");
        assertThat(dependency.getType()).isEqualTo("test");
        assertThat(dependency.getSubtype()).isEqualTo("unittest");
        assertThat(dependency.getDatasources()).contains(datasource("foo", 42, "bar"));
    }

    @Test
    public void shouldCopyDatasource() {
        DatasourceDependency dependency = mongoDependency(singletonList(datasource("foo"))).build();
        assertThat(dependency).isEqualTo(copyOf(dependency).build());
        assertThat(dependency.hashCode()).isEqualTo(copyOf(dependency).build().hashCode());
    }

    @Test
    public void shouldBuildMongoDatasource() {
        DatasourceDependency dependency = mongoDependency(singletonList(datasource("foo"))).build();
        assertThat(dependency.getDatasources()).contains(datasource("foo", -1, ""));
        assertThat(dependency.getType()).isEqualTo("db");
        assertThat(dependency.getSubtype()).isEqualTo("MongoDB");
    }

    @Test
    public void shouldBuildCassandraDatasource() {
        DatasourceDependency dependency = cassandraDependency(datasource("foo")).build();
        assertThat(dependency.getDatasources()).contains(datasource("foo", -1, ""));
        assertThat(dependency.getType()).isEqualTo("db");
        assertThat(dependency.getSubtype()).isEqualTo("Cassandra");
    }

    @Test
    public void shouldBuildRedisDatasource() {
        DatasourceDependency dependency = redisDependency(datasource("foo")).build();
        assertThat(dependency.getDatasources()).contains(datasource("foo", -1, ""));
        assertThat(dependency.getType()).isEqualTo("db");
        assertThat(dependency.getSubtype()).isEqualTo("Redis");
    }

    @Test
    public void shouldBuildElasticSearchDatasource() {
        DatasourceDependency dependency = elasticSearchDependency(datasource("foo")).build();
        assertThat(dependency.getDatasources()).contains(datasource("foo", -1, ""));
        assertThat(dependency.getType()).isEqualTo("db");
        assertThat(dependency.getSubtype()).isEqualTo("ElasticSearch");
    }

    @Test
    public void shouldBuildKafkaDatasource() {
        DatasourceDependency dependency = kafkaDependency(datasource("foo")).build();
        assertThat(dependency.getDatasources()).contains(datasource("foo", -1, ""));
        assertThat(dependency.getType()).isEqualTo("queue");
        assertThat(dependency.getSubtype()).isEqualTo("Kafka");
    }



}