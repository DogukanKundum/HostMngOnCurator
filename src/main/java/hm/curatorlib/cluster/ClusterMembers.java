package hm.curatorlib.cluster;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.nodes.GroupMember;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tr.com.argela.sebu.hm.curatorlib.core.PeerStateChanges;

public class ClusterMembers implements Closeable {

    private static final Logger      logger              = LogManager.getLogger();
    private String                   name;
    private HashMap<String, Boolean> memberStates        = null;
    private GroupMember              member;
    private ScheduledExecutorService scheduledPool;
    private PeerStateChanges         psg                 = null;
    private int                      initialDelayTocheck = 1;
    private int                      periodToCheck       = 1;
    private long                     lastCheckTime       = System.currentTimeMillis();
    private Lock                     lock                = new ReentrantLock();
    /**
     * 
     * @param client - previously created curator client for zk communication
     * @param path - znode path for cluster(group) management by curator
     * @param name - unique name of the application instance that would like to enter the cluster (group)
     * @param clusterMembers - list of peers in the cluster including the requestor's unique name
     * @param psg - implementation class of PeerStateChanges interface
     * @param initailDelay - for cluster queries - delays in milliseconds between start up and the first check
     * @param period - period for the periodic cluster queries
     */
    public ClusterMembers(CuratorFramework client, String path, String name,
                          List<String> clusterMembers, PeerStateChanges psg, int initailDelay,
                          int period) {
        this.name = name;
        logger.info(" group memeber will be created client: {} path: {} name: {}",
                    client.getNamespace(),
                    path,
                    name);
        try {
            this.member = new GroupMember(client, path, name, new String("Hi").getBytes());
            this.memberStates = new HashMap<String, Boolean>();
            this.psg = psg;
            if (initailDelay > 1)
                this.initialDelayTocheck = initailDelay;
            if (period > 1)
                this.periodToCheck = period;
            for (String otherMember : clusterMembers) {
                this.memberStates.put(otherMember, Boolean.valueOf(PeerStateChanges.DOWN));
            }
        } catch (Exception e) {
            logger.error("Exception occured", e);
        }
    }

    private void manageStates(Map<String, byte[]> actualMap, boolean callCallBack) {
        try {
            ArrayList<String> peersTurnedUp = null;
            ArrayList<String> peersTurnedDown = null;
            Set<String> clusterSet = this.memberStates.keySet();
            for (String member : clusterSet) {
                if (memberStates.get(member).booleanValue() == PeerStateChanges.DOWN
                        && actualMap.containsKey(member)) {
                    if (peersTurnedUp == null)
                        peersTurnedUp = new ArrayList<>();
                    peersTurnedUp.add(member);
                } else if (!(actualMap.containsKey(member))
                        && memberStates.get(member).booleanValue() == PeerStateChanges.UP) {
                    if (peersTurnedDown == null)
                        peersTurnedDown = new ArrayList<>();
                    peersTurnedDown.add(member);
                }
            }
            if (peersTurnedDown != null) {
                for (String peer : peersTurnedDown) {
                    memberStates.put(peer, Boolean.valueOf(PeerStateChanges.DOWN));
                }
                if (callCallBack)
                    psg.peersTurnedDown(peersTurnedDown);
            }
            if (peersTurnedUp != null) {
                for (String peer : peersTurnedUp) {
                    memberStates.put(peer, Boolean.valueOf(PeerStateChanges.UP));
                }
                if (callCallBack)
                    psg.peersTurnedUp(peersTurnedUp);
            }
        } catch (Exception e) {

        }
    }

    private void updateClusterMemberStates(boolean callCallBack) {
        try {
            this.lock.lock();
            Map<String, byte[]> actualMap = this.member.getCurrentMembers();
            if (actualMap != null && !actualMap.isEmpty()) {
                manageStates(actualMap, callCallBack);
            }
        } catch (Exception e) {

        } finally {
            this.lastCheckTime = System.currentTimeMillis();
            this.lock.unlock();
        }
    }
    /**
     * to start the cluster management actively requester should call the method when it is ready
     * @throws IOException
     */
    public void startCluster() throws IOException {
        logger.info("group management will start!");
        this.member.start();
        Runnable runnabledelayedTask = new Runnable() {

            @Override
            public void run() {
                updateClusterMemberStates(true);
            }
        };
        scheduledPool = Executors.newScheduledThreadPool(1);
        scheduledPool.scheduleWithFixedDelay(runnabledelayedTask,
                                             this.initialDelayTocheck,
                                             this.periodToCheck,
                                             TimeUnit.SECONDS);

    }
    /**
     * The state of a peer in the cluster can be retrieved with this interface
     * @param peerId  - id of the peer state of which is queried
     * @param askForCallBacks - if there is need for also getting the ID list of UP and DOWN peers should be passed as true
     *                                                  in that case after evaluation of the cluster peers the related callback interfaces are called
     *                                                  simultaneously. The evaluation can not be done twice in the periodToCheck in seconds
     * @return
     * @throws IllegalArgumentException
     */
    public boolean getPeerState(String peerId, boolean askForCallBacks)
            throws IllegalArgumentException {
        if ((System.currentTimeMillis() - this.lastCheckTime) > this.periodToCheck) {
            this.updateClusterMemberStates(askForCallBacks);
        }
        Boolean resultObject = this.memberStates.get(peerId);
        boolean result = false;
        if (resultObject != null) {
            result = resultObject.booleanValue();
        } else {
            throw new IllegalArgumentException(peerId + " not found in cluster !");
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        try {
            this.member.close();
        } catch (Exception e) {

        }
    }

}
