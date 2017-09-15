public class RundeckDoctor {

    String spreadsheetId = "1K3kynNKShOGYzBFflKU5B4Sjtfjb9YeT5YVqOdw_qfU";
    String range = "RunTable.Input";

    public static void main(String[] args) {
        try {
            RundeckDoctor rd = new RundeckDoctor(args);
            rd.execute();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void execute() {
    }
}
