package donationmanager;

public enum FileFilter {
    EXCEL("Excel files", "xlsx", "xlsm", "xlsb", "xls"),

    PDF("PDF files", "pdf");

    public final String description;
    public final String[] extensions;

    FileFilter(String description, String... extensions) {
        this.description = description;
        this.extensions = extensions;
    }

}
