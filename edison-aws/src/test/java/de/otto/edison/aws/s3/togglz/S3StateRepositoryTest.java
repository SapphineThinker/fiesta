package de.otto.edison.aws.s3.togglz;

import de.otto.edison.aws.s3.configuration.S3TogglzProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.util.HashSet;

import static de.otto.edison.aws.s3.S3TestHelper.createS3Client;
import static de.otto.edison.aws.s3.S3TestHelper.createTestContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

public class S3StateRepositoryTest {
    private static final int TEST_PORT_S3 = 4572;
    private final static String TEST_BUCKET = "test-togglz";

    @ClassRule
    public final static GenericContainer<?> localstackContainer = createTestContainer(TEST_PORT_S3);

    private S3StateRepository repository;
    private S3Client s3Client;
    private S3TogglzProperties togglzProperties;

    @Before
    public void setup() {
        final Integer mappedPort = localstackContainer.getMappedPort(TEST_PORT_S3);
        s3Client = createS3Client(mappedPort);

        final CreateBucketRequest createBucketRequest = CreateBucketRequest
                .builder()
                .bucket(TEST_BUCKET)
                .acl(BucketCannedACL.PUBLIC_READ_WRITE)
                .build();

        s3Client.createBucket(createBucketRequest);

        togglzProperties = new S3TogglzProperties();
        togglzProperties.setBucketName(TEST_BUCKET);
        repository = new S3StateRepository(togglzProperties, s3Client);
    }

    @After
    public void tearDown() {
        final String featureKey = String.format("%s%s", togglzProperties.getKeyPrefix(), TestFeature.FEATURE_1.name());
        final DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest
                .builder()
                .bucket(TEST_BUCKET)
                .key(featureKey)
                .build();

        s3Client.deleteObject(deleteObjectRequest);

        final DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest
                .builder()
                .bucket(TEST_BUCKET)
                .build();
        s3Client.deleteBucket(deleteBucketRequest);
    }

    @Test
    public void testGetSetFeatureState() {
        assertNull(repository.getFeatureState(TestFeature.FEATURE_1));

        final FeatureState initState = new FeatureState(TestFeature.FEATURE_1)
                .setEnabled(true)
                .setStrategyId("abc")
                .setParameter("key1", "value1");

        repository.setFeatureState(initState);

        final FeatureState actualState = repository.getFeatureState(TestFeature.FEATURE_1);

        assertThat(actualState.getFeature()).isEqualTo(initState.getFeature());
        assertThat(actualState.getStrategyId()).isEqualTo("abc");
        assertThat(actualState.isEnabled()).isEqualTo(true);
        assertThat(actualState.getParameter("key1")).isEqualTo("value1");
        assertThat(actualState.getParameterNames()).isEqualTo(new HashSet<String>() {
            {
                add("key1");
            }
        });
    }

    @Test
    public void testUpdateFeatureState() {
        final FeatureState initState = new FeatureState(TestFeature.FEATURE_1)
                .setEnabled(true)
                .setStrategyId("abc")
                .setParameter("key1", "value1");

        repository.setFeatureState(initState);

        FeatureState actualState = repository.getFeatureState(TestFeature.FEATURE_1);

        assertThat(actualState.getFeature()).isEqualTo(initState.getFeature());

        final FeatureState updatedState = new FeatureState(TestFeature.FEATURE_1)
                .setEnabled(false)
                .setStrategyId("def")
                .setParameter("key2", "value2");

        repository.setFeatureState(updatedState);

        actualState = repository.getFeatureState(TestFeature.FEATURE_1);

        assertThat(actualState.getFeature()).isEqualTo(initState.getFeature());
        assertThat(actualState.getStrategyId()).isEqualTo("def");
        assertThat(actualState.isEnabled()).isEqualTo(false);
        assertThat(actualState.getParameter("key2")).isEqualTo("value2");
        assertThat(actualState.getParameterNames()).isEqualTo(new HashSet<String>() {
            {
                add("key2");
            }
        });
    }

    private enum TestFeature implements Feature {
        FEATURE_1
    }

}
