package de.otto.edison.hateoas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import static de.otto.edison.hateoas.Link.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by guido on 05.07.16.
 */
public class HalRepresentationTest {

    @Test
    public void simpleHalRepresentationShouldContainAttributes() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation() {
            public final String first = "foo";
            public final String second = "bar";
        };
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"first\":\"foo\",\"second\":\"bar\"}"));

    }

    @Test
    public void shouldRenderSelfLinkAndAttribute() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(selfLink("http://example.org/test/foo")) {
            public final String test = "foo";
        };
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"}},\"test\":\"foo\"}"));
    }

    @Test
    public void shouldRenderMultipleLinks() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                link("self", "http://example.org/test/foo"),
                link("collection", "http://example.org/test")) {};
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"self\":{\"href\":\"http://example.org/test/foo\"},\"collection\":{\"href\":\"http://example.org/test\"}}}"));
    }

    @Test
    public void shouldRenderTemplatedLink() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(templatedLink("search", "/test{?bar}")) {};
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{\"search\":{\"href\":\"/test{?bar}\",\"templated\":true}}}"));
    }

    @Test
    public void shouldRenderEvenMoreComplexLinks() throws JsonProcessingException {
        // given
        final HalRepresentation representation = new HalRepresentation(
                templatedLinkBuilderFor("search", "/test{?bar}")
                        .withType("application/hal+json")
                        .withHrefLang("de-DE")
                        .withTitle("Some Title")
                        .withName("Foo")
                        .withProfile("http://example.org/profiles/test-profile")
                        .beeingDeprecated()
                        .build(),
                linkBuilderFor("foo", "/test/bar")
                        .withType("application/hal+json")
                        .withHrefLang("de-DE")
                        .withTitle("Some Title")
                        .withName("Foo")
                        .withProfile("http://example.org/profiles/test-profile")
                        .beeingDeprecated()
                        .build()
        ) {};
        // when
        final String json = new ObjectMapper().writeValueAsString(representation);
        // then
        assertThat(json, is("{\"_links\":{" + "" +
                "\"search\":{\"href\":\"/test{?bar}\",\"templated\":true,\"type\":\"application/hal+json\",\"hrefLang\":\"de-DE\",\"title\":\"Some Title\",\"name\":\"Foo\",\"profile\":\"http://example.org/profiles/test-profile\",\"deprecated\":true}," +
                "\"foo\":{\"href\":\"/test/bar\",\"type\":\"application/hal+json\",\"hrefLang\":\"de-DE\",\"title\":\"Some Title\",\"name\":\"Foo\",\"profile\":\"http://example.org/profiles/test-profile\",\"deprecated\":true}" +
                "}}"));
    }
}
