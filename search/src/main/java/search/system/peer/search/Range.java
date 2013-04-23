package search.system.peer.search;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 4/22/13
 * Time: 4:15 PM
 */
public class Range {
    private int left;
    private int right;

    public int getLeft() {
        return left;
    }

    public int getRight() {
        return right;
    }

    public Range(int left, int right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Range range = (Range) o;

        if (left != range.left) return false;
        if (right != range.right) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = left;
        result = 31 * result + right;
        return result;
    }
}
