package hm.curatorlib.core;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientConnector {

    private static final Logger logger              = LogManager.getLogger();
    private String              connectionString    = null;
    private String              clientName          = null;
    private String              groupName           = null;
    private String              leaderSelectionPath = null;

    RetryPolicy                 retryPolicy         = null;

    private CuratorFramework    client;

    public ClientConnector(String connectionString, String clientName, String groupName,
                           String leaderSelectionPath) {
        new ClientConnector(connectionString, clientName, groupName, leaderSelectionPath, 1000, 3);
    }
    /**
     * 
     * @param connectionString - hostname:port, hostname:port...  zk instances connection definitions
     * @param clientName - name of the requester
     * @param groupName - group that the requester belong to 
     * @param leaderSelectionPath - the znode for leader elections
     * @param sleepMsBetweenRetries - curator retry period in mill seconds
     * @param maxRetries - max retries for curator before making a judgment
     */
    public ClientConnector(String connectionString, String clientName, String groupName,
                           String leaderSelectionPath, int sleepMsBetweenRetries, int maxRetries) {
        this.connectionString = connectionString;
        this.clientName = clientName;
        this.groupName = groupName;
        this.leaderSelectionPath = leaderSelectionPath;
        this.retryPolicy = new RetryNTimes(maxRetries, sleepMsBetweenRetries);
        this.client = CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
    }

    public void initialize() {
        this.client.start();
        logger.info(" client started and waiting for conn {}", this.client.toString());
        try {
            this.client.blockUntilConnected();
        } catch (Exception e) {
            logger.error("Exception occured", e);
        }
        logger.info(" client started and got the conn");
    }

    public CuratorFramework getClient() {
        return client;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getClientName() {
        return clientName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getLeaderSelectionPath() {
        return leaderSelectionPath;
    }

    public void close() {
        this.client.close();
    }

}
