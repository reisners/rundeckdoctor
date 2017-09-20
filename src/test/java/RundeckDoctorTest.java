import org.junit.Test;

import java.io.IOException;

public class RundeckDoctorTest {
    private final RundeckDoctor rd = new RundeckDoctor(new String[] {});

    @Test
    public void test() throws IOException {
        rd.execute();
    }
}
