import org.junit.Test;
import sheets.RundeckJobSheet;
import sheets.SheetService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class GoogleSheetsTest {
    String spreadsheetId = "1K3kynNKShOGYzBFflKU5B4Sjtfjb9YeT5YVqOdw_qfU";

    private final SheetService sheetService = new SheetService(spreadsheetId);

    public GoogleSheetsTest() throws IOException {
    }

    @Test
    public void shouldListSheets() throws Exception {
        List<String> sheets = sheetService.listSheetTitles();
        assertEquals(Arrays.asList("01 - Get Input Data - on itpcmd-test", "02 - Extraction Pipeline (Dev version) - on itpcmd-staging", "02 - Extraction Pipeline (iteration30 Release) - on sm-staging"), sheets);
    }

    @Test
    public void shouldListRundeckJobs() throws Exception {
        List<RundeckJobSheet> rundeckJobSheets = sheetService.listRundeckJobSheets();
        assertEquals(Arrays.asList("a4bddb2a-1896-4a0f-a6ce-f4e06c94ad81", "9e77aa9b-caad-4c52-aad4-dee716b6e171", "bc0422af-2452-4ea6-8915-e638661cd1e5"), rundeckJobSheets.stream().map(rjs -> rjs.jobId).collect(Collectors.toList()));
    }

    @Test
    public void shouldListNamedRangesForSheet() throws Exception {
        List<String> sheets = sheetService.listSheetTitles();
        List<String> namedRanges = sheetService.listNamedRangesForSheet(sheets.get(0));

        List<Object> newRecord = Arrays.asList("http://sm-test-app-01.springer-sbm.com:4440/project/protocols/execution/show/767",
                "failed", null);

        System.out.println("sheets in "+spreadsheetId+"="+sheets);
        sheetService.write(namedRanges.get(0), newRecord);
    }
}
