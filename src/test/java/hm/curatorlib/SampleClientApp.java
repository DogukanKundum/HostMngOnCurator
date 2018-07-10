package tr.com.argela.sebu.hm.curatorlib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.curator.test.TestingCluster;
import org.apache.curator.test.TestingServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tr.com.argela.sebu.hm.curatorlib.cluster.ClusterMembers;
import tr.com.argela.sebu.hm.curatorlib.core.ClientConnector;
import tr.com.argela.sebu.hm.curatorlib.core.LeaderJob;
import tr.com.argela.sebu.hm.curatorlib.core.PeerStateChanges;
import tr.com.argela.sebu.hm.curatorlib.leader.LeaderElector;

/**
 * to be able to receive host state changes we need to implement PeerStateChanges
 * additionally to get leadership trigger we need to implement LeaderJob
 *          for both implementations there are also direct query interfaces in the related 
 *          Classes to get the state of a peer in the cluster and if the existing instance is
 *          leader or not in the cluster
 * @author gokhanka
 *
 */
public class SampleClientApp implements PeerStateChanges, LeaderJob {

    private static final Logger logger = LogManager.getLogger();
    private LeaderElector       le     = null;
    private ClusterMembers      cm     = null;
    private ClientConnector     cn     = null;

    public SampleClientApp(String connectionString, String clientName, String groupName,
                           String groupPath, String leaderSelectionPath, int sleepMsBetweenRetries,
                           int maxRetries, List<String> clusterMembers) {
        // first of all we have to create a client to connect to ZK cluster via curator
        this.cn = new ClientConnector(connectionString,
                                      clientName,
                                      groupName,
                                      leaderSelectionPath,
                                      sleepMsBetweenRetries,
                                      maxRetries);
        // one we have create the connection we should start it to activate the communication
        this.cn.initialize();
        // for application cluster management we need to instantiate ClusterMembers class with the client (this.cn)
        this.cm = new ClusterMembers(this.cn.getClient(),
                                     groupPath,
                                     clientName,
                                     clusterMembers,
                                     this,
                                     1, // initial delay for the first check
                                     1); // period for the upcoming checks
        try {
            // when ready to get the states of the peers in the cluster we need to start it
            this.cm.startCluster();
        } catch (IOException e) {
            logger.error("Exception on cluster init", e);
        }
        // for the leader election in the cluster we need the instantiate LeaderElector with the client (this.cn)
        this.le = new LeaderElector(this.cn.getClient(), leaderSelectionPath, clientName, this);
        try {
            // when ready for leader operations in the application start leader election
            this.le.startLeaderSelector();
        } catch (Exception e) {
            logger.error("Exception on leader init", e);
        }

    }

    @Override
    public void peersTurnedUp(List<String> ids) {
        for (String id : ids) {
            logger.info("{} turned Up", id);
        }
    }

    @Override
    public void peersTurnedDown(List<String> ids) {
        for (String id : ids) {
            logger.info("{} Turned Down", id);
        }
    }

    @Override
    public boolean doLeaderJob() {
        boolean keepLeaderShip = true;
        logger.info("{} I AM LEADER .. state: {} ... will I keep the leadership: {}",
                    this.le.amILeader(),
                    keepLeaderShip);
        return keepLeaderShip;
    }

    public LeaderElector getLe() {
        return le;
    }

    public ClusterMembers getCm() {
        return cm;
    }

    public ClientConnector getCn() {
        return cn;
    }

    /**
     * run options
     * 0: localhost:2181,localhost:2182,localhost:2183,localhost:2184,localhost:2185 // Connection String to the zk cluster (sample is for a 5 instances cluster)
     * 1: btm1 // uniqueue name of the application instance in the application cluster
     * 2: btm //application cluster name
     * 3: /btm // application cluster node name
     * 4: /btmleader // application leadershipment node name
     * 5: 1000 // delays in milliseconds for retries in curator as integer
     * 6: 5  // number of retries in curator as integer
     * 7: btm1,btm2,btm3 // all application peer names that are subject to the application cluster
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            //TestingServer server = new TestingServer(7777,true);
            TestingCluster server = new TestingCluster(5);
            // server.start();
            String clusterMembers = args[7];
            String delims = "[,]";
            String[] tokens = clusterMembers.split(delims);
            List<String> members = new ArrayList<>(Arrays.asList(tokens));

            SampleClientApp sca = new SampleClientApp(args[0],
                                                      args[1],
                                                      args[2],
                                                      args[3],
                                                      args[4],
                                                      Integer.parseInt(args[5]),
                                                      Integer.parseInt(args[6]),
                                                      members);
            while (true) {
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
