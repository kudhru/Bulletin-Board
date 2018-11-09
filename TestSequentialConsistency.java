import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Test driver for Sequential Consistency
 */
public class TestSequentialConsistency {

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        List<ServerInfo> serverInfoList = Utils.getServerListFromProperties("IP.properties");
        String post_content, reply_content, post_response, reply_response;
        String successMsg = "All the replicas have commits/writes in the same order";
        int articleId;
        for(int i=0; i<5; i++) {
            // POST
            post_content = String.format("POST - %d", i);
            post_response = Utils.post(serverInfoList, post_content);
            articleId = Integer.parseInt(post_response.split(":")[1]);

            // REPLY
            reply_content = String.format("REPLY - %d", i);
            reply_response = Utils.reply(serverInfoList, articleId, reply_content);
        }

        List<List<Integer>> commitLogs = new ArrayList<>();
        for(int i=0; i<serverInfoList.size(); i++) {
            commitLogs.add(Utils.getCommitLogs(serverInfoList, i));
            Utils.assertEquals(commitLogs.get(0), commitLogs.get(i), successMsg);
        }
    }
}
