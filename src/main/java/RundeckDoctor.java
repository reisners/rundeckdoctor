import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.util.IOUtils;
import feign.Feign;
import feign.Logger;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import rundeck.RundeckClient;
import rundeck.RundeckJobExecution;
import rundeck.S3Delivery;
import sheets.RundeckJobSheet;
import sheets.SheetService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class RundeckDoctor {
    private static final SimpleDateFormat SHEET_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final SimpleDateFormat S3_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm");

    static {
        S3_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final SheetService sheetService;
    private final String rundeckApiToken;

    public RundeckDoctor(String[] args) {
        String spreadsheetId = System.getenv("RDDOC_GOOGLE_SHEET_ID");
        rundeckApiToken = System.getenv("RDDOC_RUNDECK_API_TOKEN");
        sheetService = new SheetService(spreadsheetId);

    }

    public static void main(String[] args) {
        try {
            RundeckDoctor rd = new RundeckDoctor(args);
            rd.execute();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void execute() throws IOException {
        for (RundeckJobSheet jobSheet : sheetService.listRundeckJobSheets()) {
            updateJobSheet(jobSheet);
        }
    }

    private void updateJobSheet(RundeckJobSheet jobSheet) {
        System.out.println("updating job sheet "+jobSheet.sheetTitle);
        RundeckClient rundeckClient = Feign.builder()
                .client(new OkHttpClient())
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger(RundeckClient.class))
                .logLevel(Logger.Level.FULL)
                .target(RundeckClient.class, jobSheet.rundeckServer);
        rundeckClient.listJobExecutions(rundeckApiToken, jobSheet.jobId).executions.stream()
                .filter(execution -> execution.id > jobSheet.lastKnownExecutionId)
                .sorted()
                .forEach(execution -> {
                    System.out.println("found new execution "+execution.id);

                    // map each header to the respective value
                    List<Object> row = jobSheet.tableHeaders.stream()
                            .map(header -> {
                                switch (header) {
                                    case "Execution":
                                        return execution.id;
                                    case "Link":
                                        return execution.permalink.toString();
                                    case "Status":
                                        return execution.status;
                                    case "Started":
                                        return SHEET_DATE_FORMAT.format(execution.dateStarted.date);
                                    case "Ended":
                                        return Optional.ofNullable(execution.dateEnded).map(dateEnded -> SHEET_DATE_FORMAT.format(dateEnded.date)).orElse("");
                                    case "Delivery":
                                        // find out about the execution's delivery
                                        return getDelivery(execution).map(delivery -> String.format("%s\ndoi_count=%s%1000.1000s", delivery.name, delivery.doi_count, delivery.error_log)).orElse("no delivery found");
                                    case "Timestamp":
                                        return SHEET_DATE_FORMAT.format(new Date());
                                    default:
                                        return "";
                                }
                            })
                            .collect(Collectors.toList());
                    sheetService.write(jobSheet.tableRange, row);
                });

    }

    private Optional<S3Delivery> getDelivery(RundeckJobExecution execution) {
        try {
            String s3prefix = execution.job.options.get("S3_bucket");

            if (s3prefix == null || execution.dateStarted == null || execution.dateEnded == null) {
                return Optional.empty();
            }

            String s3started = S3_DATE_FORMAT.format(execution.dateStarted.date);
            String s3ended = S3_DATE_FORMAT.format(execution.dateEnded.date);

            String s3bucket = System.getenv("RDDOC_AWS_S3_BUCKET");

            BasicAWSCredentials awsCreds = new BasicAWSCredentials(System.getenv("RDDOC_AWS_ACCESS_KEY"), System.getenv("RDDOC_AWS_SECRET_KEY"));
            AmazonS3 s3client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .build();

            System.out.println("Listing objects");

            for (String prefix : s3client.listObjectsV2(new ListObjectsV2Request().withBucketName(s3bucket).withPrefix(s3prefix).withDelimiter("/")).getCommonPrefixes()) {
                String name = prefix.substring(s3prefix.length());
                if (s3started.compareTo(name) <= 0 && s3ended.compareTo(name) >= 0) {
                    S3Delivery delivery = new S3Delivery();
                    delivery.doi_count = IOUtils.toString(s3client.getObject(s3bucket, prefix + "doi_count.txt").getObjectContent());
                    delivery.error_log = IOUtils.toString(s3client.getObject(s3bucket, prefix + "log/error.log").getObjectContent());
                    delivery.name = name;
                    return Optional.of(delivery);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
