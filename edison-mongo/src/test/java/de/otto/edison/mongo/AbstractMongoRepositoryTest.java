package de.otto.edison.mongo;

import static java.util.stream.Collectors.toList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNot.not;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import com.github.fakemongo.Fongo;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class AbstractMongoRepositoryTest {

    private TestRepository testee;

    @Before
    public void setUp() throws Exception {
        final Fongo fongo = new Fongo("inmemory-mongodb");
        final MongoDatabase mongoDatabase = fongo.getDatabase("db");

        testee = new TestRepository(mongoDatabase);
    }

    @Test
    public void shouldUpdateExistingDocument() throws Exception {
        // given
        final TestObject testObject = new TestObject("someId", "someValue");
        testee.create(testObject);

        final TestObject testObjectToUpdate = new TestObject("someId", "someUpdatedValue");

        // when
        final boolean updated = testee.update(testObjectToUpdate);

        // then
        assertThat(updated, is(true));
        final TestObject updatedTestObject = testee.findOne("someId").get();
        assertThat(updatedTestObject.eTag, notNullValue());
        assertThat(updatedTestObject.eTag, is(not(testObject.eTag)));
        assertThat(updatedTestObject.id, is("someId"));
        assertThat(updatedTestObject.value, is("someUpdatedValue"));
    }

    @Test
    public void shouldNotUpdateMissingDocument() throws Exception {
        // given

        final TestObject testObjectToUpdate = new TestObject("someId", "someUpdatedValue");

        // when
        final boolean updated = testee.update(testObjectToUpdate);

        // then
        assertThat(updated, is(false));
        assertThat(testee.findOne("someId").isPresent(), is(false));
    }

    @Test
    public void shouldUpdateIfETagMatch() throws Exception {
        // given
        final TestObject testObject = new TestObject("someId", "someValue");
        testee.create(testObject);

        final String etagFromCreated = testee.findOne("someId").get().eTag;
        final TestObject testObjectToUpdate = new TestObject("someId", "someUpdatedValue", etagFromCreated);

        // when
        final UpdateIfMatchResult updateIfMatchResult = testee.updateIfMatch(testObjectToUpdate, etagFromCreated);
        final TestObject updatedTestObject = testee.findOne("someId").get();

        // then
        assertThat(updateIfMatchResult, is(UpdateIfMatchResult.OK));
        assertThat(updatedTestObject.eTag, notNullValue());
        assertThat(updatedTestObject.eTag, is(not(etagFromCreated)));
        assertThat(updatedTestObject.id, is("someId"));
        assertThat(updatedTestObject.value, is("someUpdatedValue"));
    }

    @Test
    public void shouldNotUpdateIfEtagNotMatch() throws Exception {
        // given
        final TestObject testObject = new TestObject("someId", "someValue", "someEtagWhichIsNotInTheDb");
        testee.create(testObject);

        // when
        final UpdateIfMatchResult updated = testee.updateIfMatch(testObject, "someOtherETag");

        // then
        assertThat(updated, is(UpdateIfMatchResult.CONCURRENTLY_MODIFIED));
    }

    @Test
    public void shouldNotUpdateIfEtagNotExists() throws Exception {
        // given
        final TestObject testObject = new TestObject("someId", "someValue");

        // when
        final UpdateIfMatchResult updated = testee.updateIfMatch(testObject, "someETag");

        // then
        assertThat(updated, is(UpdateIfMatchResult.NOT_FOUND));
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotCreateOrUpdateWithMissingId() throws Exception {
        // given
        final TestObject testObject = new TestObject(null, "someValue");

        // when
        final TestObject resultingObject = testee.createOrUpdate(testObject);

        // then
        // NullPointerException is thrown
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotCreateWithMissingId() throws Exception {
        // given
        final TestObject testObject = new TestObject(null, "someValue");

        // when
        final TestObject resultingObject = testee.create(testObject);

        // then
        // NullPointerException is thrown
    }

    @Test(expected = NullPointerException.class)
    public void shouldFindOneWithMissingId() throws Exception {
        // when
        testee.findOne(null);
        // then
        // NullPointerException is thrown
    }

    private void createTestObjects(final String... values) {
        Arrays.stream(values)
                .map(value -> new TestObject(value, value))
                .forEach(testee::create);
    }

    @Test
    public void shouldFindAllEntries() {
        createTestObjects("testObject01", "testObject02", "testObject03", "testObject04", "testObject05", "testObject06");

        // when
        final List<TestObject> foundObjects = testee.findAll();

        // then
        assertThat(foundObjects, hasSize(6));
        assertThat(foundObjects.get(0).value, is("testObject01"));
        assertThat(foundObjects.get(1).value, is("testObject02"));
        assertThat(foundObjects.get(2).value, is("testObject03"));
        assertThat(foundObjects.get(3).value, is("testObject04"));
        assertThat(foundObjects.get(4).value, is("testObject05"));
        assertThat(foundObjects.get(5).value, is("testObject06"));
    }

    @Test
    public void shouldStreamAllEntries() {
        createTestObjects("testObject01", "testObject02", "testObject03", "testObject04", "testObject05", "testObject06");

        // when
        final List<TestObject> foundObjects = testee.findAllAsStream().collect(toList());

        // then
        assertThat(foundObjects, hasSize(6));
        assertThat(foundObjects.get(0).value, is("testObject01"));
        assertThat(foundObjects.get(1).value, is("testObject02"));
        assertThat(foundObjects.get(2).value, is("testObject03"));
        assertThat(foundObjects.get(3).value, is("testObject04"));
        assertThat(foundObjects.get(4).value, is("testObject05"));
        assertThat(foundObjects.get(5).value, is("testObject06"));
    }

    @Test
    public void shouldFindAllEntriesWithSkipAndLimit() {
        createTestObjects("testObject01", "testObject02", "testObject03", "testObject04", "testObject05", "testObject06");

        // when
        final List<TestObject> foundObjects = testee.findAll(2, 3);

        // then
        assertThat(foundObjects, hasSize(3));
        assertThat(foundObjects.get(0).value, is("testObject03"));
        assertThat(foundObjects.get(1).value, is("testObject04"));
        assertThat(foundObjects.get(2).value, is("testObject05"));
    }

    @Test
    public void shouldStreamAllEntriesWithSkipAndLimit() {
        createTestObjects("testObject01", "testObject02", "testObject03", "testObject04", "testObject05", "testObject06");

        // when
        final List<TestObject> foundObjects = testee.findAllAsStream(2, 3).collect(toList());

        // then
        assertThat(foundObjects, hasSize(3));
        assertThat(foundObjects.get(0).value, is("testObject03"));
        assertThat(foundObjects.get(1).value, is("testObject04"));
        assertThat(foundObjects.get(2).value, is("testObject05"));
    }

    // ~~

    public class TestRepository extends AbstractMongoRepository<String, TestObject> {

        private final MongoCollection<Document> collection;

        public TestRepository(final MongoDatabase mongoDatabase) {
            this.collection = mongoDatabase.getCollection("test");
        }

        @Override
        protected MongoCollection<Document> collection() {
            return collection;
        }

        @Override
        protected String keyOf(final TestObject value) {
            return value.id;
        }

        @Override
        protected Document encode(final TestObject value) {
            final Document document = new Document();

            if (value.id != null) {
                document.append("_id", value.id);
            }
            document.append("etag", UUID.randomUUID().toString());
            document.append("value", value.value);

            return document;
        }

        @Override
        protected TestObject decode(final Document document) {
            return new TestObject(
                    document.containsKey("_id") ? document.get("_id").toString() : null,
                    document.getString("value"),
                    document.getString("etag")
            );
        }

        @Override
        protected void ensureIndexes() {
        }
    }

    public class TestObject {

        private final String id;
        private final String value;

        private final String eTag;

        protected TestObject(final String id, final String value, final String eTag) {
            this.eTag = eTag;
            this.id = id;
            this.value = value;
        }

        protected TestObject(final String id, final String value) {
            this(id, value, null);
        }

    }
}
