package rundeck;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

public interface RundeckClient {
    @RequestLine("GET /api/19/jobs?project={project}")
    @Headers({"Content-Type: application/json", "Accept: application/json", "X-RunDeck-Auth-Token: RRZUbKthYy9OnLl90lJrKSLWO9uci8Rd"})
    List<RundeckJob> listJobByProject(@Param("project") String projectName);

    @RequestLine("GET /api/19/job/{jobId}/executions")
    @Headers({"Content-Type: application/json", "Accept: application/json", "X-RunDeck-Auth-Token: RRZUbKthYy9OnLl90lJrKSLWO9uci8Rd"})
    RundeckExecutionRoot listJobExecutions(@Param("jobId") String jobId);
}
