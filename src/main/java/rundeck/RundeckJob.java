package rundeck;

import java.net.URL;
import java.util.Map;

public class RundeckJob {
    public String id;
    public boolean scheduled;
    public URL href;
    public boolean scheduleEnabled;
    public boolean enabled;
    public URL permalink;
    public String name;
    public String group;
    public String description;
    public String project;
    public int averageDuration;
    public Map<String, String> options;

}
