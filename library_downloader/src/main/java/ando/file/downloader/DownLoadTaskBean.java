package ando.file.downloader;

/**
 * # DownLoadTaskBean
 *
 * @author javakam
 * @date 2020/1/16  16:10
 */
public class DownLoadTaskBean {

    private String tname;
    private String url;
    private String parentFile;

    public DownLoadTaskBean(String tname, String url) {
        this.tname = tname;
        this.url = url;
    }

    public DownLoadTaskBean(String tname, String url, String parentFile) {
        this.tname = tname;
        this.url = url;
        this.parentFile = parentFile;
    }

    public String getTname() {
        return tname;
    }

    public void setTname(String tname) {
        this.tname = tname;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getParentFile() {
        return parentFile;
    }

    public void setParentFile(String parentFile) {
        this.parentFile = parentFile;
    }
}