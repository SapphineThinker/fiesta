package de.otto.edison.logging.ui;

import de.otto.edison.navigation.NavBar;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class LoggersAcceptanceTest {


    @Autowired
    private ConfigurableApplicationContext ctx;
    @Autowired
    private TestRestTemplate template;

    @Test
    public void shouldRemoveLoggersEndpoint() {
        assertThat(ctx.containsBean("loggersMvcEndpoint"), is(false));
    }

    @Test
    public void shouldHaveLoggersAsHtml() {
        final ResponseEntity<String> response = template.getForEntity("/internal/loggers.html", String.class);
        assertThat(response.getStatusCodeValue(), is(200));
        assertThat(response.getHeaders().getContentType().isCompatibleWith(TEXT_HTML), is(true));
    }

    @Test
    public void shouldHaveLoggersAsJson() {
        final ResponseEntity<String> response = template.getForEntity("/internal/loggers.json", String.class);
        assertThat(response.getStatusCodeValue(), is(200));
        assertThat(response.getHeaders().getContentType().isCompatibleWith(APPLICATION_JSON), is(true));
    }

    @Test
    public void shouldGetLoggerAsJson() {
        final ResponseEntity<String> response = template.getForEntity("/internal/loggers/ROOT.json", String.class);
        assertThat(response.getStatusCodeValue(), is(200));
        assertThat(response.getHeaders().getContentType().isCompatibleWith(APPLICATION_JSON), is(true));
        assertThat(response.getBody(), is("{\"configuredLevel\":null,\"effectiveLevel\":\"INFO\"}"));
    }

    @Test
    public void shouldPostLoggerAsJson() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(singletonList(APPLICATION_JSON));
        headers.setContentType(APPLICATION_JSON);
        final ResponseEntity<String> postResponse = template.exchange("/internal/loggers/ROOT", POST, new HttpEntity<>("{\"configuredLevel\":\"WARN\"}", headers), String.class);
        assertThat(postResponse.getStatusCodeValue(), is(200));

        final ResponseEntity<String> response = template.exchange("/internal/loggers/ROOT", GET, new HttpEntity<>("", headers), String.class);
        assertThat(response.getStatusCodeValue(), is(200));
        assertThat(response.getHeaders().getContentType().isCompatibleWith(APPLICATION_JSON), is(true));
        assertThat(response.getBody(), is("{\"configuredLevel\":\"WARN\",\"effectiveLevel\":\"WARN\"}"));
    }

    @Test
    public void shouldHaveLoggersInRightNavBar() {
        final NavBar rightNavBar = ctx.getBean("rightNavBar", NavBar.class);
        assertThat(rightNavBar.getItems().stream().anyMatch(item->item.getTitle().equals("Loggers")), is(true));
    }
}