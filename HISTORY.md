# Release History

## Release 1.0.0 "Samuel Ogden Edison, Jr."

New Features:
* Broken Jobs are automatically restarted according to the number of retries specified in the ```JobDefintion```.
* ```JobDefinition``` now has a new field ```restarts```, specifying how often a job is restarted if it failed because of an error.

Breaking Changes:
* The factory methods of the ```DefaultJobDefinition``` now have an additional parameter for the number of restarts.
* Every ```JobRunnable``` now needs to provide a ```JobDefinition```
* Removed ```JobRunnable#getJobType```. The job type is accessed by the ```JobDefinition```
* Removed hystrix module

## Release 0.44.0

New Features:
* MongoDB persistence: Allow to retrieve the object (including key) when it is created.
  Changed signature of ```create``` and  ```createOrUpdate``` in ```AbstractMongoRepository```
  to return the object instead of void

Bug Fixes:
* Allow also a value without key to be given to ```AbstractMongoRepository#createOrUpdate``` without throwing a NPE
* ```application.properties```: renamed ````edison.application.name```` (introduced in Release 0.35.0) back into ```spring.application.name```
  (see [ContextIdApplicationContextInitializer](https://github.com/spring-projects/spring-boot/blob/v1.3.0.RELEASE/spring-boot/src/main/java/org/springframework/boot/context/ContextIdApplicationContextInitializer.java)
  for details on identifying your application)
* Added dummy feature toggle implementations (for ```FeatureClassProvider```) to example projects, which can now
  be run on its own again (ie. ```gradle example-jobs:bootRun```)


## Release 0.43.0
* Updated spring-boot to version [1.3.0](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-1.3-Release-Notes), hystrix to 1.4.21 and metrics library dependency to 3.1.3

## Release 0.40.0
* Update togglz library to 2.2.0

## Release 0.39.2
* Cleaned up link construction on internal status and job pages
* Timestamps in JSON status representation are now in ISO-8601 date format (including timezone)

## Release 0.39.1
* Fixed possible broken links on /internal pages

## Release 0.39.0
* Added servicediscovery-client to edison-microservice
* ServiceDiscovery is only used if edison.servicediscovery.* properties are available

## Release 0.38.0
* JobDefinitions extended to support jobs that are not triggered automatically.

## Release 0.37.0
* Fixed links on internal pages

## Release 0.35.0

New Features:
* New library servicediscovery-client introduced to provide basic ServiceDiscovery features for Edison Microservices.
This lib may be used to register services at an Edison JobTrigger.

Bug Fixes:
* Fixed possible ConcurrentModificationException if job messages are accessed from JobInfo.

Breaking Changes:
* application.properties is now using server.context-path instead of server.contextPath.
* application.properties is now using edison.application.name instead of application.name.


## Release 0.34.0
* Added ID to panel with job messages for identification in JobTrigger

## Release 0.33.0
* Added UIs for Status pages
* Nicer internal pages, including reworked jobs and job-definitions

## Release 0.29.0

New Features:
* Prepared integration with (upcoming) Edison JobTrigger
* Added Bootstrap UIs for Jobs and /internal
* Added edison-service for typical microservices, including jobs, health, status,
* Using webjars in edison-service to include bootstrap and jquery

Breaking Changes:
* The format of job definitions and jobs has changed.

## Release 0.28.2

Bugs fixed:
* Fixed critical bug that prevented the startup of the server in combination with usage of edison jobs-mongo

## Release 0.28.1

Bugs fixed:
* Fixed bugs that prevented the StatusDetails of jobs to be added to status page.

## Release 0.28.0

New Features:
* Jobs are rendered with absolute URLs in JSON and HTML representations.
* Introduced the (optional) concept of JobDefinitions to describe the expected frequency etc. to trigger jobs.
* Based on the JobDefinitions, for every Job a StatusDetailIndicator is registered automatically. This may be
disabled by either not providing a JobDefinition, or by setting property edison.jobs.status.enabled=false

Breaking Changes:
* Jobs in earlier version where identified by the URI of the job including the servlet context path. Starting with
this version, the context path is not part of the identifier anymore. You should DELETE all old jobs, otherwise
the URLs a broken.
* Job representations now contain the full URL of the jobs instead of relative URIs.


