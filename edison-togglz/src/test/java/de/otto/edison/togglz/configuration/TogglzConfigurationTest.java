package de.otto.edison.togglz.configuration;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

public class TogglzConfigurationTest {

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @After
    public void close() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void shouldRegisterTogglzConsoleServlet() {
        this.context.register(TogglzConfiguration.class);
        this.context.register(InMemoryFeatureStateRepositoryConfiguration.class);
        addEnvironment(this.context, "management.context-path=/internal");
        this.context.refresh();

        assertThat(this.context.containsBean("togglzFilter"), is(true));
        assertThat(this.context.containsBean("featureClassProvider"), is(true));
        assertThat(this.context.containsBean("userProvider"), is(true));
        assertThat(this.context.containsBean("togglzConfig"), is(true));
        assertThat(this.context.containsBean("featureManager"), is(true));
    }

}
