package de.otto.edison.validation.web;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import de.otto.edison.validation.validators.SafeId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletContext;
import java.util.Collections;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableAutoConfiguration
@ComponentScan("de.otto.edison.validation")
@ContextConfiguration(classes = {
        ValidationExceptionHandler.class,
        ValidationExceptionHandlerAcceptanceTest.TestConfiguration.class})
public class ValidationExceptionHandlerAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ServletContext servletContext;

    @Before
    public void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = servletContext.getContextPath();
    }

    @Test
    public void shouldValidateAndProduceErrorRepresentation() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"id\":\"_!NON_SAFE_ID!!?**\"}")
        .when()
                .put("/testing")
        .then()
                .assertThat()
                .statusCode(422).and()
                .content("errors.id[0].key", Collections.emptyList(), is("id.invalid"))
                .content("errors.id[0].message", Collections.emptyList(), is("Ungueltiger Id-Wert."))
                .content("errors.id[0].rejected", Collections.emptyList(), is("_!NON_SAFE_ID!!?**"));
    }

    public static class TestConfiguration {
        @RestController
        public static class TestController {
            @RequestMapping(value = "/testing",
                    method = RequestMethod.PUT,
                    consumes = APPLICATION_JSON_VALUE,
                    produces = APPLICATION_JSON_VALUE)
            public String doTest(@Validated @RequestBody ContentRepresentation content) {
                return "bla";
            }
        }
    }

    public static class ContentRepresentation {
        @SafeId
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}

