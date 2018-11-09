import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 *  Class to hold article which corresponds to a post or reply
 */
public class Article implements Serializable{
	private static final long serialVersionUID = -1916546900328366290L;
	private int id;         // ID of article
	private int parent_id;  // ID of parent of article : -1 in case of post else "> 1"
    private String content;
    Set<Integer> replies;   // list of replies to this article

    public int getId() {
        return id;
    }
    
    public int getParentId() {
        return parent_id;
    }

    public String getContent() {
        return content;
    }

    public Set<Integer> getReplies() {
        return replies;
    }
    
    public void addReply(int id) {
        replies.add(id);
    }

    public Article(int id, int parent_id, String content) {
        this.id = id;
        this.parent_id = parent_id;
        this.content = content;
        replies = new HashSet<>();
    }

    public String toString() {
        return String.format("ID:%d PAR_ID:%d %s REPLIES:%s" ,
                this.id, this.parent_id, this.content, this.replies.toString());
    }
}