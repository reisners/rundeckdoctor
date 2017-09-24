package rundeck;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

public interface RundeckClient {
    @RequestLine("GET /api/19/jobs?project={project}")
    @Headers({"Content-Type: application/json", "Accept: application/json", "X-RunDeck-Auth-Token: {token}"})
    List<RundeckJob> listJobByProject(@Param("token") String token, @Param("project") String projectName);

    @RequestLine("GET /api/19/job/{jobId}/executions")
    @Headers({"Content-Type: application/json", "Accept: application/json", "X-RunDeck-Auth-Token: {token}"})
    RundeckExecutionRoot listJobExecutions(@Param("token") String token, @Param("jobId") String jobId);
}
