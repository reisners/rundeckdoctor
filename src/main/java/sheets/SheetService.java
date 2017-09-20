package sheets;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class SheetService {

    private final Sheets service;

    private final String spreadsheetId;

    public SheetService(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
        this.service = getSheetsService();
    }

    /** Application name. */
    private static final String APPLICATION_NAME =
            "Google Sheets API Java Quickstart";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/sheets.googleapis.com-java-quickstart");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/sheets.googleapis.com-java-quickstart
     */
    private static final List<String> SCOPES =
            Arrays.asList(SheetsScopes.SPREADSHEETS);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
                SheetService.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    /**
     * Build and return an authorized Sheets API client service.
     * @return an authorized Sheets API client service
     */
    public static Sheets getSheetsService() {
        try {
            Credential credential = authorize();
            return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public List<String> listNamedRanges() throws IOException {
        Spreadsheet metadata = service.spreadsheets().get(spreadsheetId).execute();
        List<NamedRange> nrs = (List<NamedRange>) metadata.get("namedRanges");
        return nrs.stream().map(nr -> nr.getName()).collect(Collectors.toList());
    }

    public List<String> listNamedRangesForSheet(String sheetTitle) {
        try {
            Spreadsheet metadata = service.spreadsheets().get(spreadsheetId).execute();
            List<Sheet> sheets = (List<Sheet>) metadata.get("sheets");
            Optional<Integer> optSheetId = sheets.stream().filter(sh -> sh.getProperties().getTitle().equals(sheetTitle)).map(sh -> sh.getProperties().getSheetId()).findAny();
            List<NamedRange> nrs = (List<NamedRange>) metadata.get("namedRanges");
            return nrs.stream()
                    .filter(nr -> optSheetId.map(id -> id.equals(Optional.ofNullable(nr.getRange().getSheetId()).orElse(0))).orElse(false))
                    .map(nr -> nr.getName()).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> listSheetTitles() throws IOException {
        List<Sheet> sheets = listSheets();
        return sheets.stream().map(sheet -> sheet.getProperties().getTitle()).collect(Collectors.toList());
    }

    public List<Sheet> listSheets() throws IOException {
        Spreadsheet metadata = service.spreadsheets().get(spreadsheetId).execute();
        return (List<Sheet>) metadata.get("sheets");
    }

    public List<RundeckJobSheet> listRundeckJobSheets() throws IOException {
        return listSheets().stream()
                .map(sheet -> {
                    String sheetTitle = sheet.getProperties().getTitle();
                    if ("Rundeck job".equals(readCell(sheetTitle, "A1"))) {
                        String url = readCell(sheetTitle, "B1");
                        URI uri = URI.create(url);
                        RundeckJobSheet jobSheet = new RundeckJobSheet();
                        jobSheet.rundeckServer = url.replaceAll("(http://[^/]+)/.*$", "$1");
                        jobSheet.rundeckProject = url.replaceAll(".*/project/(.*)/job/.*", "$1");
                        jobSheet.jobId = url.replaceAll(".*/([^/]*)", "$1");
                        jobSheet.tableRange = listNamedRangesForSheet(sheetTitle).get(0);
                        jobSheet.sheetTitle = sheetTitle;
                        jobSheet.sheetId = sheet.getProperties().getSheetId();
                        // read all rows of jobSheet.tableRange
                        jobSheet.lastKnownExecutionId = 0;
                        List<List<Object>> tableRows = readTableRows(jobSheet);
                        jobSheet.tableHeaders = tableRows.get(0).stream().map(o -> (String)o).collect(Collectors.toList());
                        for (List<Object> row : tableRows.subList(1, tableRows.size())) {
                            jobSheet.lastKnownExecutionId = Math.max(jobSheet.lastKnownExecutionId, Integer.parseInt((String) row.get(0)));
                        }
                        return jobSheet;
                    } else {
                        return null;
                    }
                })
                .filter(js -> js != null)
                .collect(Collectors.toList());
    }

    private List<List<Object>> readTableRows(RundeckJobSheet jobSheet) {
        try {
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, jobSheet.tableRange)
                    .execute();
            return response.getValues();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(String range, List<Object> newRecord) {
        try {
            ValueRange addition = new ValueRange();
            addition.setValues(Arrays.asList(newRecord));
            service.spreadsheets().values().append(spreadsheetId, range, addition).setValueInputOption("RAW").execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readCell(String sheetTitle, String address) {
        try {
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, sheetTitle+"!"+address)
                    .execute();
            List<List<Object>> values = response.getValues();
            return (String) values.get(0).get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
