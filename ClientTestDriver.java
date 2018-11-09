import java.util.*;

/**
 * Stress test driver to test
 */
public class ClientTestDriver {

    public static void main(String[] args) {
	    long start;
	    long end;

        // Read from file for servers  #########################################
        List<ServerInfo> serverInfoList =
                Utils.getServerListFromProperties("IP.properties");
        // #####################################################################
        System.out.println("Number of replicas = " + serverInfoList.size());

        int numClients = 4;
        int numWrites = 3;
        int numReads = 3;
        // Writing to replicas   ###############################################
	    List<TestWrite> writeIdList = new ArrayList<>();
        start = System.currentTimeMillis();
        for (int i = 0  ; i < numClients ; i++) {
            TestWrite testWrite = new TestWrite(numWrites , serverInfoList);
            testWrite.start();
            writeIdList.add(testWrite);
        }
        // Waiting for all threads to end and checking max id wrote
        int max_id = -1;
        for (TestWrite thread : writeIdList) {
            try {
                thread.join();
                max_id = Math.max(max_id , thread.getMaxArticleId());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
	    end = System.currentTimeMillis();
        System.out.println("Writing takes " + (end - start) + " msec");
        // #####################################################################

        System.out.println("All writes Completed and the max id = " + max_id);

        // Choosing from replicas  ###############################################
        List<TestRead> readIdList = new ArrayList<>();
        start = System.currentTimeMillis();
        for (int i = 0  ; i < numClients ; i++) {
            TestRead testRead = new TestRead(numReads , serverInfoList, max_id);
            testRead.start();
            readIdList.add(testRead);
        }
        // Waiting for all threads to end
        for (TestRead thread : readIdList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Choose takes " + (end - start) + " msec");
        // ######################################################################

        // Reading from replicas  ###############################################
        List<TestReadFull> readIdListF = new ArrayList<>();
        start = System.currentTimeMillis();
        for (int i = 0  ; i < numClients ; i++) {
            TestReadFull testRead = new TestReadFull(numReads , serverInfoList, 10000);
            testRead.start();
            readIdListF.add(testRead);
        }
        // Waiting for all threads to end
        for (TestReadFull thread : readIdListF) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Read takes " + (end - start) + " msec");
//        // ######################################################################
    }
}
