import java.util.concurrent.ConcurrentHashMap;

/**
 * Class to hold all the data resident on one server
 */
public class DataService {
    // Map to hold all the data of bulletin board for the server
    private ConcurrentHashMap<Integer, Article> database;

    public DataService() {
        this.database = new ConcurrentHashMap<>();
    }

    public Article getArticle(int id) {
        return database.get(id);
    }

    public boolean putArticle(Article article) {
        if (database.containsKey(article.getId())) {
            return false;
        }
        database.put(article.getId(), article);
        return true;
    }

	public synchronized ConcurrentHashMap<Integer, Article> getDatabase() {
		return database;
	}
}
