package mcmaster.ilos.datacollectionapp.UploadSavedData;

import java.io.Serializable;

/* Class used to represent an uploaded item from the Upload RecycleView */
public class Upload_Item_Model implements Serializable {

    private String title, subTitle;
    private String type;
    private Boolean uploaded;
    private Boolean uploading;
    private String filename;

    Upload_Item_Model(String title, String subTitle, Boolean uploaded, String type, String filename) {
        this.title = title;
        this.subTitle = subTitle;
        this.uploaded = uploaded;
        this.uploading = false;
        this.type = type;
        this.filename = filename;
    }

    public String getTitle() {
        return title;
    }

    String getSubTitle() {
        return subTitle;
    }

    Boolean getUploaded() {
        return uploaded;
    }

    void setUploaded(Boolean uploaded) {
        this.uploaded = uploaded;
    }

    void setUploading(Boolean uploading) {
        this.uploading = uploading;
    }

    Boolean getUploading() {
        return uploading;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getFilename() {
        return filename;
    }
}
