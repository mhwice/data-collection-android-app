package mcmaster.ilos.datacollectionapp.CustomDataTypes;

/* Represents some metadata about maps in a table */
public class MapDownloadReturnType {

    private Boolean success;
    private String rowNumber;

    public MapDownloadReturnType(Boolean success, String rowNumber) {
        this.success = success;
        this.rowNumber = rowNumber;
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(String rowNumber) {
        this.rowNumber = rowNumber;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}