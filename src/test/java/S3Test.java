import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Test;

import java.util.List;

public class S3Test {

    @Test
    public void test() {
        String s3bucket = "s3-service-broker-dev-e5772d92-0d95-485d-aa17-151e230a851a";
        String s3prefix = "normalized-protocol-full/";

        BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAIF2NZIU4SZBBSPHQ", "Ogj1rG/3Ww5kGtUPg9xvtdovtlWDz65HVVlIMKmb");
        AmazonS3 s3client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();

        System.out.println("Listing objects");

        ListObjectsV2Request x = new ListObjectsV2Request().withBucketName(s3bucket).withPrefix(s3prefix).withDelimiter("/");
        ListObjectsV2Result listing = s3client.listObjectsV2(x);

        for ( String prefix : listing.getCommonPrefixes() ) {
            System.out.printf("Object with key '%s'\n", prefix );
        }

    }


}
