package search.system.peer.search;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 4/22/13
 * Time: 7:17 PM
 */
public class BasicTorrentData implements TorrentData {
    private Integer id;
    private String title;
    private String magnet;

    public BasicTorrentData(Integer id, String title, String magnet) {
        this.id = id;
        this.title = title;
        this.magnet = magnet;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMagnet() {
        return magnet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BasicTorrentData that = (BasicTorrentData) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
