import java.io.IOException;
import java.util.*;

/**
 * Chat client which can be used to use the system in an interactive way
 */
public class ClientTerminal {

    public static void main(String[] args) throws IOException {

        List<ServerInfo> serverInfoList =
                Utils.getServerListFromProperties("IP.properties");

        Scanner scanner = new Scanner(System.in);
        String operation, content, response;
        int articleId;
        boolean read_further = true;
        while(read_further) {
            System.out.println("Enter the Operation to perform\n " +
                    "POST [P] , REPLY [R] , CHOOSE [C], READ [RE], EXIT [E] ");
            operation = scanner.nextLine();
            operation = operation.trim();
            switch (operation.toUpperCase()) {
                case "P" :
                    System.out.println("Enter the content to be posted:");
                    content = scanner.nextLine();
                    response = Utils.post(serverInfoList, content);
                    System.out.println("Client received the following response:\n"+ response + "\n");
                    break;
                case "R" :
                    System.out.println("Enter the article id for which you want to reply:");
                    while(true) {
                        try {
                            articleId = Integer.parseInt(scanner.nextLine());
                            break;
                        }
                        catch (Exception e) {
                            System.out.println("Wrong format for articleId. " +
                                    "Enter the article id for which you want to reply:");
                        }
                    }

                    System.out.println("Enter the content of your reply:");
                    content = scanner.nextLine();
                    response = Utils.reply(serverInfoList, articleId, content);
                    System.out.println("Client received the following response:\n"+ response + "\n");
                    break;
                case "C" :
                    System.out.println("Enter the article id which you want to choose:");
                    while(true) {
                        try {
                            articleId = Integer.parseInt(scanner.nextLine());
                            break;
                        }
                        catch (Exception e) {
                            System.out.println("Wrong format for articleId. " +
                                    "Enter the article id which you want to choose:");
                        }
                    }
                    response = Utils.choose(serverInfoList, articleId);
                    System.out.println("Client received the following response:\n"+
                            response.split(":")[2] + "\n");
                    break;
                case "RE" :
                    System.out.println("Sending the request for reading the entire database:");
                    response = Utils.read(serverInfoList);
                    System.out.println("Client received the following response:\n"+ response + "\n");
                    break;
                case "E" :
                    System.out.println("Exiting...");
                    read_further = false;
                    // send a read request
                    break;
                default:
                    System.out.println("Incorrect option");
                    break;
            }
        }

        System.exit(0);
        return;
    }
}
