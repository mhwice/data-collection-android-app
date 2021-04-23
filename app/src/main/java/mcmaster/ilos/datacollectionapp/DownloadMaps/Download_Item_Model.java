package mcmaster.ilos.datacollectionapp.DownloadMaps;

import android.support.annotation.NonNull;

import java.io.Serializable;

/* Represents a downloaded item */
public class Download_Item_Model implements Serializable {

    private String title, subTitle;
    private Boolean downloaded;
    private Boolean downloading;

    public Download_Item_Model(String title, String subTitle, Boolean downloaded) {
        this.title = title;
        this.subTitle = subTitle;
        this.downloaded = downloaded;
        this.downloading = false;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    Boolean getDownloaded() {
        return downloaded;
    }

    void setDownloaded(Boolean downloaded) {
        this.downloaded = downloaded;
    }

    void setDownloading(Boolean downloading) {
        this.downloading = downloading;
    }

    Boolean getDownloading() {
        return downloading;
    }

    @Override
    @NonNull
    public String toString() {
        return "Title: " + title + ", Subtitle: " + subTitle + ", Downloaded: " + downloaded + ", Downloading: " + downloading;
    }
}
