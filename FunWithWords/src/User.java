import java.util.Comparator;

public class User implements Comparable<User>{
    private String id;
    private StringBuffer sb;
    private int score;
    private int uniqueID;

    public User(String id, int uniqueID) {
        this.id = id;
        this.sb = new StringBuffer("");
        this.score = 0;
        this.uniqueID = uniqueID;
    }


    public void setSb(StringBuffer sb) {
        this.sb = sb;
    }

    public String getSb() {
        return sb.toString();
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void updateScore(int score) {
        this.score = this.score + score;
    }

    public void resetScore() {
        this.score = 0;
    }

    public int getScore() {
        return score;
    }

    public void setUniqueID(int uniqueID) {
        this.uniqueID = uniqueID;
    }

    public int getUniqueID() {
        return uniqueID;
    }

    @Override
    public int compareTo(User o) {
        return ((Integer) this.uniqueID).compareTo(o.getUniqueID());

    }

    public void replaceWord(String word) {
        this.sb.delete(0, 19);
        this.sb.insert(0, word);
    }





}
