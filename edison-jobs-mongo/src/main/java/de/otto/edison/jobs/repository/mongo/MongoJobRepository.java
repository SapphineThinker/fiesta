package de.otto.edison.jobs.repository.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.otto.edison.jobs.domain.JobInfo;
import de.otto.edison.jobs.domain.JobInfo.JobStatus;
import de.otto.edison.jobs.domain.JobMessage;
import de.otto.edison.jobs.domain.Level;
import de.otto.edison.jobs.repository.JobBlockedException;
import de.otto.edison.jobs.repository.JobRepository;
import de.otto.edison.mongo.AbstractMongoRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static de.otto.edison.jobs.domain.JobInfo.newJobInfo;
import static de.otto.edison.jobs.domain.JobMessage.jobMessage;
import static de.otto.edison.jobs.repository.mongo.DateTimeConverters.toDate;
import static de.otto.edison.jobs.repository.mongo.DateTimeConverters.toOffsetDateTime;
import static de.otto.edison.jobs.repository.mongo.JobStructure.*;
import static java.time.Clock.systemDefaultZone;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Date.from;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

@Repository(value = "jobRepository")
public class MongoJobRepository extends AbstractMongoRepository<String, JobInfo> implements JobRepository {

    private static final int DESCENDING = -1;
    public static final String COLLECTION_NAME = "jobinfo";
    public static final String RUNNING_JOBS_COLLECTION_NAME = "runningJobs";
    public static final String NO_LOG_MESSAGE_FOUND = "No log message found";
    public static final String RUNNING_JOBS_DOCUMENT = "RUNNING_JOBS";

    private final MongoCollection<Document> jobInfoCollection;
    private final MongoCollection<Document> runningJobsCollection;
    private final Clock clock;

    @Autowired
    public MongoJobRepository(final MongoDatabase database) {
        this(database, systemDefaultZone());
    }

    @PostConstruct
    public void initRunningJobsDocumentOnStartup() {
        if(runningJobsCollection.count(byId(RUNNING_JOBS_DOCUMENT))==0) {
            runningJobsCollection.insertOne(new Document("_id", RUNNING_JOBS_DOCUMENT));
        }
    }

    MongoJobRepository(final MongoDatabase database, final Clock clock) {
        this.jobInfoCollection = database.getCollection(COLLECTION_NAME);
        this.runningJobsCollection = database.getCollection(RUNNING_JOBS_COLLECTION_NAME);
        this.clock = clock;
    }

    @Override
    public JobStatus findStatus(String jobId) {
        return JobStatus.valueOf(collection()
                .find(byId(jobId))
                .projection(new Document(STATUS.key(), true))
                .first().getString(STATUS.key()));
    }

    @Override
    public void removeIfStopped(final String id) {
        findOne(id).ifPresent(jobInfo -> {
            if (jobInfo.isStopped()) {
                collection().deleteOne(byId(id));
            }
        });
    }

    @Override
    public void appendMessage(String jobId, JobMessage jobMessage) {
        Document document = new Document("$push", new Document(MESSAGES.key(), encodeJobMessage(jobMessage)));
        collection().updateOne(byId(jobId), document);
    }

    @Override
    public JobInfo startJob(JobInfo jobInfo, Set<String> blockingJobs) throws JobBlockedException {
        String jobId = newJobId();
        assertNotBlockedByOtherJobAndSet(blockingJobs, jobInfo.getJobType(), jobId);
        JobInfo jobInfoWithId = jobInfo.copy().setJobId(jobId).build();
        return createOrUpdate(jobInfoWithId);
    }

    private void assertNotBlockedByOtherJobAndSet(Set<String> blockingJobs, String jobType, String jobId) {
        Document query = byId(RUNNING_JOBS_DOCUMENT);
        for(String blockingJob: blockingJobs) {
            query.append(blockingJob, new Document("$exists", false));
        }
        Document updatedRunningJobsDocument = runningJobsCollection.findOneAndUpdate(query, new Document("$set", new Document(jobType, jobId)));
        if(updatedRunningJobsDocument==null)  {
            throw new JobBlockedException("Blocked by some other job");
        }
    }

    private String newJobId() {
        return randomUUID().toString();
    }

    @Override
    public List<JobInfo> findLatest(final int maxCount) {
        return collection()
                .find()
                .sort(orderByStarted(DESCENDING))
                .limit(maxCount)
                .map(this::decode)
                .into(new ArrayList<>());
    }

