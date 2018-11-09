import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test driver for Quorum Consistency
 */
public class TestQuorumConsistency {

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        List<ServerInfo> serverInfoList = Utils.getServerListFromProperties("IP.properties");
        String post_content, reply_content, post_response, reply_response;
        String successMsg = "At least %d replicas have all the commits/writes.";
        int articleId;
        for(int i=0; i<4; i++) {
            // POST
            post_content = String.format("POST - %d", i);
            post_response = Utils.post(serverInfoList, post_content);
            articleId = Integer.parseInt(post_response.split(":")[1]);

            // REPLY
            reply_content = String.format("REPLY - %d", i);
            reply_response = Utils.reply(serverInfoList, articleId, reply_content);
        }

        List<ConcurrentHashMap<Integer, Article>> databases = new ArrayList<>();
        int maxServerId = -1, maxArticleId=-100;
        ConcurrentHashMap<Integer, Article> database;
        for(int i=0; i<serverInfoList.size(); i++) {
            database = Utils.getDatabase(serverInfoList, i);
            databases.add(database);
            if(maxArticleId < Collections.max((database.keySet()))) {
                maxArticleId = Collections.max((database.keySet()));
                maxServerId = i;
            }
        }

        int equalCount = 0;
        for(int i=0; i<serverInfoList.size(); i++) {
            if(databases.get(i).keySet().equals(databases.get(maxServerId).keySet()))
                equalCount++;
        }

        // Do sanity testing of data
        if(equalCount >= (serverInfoList.size()/2) + 1)
            System.out.println("*******Quorum Consistency satisfied*******");
        else
            System.out.println("*******Quorum Consistency violated*******");
    }
}
