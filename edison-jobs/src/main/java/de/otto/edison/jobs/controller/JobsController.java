package de.otto.edison.jobs.controller;

import static java.util.stream.Collectors.toList;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import static de.otto.edison.jobs.controller.JobRepresentation.representationOf;
import static de.otto.edison.jobs.controller.UrlHelper.baseUriOf;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import de.otto.edison.jobs.domain.JobInfo;
import de.otto.edison.jobs.service.JobService;

@Controller
@ConditionalOnProperty(name = "edison.jobs.web.controller.enabled", havingValue = "true", matchIfMissing = true)
public class JobsController {

    private static final Logger LOG = LoggerFactory.getLogger(JobsController.class);

    @Autowired
    private JobService jobService;
    @Value("${server.context-path}")
    private String serverContextPath;


    public JobsController() {
    }

    JobsController(final JobService jobService) {
        this.jobService = jobService;
    }

    @RequestMapping(value = "/internal/jobs", method = GET, produces = "text/html")
    public ModelAndView getJobsAsHtml(@RequestParam(value = "type", required = false) String type,
                                      @RequestParam(value = "count", defaultValue = "100") int count,
                                      @RequestParam(value = "distinct", defaultValue = "true", required = false) boolean distinct,
                                      HttpServletRequest request) {
        final List<JobRepresentation> jobRepresentations = getJobInfos(type, count, distinct).stream()
                .map((j) -> representationOf(j, true, baseUriOf(request)))
                .collect(toList());

        final ModelAndView modelAndView = new ModelAndView("jobs");
        modelAndView.addObject("jobs", jobRepresentations);
        return modelAndView;
    }

    @RequestMapping(value = "/internal/jobs", method = GET, produces = "application/json")
    @ResponseBody
    public List<JobRepresentation> getJobsAsJson(@RequestParam(value = "type", required = false) String type,
                                                 @RequestParam(value = "count", defaultValue = "100") int count,
                                                 @RequestParam(value = "distinct", defaultValue = "true", required = false) boolean distinct,
                                                 HttpServletRequest request) {
        return getJobInfos(type, count, distinct)
                .stream()
                .map((j) -> representationOf(j, false, baseUriOf(request)))
                .collect(toList());
    }

    @RequestMapping(value = "/internal/jobs", method = DELETE)
    public void deleteJobs(@RequestParam(value = "type", required = false) String type) {
        jobService.deleteJobs(Optional.ofNullable(type));
    }

    /**
     * Starts a new job of the specified type, if no such job is currently running.
     * <p>
     * The method will return immediately, without waiting for the job to complete.
     * <p>
     * If a job with same type is running, the response will have HTTP status 409 CONFLICT,
     * otherwise HTTP 204 NO CONTENT is returned, together with the response header 'Location',
     * containing the full URL of the running job.
     *
     * @param jobType  the type of the started job
     * @param request  the HttpServletRequest used to determine the base URL
     * @param response the HttpServletResponse used to set the Location header
     * @throws IOException in case the job was not able to start properly (ie. conflict)
     */
    @RequestMapping(
            value = "/internal/jobs/{jobType}",
            method = POST)
    public void startJob(final @PathVariable String jobType,
                         final HttpServletRequest request,
                         final HttpServletResponse response) throws IOException {
        final Optional<String> jobId = jobService.startAsyncJob(jobType);
        if (jobId.isPresent()) {
            response.setHeader("Location", baseUriOf(request) + "/internal/jobs/" + jobId.get());
            response.setStatus(SC_NO_CONTENT);
        } else {
            response.sendError(SC_CONFLICT);
        }
    }

    @RequestMapping(
            value = "/internal/jobs/{jobType}/disable",
            method = POST
    )
    public String disableJobType(final @PathVariable String jobType) {
        jobService.disableJobType(jobType);
        return "redirect:/internal/jobdefinitions";
    }

    @RequestMapping(
            value = "/internal/jobs/{jobType}/enable",
            method = POST
    )
    public String enableJobType(final @PathVariable String jobType) {
        jobService.enableJobType(jobType);
        return "redirect:/internal/jobdefinitions";
    }

    @RequestMapping(value = "/internal/jobs/{id}", method = GET, produces = "text/html")
    public ModelAndView getJobAsHtml(final HttpServletRequest request,
                                     final HttpServletResponse response,
                                     @PathVariable("id") final String jobId) throws IOException {

        setCorsHeaders(response);

        final Optional<JobInfo> optionalJob = jobService.findJob(jobId);
        if (optionalJob.isPresent()) {
            final ModelAndView modelAndView = new ModelAndView("job");
            modelAndView.addObject("job", representationOf(optionalJob.get(), true, baseUriOf(request)));
            return modelAndView;
        } else {
            response.sendError(SC_NOT_FOUND, "Job not found");
            return null;
        }
    }

    @RequestMapping(value = "/internal/jobs/{id}", method = GET, produces = "application/json")
    @ResponseBody
    public JobRepresentation getJob(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    @PathVariable("id") final String jobId) throws IOException {

        setCorsHeaders(response);

        final Optional<JobInfo> optionalJob = jobService.findJob(jobId);
        if (optionalJob.isPresent()) {
            return representationOf(optionalJob.get(), false, baseUriOf(request));
        } else {
            response.sendError(SC_NOT_FOUND, "Job not found");
            return null;
        }
    }

    private void setCorsHeaders(final HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Methods", "GET");
        response.setHeader("Access-Control-Allow-Origin", "*");
    }

    private List<JobInfo> getJobInfos(String type, int count, boolean distinct) {
        final List<JobInfo> jobInfos;
        if (type == null && distinct) {
            jobInfos = jobService.findJobsDistinct();
        } else {
            jobInfos = jobService.findJobs(Optional.ofNullable(type), count);
        }
        return jobInfos;
    }
}
