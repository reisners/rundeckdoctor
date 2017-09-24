import feign.Feign;
import feign.Logger;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.junit.Test;
import rundeck.RundeckClient;
import rundeck.RundeckExecutionRoot;
import rundeck.RundeckJob;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RundeckTest {

    @Test
    public void shouldListProtcolsJobs() {
        RundeckClient rundeckClient = Feign.builder()
                .client(new OkHttpClient())
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger(RundeckClient.class))
                .logLevel(Logger.Level.FULL)
                .target(RundeckClient.class, "http://sm-test-app-01.springer-sbm.com:4440");
        String rundeckApiToken = System.getenv("RDDOC_RUNDECK_API_TOKEN");
        List<RundeckJob> list = rundeckClient.listJobByProject(rundeckApiToken,"protocols");
        System.out.println(list);

        Map<String, RundeckJob> jobs = list.stream().collect(Collectors.toMap(job -> job.name, job -> job));

        RundeckJob inputJob = jobs.get("01 - Get Input Data - on itpcmd-test");

        RundeckExecutionRoot inputExecutions = rundeckClient.listJobExecutions(rundeckApiToken, inputJob.id);
        System.out.println(inputExecutions);

        inputExecutions.executions.forEach(ex -> {
            System.out.println(ex.id+" "+ex.status+" "+ex.href);
        });
    }
}