    @Override
    public List<JobInfo> findLatestBy(final String type, final int maxCount) {
        return collection()
                .find(byType(type))
                .sort(orderByStarted(DESCENDING))
                .limit(maxCount)
                .map(this::decode)
                .into(new ArrayList<>());
    }

    @Override
    public List<JobInfo> findLatestFinishedBy(final String type, final JobStatus status, final int maxCount) {
        return collection()
                .find(byTypeAndStatus(type, status).append(STOPPED.key(), singletonMap("$exists", true)))
                .sort(orderByStarted(DESCENDING))
                .limit(maxCount)
                .map(this::decode)
                .into(new ArrayList<>());
    }

    @Override
    public List<JobInfo> findByType(final String type) {
        return collection()
                .find(byType(type))
                .sort(orderByStarted(DESCENDING))
                .map(this::decode)
                .into(new ArrayList<>());
    }

    @Override
    public List<JobInfo> findRunningWithoutUpdateSince(final OffsetDateTime timeOffset) {
        return collection()
                .find(new Document()
                        .append(STOPPED.key(), singletonMap("$exists", false))
                        .append(LAST_UPDATED.key(), singletonMap("$lt", from(timeOffset.toInstant()))))
                .map(this::decode)
                .into(new ArrayList<>());
    }

    @Override
    protected final Document encode(final JobInfo job) {
        final Document document = new Document()
                .append(JobStructure.ID.key(), job.getJobId())
                .append(JOB_TYPE.key(), job.getJobType())
                .append(STARTED.key(), toDate(job.getStarted()))
                .append(LAST_UPDATED.key(), toDate(job.getLastUpdated()))
                .append(MESSAGES.key(), job.getMessages().stream()
                        .map(MongoJobRepository::encodeJobMessage)
                        .collect(toList()))
                .append(STATUS.key(), job.getStatus().name())
                .append(HOSTNAME.key(), job.getHostname());
        if (job.isStopped()) {
            document.append(STOPPED.key(), toDate(job.getStopped().get()));
        }
        return document;
    }

    private static Document encodeJobMessage(JobMessage jm) {
        return new Document() {{
            put(MSG_LEVEL.key(), jm.getLevel().name());
            put(MSG_TS.key(), toDate(jm.getTimestamp()));
            put(MSG_TEXT.key(), jm.getMessage());
        }};
    }

    @Override
    protected final JobInfo decode(final Document document) {
        return newJobInfo(
                document.getString(JobStructure.ID.key()),
                document.getString(JOB_TYPE.key()),
                toOffsetDateTime(document.getDate(STARTED.key())),
                toOffsetDateTime(document.getDate(LAST_UPDATED.key())),
                ofNullable(toOffsetDateTime(document.getDate(STOPPED.key()))),
                JobStatus.valueOf(document.getString(STATUS.key())),
                getMessagesFrom(document),
                clock,
                document.getString(HOSTNAME.key()));
    }

    @SuppressWarnings("unchecked")
    private List<JobMessage> getMessagesFrom(final Document document) {
        List<Document> messages = (List<Document>) document.get(MESSAGES.key());
        if (messages != null) {
            return messages.stream()
                    .map(this::toJobMessage)
                    .collect(toList());
        } else {
            return emptyList();
        }
    }

    private JobMessage toJobMessage(final Document document) {
        return jobMessage(
                Level.valueOf(document.get(MSG_LEVEL.key()).toString()),
                getMessage(document),
                toOffsetDateTime(document.getDate(MSG_TS.key()))
        );
    }

    @Override
    protected final String keyOf(JobInfo value) {
        return value.getJobId();
    }

    @Override
    protected final MongoCollection<Document> collection() {
        return jobInfoCollection;
    }

    @Override
    protected final void ensureIndexes() {
        collection().createIndex(new BasicDBObject(JOB_TYPE.key(), 1));
        collection().createIndex(new BasicDBObject(STARTED.key(), 1));
    }

    private String getMessage(Document document) {
        return document.get(MSG_TEXT.key()) == null ? NO_LOG_MESSAGE_FOUND : document.get(MSG_TEXT.key()).toString();
    }

    private Document byType(final String type) {
        return new Document(JOB_TYPE.key(), type);
    }

    private Document byTypeAndStatus(final String type, final JobStatus status) {
        return new Document(JOB_TYPE.key(), type).append(STATUS.key(), status.name());
    }

    private Document orderByStarted(final int order) {
        return new Document(STARTED.key(), order);
    }

}
