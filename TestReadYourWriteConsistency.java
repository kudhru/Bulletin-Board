import java.io.IOException;
import java.util.*;

public class TestReadYourWriteConsistency {

    public static void main(String[] args) throws IOException {

        List<ServerInfo> serverInfoList = Utils.getServerListFromProperties("IP.properties");
        String post_content, reply_content, post_response, choose_response, reply_response;
        int articleId;
        String successMsg = "Read immediately after Write gives the last written value";
        for(int i=0; i<5; i++) {
            // POST
            post_content = String.format("POST - %d", i);
            post_response = Utils.post(serverInfoList, post_content);
            articleId = Integer.parseInt(post_response.split(":")[1]);

            // CHOOSE the articleId returned in the last POST.
            choose_response = Utils.choose(serverInfoList, articleId);
            Utils.assertEquals(post_content, choose_response.split(":")[2], successMsg);

            // REPLY
            reply_content = String.format("REPLY - %d", i);
            reply_response = Utils.reply(serverInfoList, articleId, reply_content);
            articleId = Integer.parseInt(reply_response.split(":")[1]);

            // CHOOSE the articleId returned in the last REPLY.
            choose_response = Utils.choose(serverInfoList, articleId);
            Utils.assertEquals(reply_content, choose_response.split(":")[2], successMsg);

        }
    }
}
