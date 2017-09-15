package rundeck;

import com.google.gson.annotations.SerializedName;

import java.net.URL;
import java.util.List;

public class RundeckJobExecution {
    public int id;
    public URL href;
    public String status;
    public String project;
    public String user;
    @SerializedName("date-started")
    public RundeckDate dateStarted;
    @SerializedName("date-ended")
    public RundeckDate dateEnded;
    public RundeckJob job;
    public String description;
    public String argstring;
    public List<String> successfulNodes;
    public int averageDuration;
    public URL permalink;
}
