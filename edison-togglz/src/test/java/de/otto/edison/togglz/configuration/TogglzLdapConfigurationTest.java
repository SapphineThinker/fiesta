package de.otto.edison.togglz.configuration;

import de.otto.edison.authentication.configuration.LdapConfiguration;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TogglzLdapConfigurationTest {

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @ImportAutoConfiguration({LdapConfiguration.class})
    static class EnableAutoConfig {
    }

    @After
    public void close() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void shouldRegisterLdapFilter() {
        this.context.register(EnableAutoConfig.class);
        TestPropertyValues.of(
                "edison.application.management.base-path=/internal",
                "edison.ldap.enabled=true",
                "edison.ldap.host=localhost",
                "edison.ldap.base-dn=test-dn",
                "edison.ldap.rdn-identifier=test-rdn").applyTo(context);
        this.context.refresh();

        assertThat(this.context.containsBean("ldapAuthenticationFilter"), is(true));
    }

    @Test
    public void shouldNotRegisterLdapFilterIfDisabled() {
        this.context.register(EnableAutoConfig.class);
        TestPropertyValues.of(
                "edison.application.management.base-path=/internal",
                "edison.ldap.enabled=false").applyTo(context);
        this.context.refresh();

        assertThat(this.context.containsBean("ldapAuthenticationFilter"), is(false));
    }

    @Test(expected = UnsatisfiedDependencyException.class)
    public void shouldValidateProperties() {
        this.context.register(EnableAutoConfig.class);
        TestPropertyValues.of(
                "edison.application.management.base-path=/internal",
                "edison.ldap.enabled=true").applyTo(context);

        this.context.refresh();
    }

}
