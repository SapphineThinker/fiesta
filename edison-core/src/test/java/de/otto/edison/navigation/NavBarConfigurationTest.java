package de.otto.edison.navigation;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static de.otto.edison.navigation.NavBarItem.top;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

public class NavBarConfigurationTest {

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @After
    public void close() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void shouldHaveRightNavBar() {
        addEnvironment(context, "management.context-path=/internal");
        context.register(NavBarConfiguration.class);
        context.refresh();

        final NavBar rightNavBar = context.getBean("rightNavBar", NavBar.class);
        assertThat(rightNavBar.getItems(), hasSize(1));

        final NavBarItem item = rightNavBar.getItems().get(0);
        assertThat(item.getLink(), is("/internal/status"));
        assertThat(item.getTitle(), is("Status"));
        assertThat(item.getPosition(), is(top()));
    }

    @Test
    public void shouldHaveEmptyMainNavBar() {
        context.register(NavBarConfiguration.class);
        context.refresh();

        final NavBar mainNavBar = context.getBean("mainNavBar", NavBar.class);
        assertThat(mainNavBar.getItems(), hasSize(0));
    }
}

