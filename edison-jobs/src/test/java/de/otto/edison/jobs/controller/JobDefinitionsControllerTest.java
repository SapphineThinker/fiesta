package de.otto.edison.jobs.controller;

import de.otto.edison.jobs.definition.JobDefinition;
import de.otto.edison.jobs.repository.JobRepository;
import de.otto.edison.jobs.service.JobDefinitionService;
import de.otto.edison.jobs.service.JobRunnable;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.otto.edison.jobs.controller.JobDefinitionRepresentation.representationOf;
import static de.otto.edison.jobs.controller.Link.link;
import static de.otto.edison.jobs.definition.DefaultJobDefinition.fixedDelayJobDefinition;
import static de.otto.edison.jobs.definition.DefaultJobDefinition.manuallyTriggerableJobDefinition;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Test
public class JobDefinitionsControllerTest {

    JobDefinitionsController controller;

    @Mock
    JobDefinitionService jobDefinitionService;

    @Mock
    JobRepository jobRepository;

    private MockMvc mockMvc;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        controller = new JobDefinitionsController(jobDefinitionService, jobRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void shouldReturn404IfJobDefinitionIsUnknown() throws Exception {
        when(jobDefinitionService.getJobDefinition("FooJob")).thenReturn(Optional.empty());
        mockMvc.perform(get("/internal/jobdefinitions/FooJob"))
                .andExpect(status().is(404));
    }

    @Test
    public void shouldReturnJobDefinitionIfJobExists() throws Exception {
        // given
        final String jobType = "FooJob";
        final JobDefinition expectedDef = jobDefinition(jobType, "Foo");
        when(jobDefinitionService.getJobDefinition(jobType)).thenReturn(Optional.of(expectedDef));

        // when
        mockMvc.perform(
                get("/internal/jobdefinitions/FooJob")
                        .accept("application/json")
        )
                .andExpect(status().is(200))
                .andExpect(content().json("{\"type\":\"FooJob\"," +
                        "\"name\":\"Foo\"," +
                        "\"retries\":0," +
                        "\"fixedDelay\":3600," +
                        "\"links\":[" +
                        "{\"href\":\"/internal/jobsdefinitions/FooJob\",\"rel\":\"self\"},{\"href\":\"/internal/jobdefinitions\",\"rel\":\"collection\"}," +
                        "{\"href\":\"/internal/jobs/FooJob\",\"rel\":\"http://github.com/otto-de/edison/link-relations/job/trigger\"}" +
                        "]" +
                        "}"));
    }

    @Test
    public void shouldReturnAllJobDefinitions() throws Exception {
        // given
        final JobDefinition fooJobDef = jobDefinition("FooJob", "Foo");
        final JobDefinition barJobDef = jobDefinition("BarJob", "Bar");
        when(jobDefinitionService.getJobDefinitions()).thenReturn(asList(fooJobDef, barJobDef));

        // when
        mockMvc.perform(
                get("/internal/jobdefinitions/")
                        .accept("application/json")
        )
                .andExpect(status().is(200))
                .andExpect(content().json("{" +
                        "\"links\":[" +
                        "{\"href\":\"/internal/jobdefinitions/FooJob\",\"rel\":\"http://github.com/otto-de/edison/link-relations/job/definition\",\"title\":\"Foo\"}," +
                        "{\"href\":\"/internal/jobdefinitions/BarJob\",\"rel\":\"http://github.com/otto-de/edison/link-relations/job/definition\",\"title\":\"Bar\"}," +
                        "{\"href\":\"/internal/jobdefinitions\",\"rel\":\"self\",\"title\":\"Self\"}" +
                        "]" +
                        "}"));
    }

    @Test
    public void shouldReturnAllJobDefinitionsAsHtml() throws Exception {
        // given
        final JobDefinition fooJobDef = jobDefinition("FooJob", "Foo");
        final JobDefinition barJobDef = notTriggerableDefinition("BarJob", "Bar");
        when(jobDefinitionService.getJobDefinitions()).thenReturn(asList(fooJobDef, barJobDef));
        when(jobRepository.findDisabledJobTypes()).thenReturn(asList("BarJob"));

        // when
        mockMvc.perform(
                get("/internal/jobdefinitions/")
                        .accept("text/html")
        )
                .andExpect(status().is(200))
                .andDo(result -> {
                    Map<String, Object> model = result.getModelAndView().getModel();
                    List<Map<String, Object>> jobDefinitions = (List<Map<String, Object>>) model.get("jobdefinitions");
                    assertThat(jobDefinitions.size(), is(2));
                    assertThat(jobDefinitions.get(0).get("frequency"), is("Every 60 Minutes"));
                    assertThat(jobDefinitions.get(1).get("frequency"), is("Never"));
                    List<String> disabledJobs = (List<String>) model.get("disabledJobs");
                    assertThat(disabledJobs, hasSize(1));
                    assertThat(disabledJobs, contains("BarJob"));
                });
    }

    @Test
    public void shouldConvertToSecondsIfSecondsIsLessThan60() throws Exception {
        // Given
        final JobDefinition jobDef = jobDefinition("TheJob", "Job", ofSeconds(59));
        when(jobDefinitionService.getJobDefinitions()).thenReturn(asList(jobDef));

        // when
        // when
        mockMvc.perform(
                get("/internal/jobdefinitions/")
                        .accept("text/html")
        )
                .andExpect(status().is(200))
                .andDo(result -> {
                    List<Map<String, Object>> jobDefinitions = (List<Map<String, Object>>) result.getModelAndView().getModel().get("jobdefinitions");
                    assertThat(jobDefinitions.size(), is(1));
                    assertThat(jobDefinitions.get(0).get("frequency"), is("Every 59 Seconds"));
                });
    }

    @Test
    public void shouldConvertToMinutesIfSecondsIsNotLessThan60() throws Exception {
        // Given
        final JobDefinition jobDef = jobDefinition("TheJob", "Job", ofSeconds(60));
        when(jobDefinitionService.getJobDefinitions()).thenReturn(asList(jobDef));

        // when
        // when
        mockMvc.perform(
                get("/internal/jobdefinitions/")
                        .accept("text/html")
        )
                .andExpect(status().is(200))
                .andDo(result -> {
                    List<Map<String, Object>> jobDefinitions = (List<Map<String, Object>>) result.getModelAndView().getModel().get("jobdefinitions");
                    assertThat(jobDefinitions.size(), is(1));
                    assertThat(jobDefinitions.get(0).get("frequency"), is("Every 1 Minutes"));
                });
    }

    private JobDefinition jobDefinition(final String jobType, final String name) {
        return jobDefinition(jobType, name, ofHours(1));
    }

    private JobDefinition jobDefinition(final String jobType, final String name, Duration fixedDelay) {
        return fixedDelayJobDefinition(jobType, name, name, fixedDelay, 0, empty());
    }

    private JobDefinition notTriggerableDefinition(final String jobType, final String name) {
        return manuallyTriggerableJobDefinition(jobType, name, name, 0, empty());
    }

}