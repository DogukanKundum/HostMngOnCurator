Host Management on Curator
--------------------------------

HostMngOnCurator is a library implementation in order to manage the application instances that are spread among multiple JVMs on multiple hosts/vms.

It is using curator lib (4.0.1) and zk (3.5.x) cluster(ensemble) underneath for low level process health state controls and leader (master) elections.

If an application has a valid client connection on zk cluster via curator framework this application is assumed to be health as a cluster member.

A typical usage can be found in SampleClientApp .

An application that is willing to make host management via this library should follow the steps in the sample code and it is copied to here either;

1. to be able to receive host state changes we need to implement PeerStateChanges in our application
2. additionally to get leadership trigger we need to implement LeaderJob in our application
3.  for both implementations there are also direct query interfaces in the related  Classes to get the state of a peer in the cluster and if the existing instance is leader or not in the cluster.
	a) public boolean getPeerState(String peerId, boolean askForCallBacks) in ClusterMembers class:
		The state of a peer in the cluster can be retrieved with this interface
     * @param peerId  - id of the peer state of which is queried
     * @param askForCallBacks - if there is need for also getting the ID list of UP and DOWN peers should be passed as true
     *                                                  in that case after evaluation of the cluster peers the related callback interfaces are called
     *                                                  simultaneously. The evaluation can not be done twice in the periodToCheck in seconds
	b) public boolean amILeader() in LeaderElector class: in order to check the leader state of the application
4. After instantiation of ClientConnector , LeaderElector and ClusterMembers they have to be initialized by the application whenever needed according to the business needs.
5. In the instantiation phases ClusterMembers need the implementation of PeerStateChanges and LeaderElector need the implementation of LeaderJob interfaces for callbacks
	a) call back for cluster management :
		*public void peersTurnedUp(List<String> ids);
		*public void peersTurnedDown(List<String> ids);
		periodically ClusterMembers class check the state of the peers that are in the cluster and give information to the application about the state changes via the call back interfaces above
	b) call back for leadership :
		*public boolean doLeaderJob();
		on event received that gives the leadershipment to the application by the LeaderElector class , this interface is called by it in order to inform the application about being leadership. If the application would like to do a business flow once it receives the leadership
		and release it it should return false at the end of the execution, otherwise true in order to keep the leadership in its hands. 
		
		
Running the Sampke App for testing 

first of all install a ZK ensemble 

a five node ZK ensemble with following configuration can ve installed on any host

under the directories called zookeeper-3.5.4_btm1, zookeeper-3.5.4_btm2 ,zookeeper-3.5.4_btm3 , zookeeper-3.5.4_tph1, zookeeper-3.5.4_tph2 configure each ZK node appropriately.
---------------------------------------
sample for zookeeper-3.5.4_btm1

../zookeeper-3.5.4_btm1/conf/zoo.cfg

# The number of milliseconds of each tick
tickTime=2000
# The number of ticks that the initial 
# synchronization phase can take
initLimit=20
# The number of ticks that can pass between 
# sending a request and getting an acknowledgement
syncLimit=5
# the directory where the snapshot is stored.
# do not use /tmp for storage, /tmp here is just 
# example sakes.
dataDir=C:/temp/curator/zookeeper-3.5.4_btm1/data
# the port at which the clients will connect
clientPort=2181
# the maximum number of client connections.
# increase this if you need to handle more clients
#maxClientCnxns=60
#
# Be sure to read the maintenance section of the 
# administrator guide before turning on autopurge.
#
# http://zookeeper.apache.org/doc/current/zookeeperAdmin.html#sc_maintenance
#
# The number of snapshots to retain in dataDir
#autopurge.snapRetainCount=3
# Purge task interval in hours
# Set to "0" to disable auto purge feature
#autopurge.purgeInterval=1
server.1=localhost:2888:3888
server.2=localhost:2889:3889
server.3=localhost:2890:3890
server.4=localhost:2891:3891
server.5=localhost:2892:3892

C:\temp\curator\zookeeper-3.5.4_btm1\data\myid

1

------------------------------------------------------------------------------------

once all zk nodes ar UP and ensemble is constructed successfully you may run your simulation app with the arguments in java command line as follows;

* run options
     *args[0]: localhost:2181,localhost:2182,localhost:2183,localhost:2184,localhost:2185 // Connection String to the zk cluster (sample is for a 5 instances cluster)
     *args[1]: btm1 // uniqueue name of the application instance in the application cluster
     *args[2]: btm //application cluster name
     *args[3]: /btm // application cluster node name
     *args[4]: /btmleader // application leadershipment node name
     *args[5]: 1000 // delays in milliseconds for retries in curator as integer
     *args[6]: 5  // number of retries in curator as integer
     *args[7]: btm1,btm2,btm3 // all application peer names that are subject to the application cluster
     
     in each run change the arg[1] param to the requested app instance name to simulate that specific instance in the cluster given args[7]
     
     you may do two app clusters' test on the same ZK ensemle simultaneously as follows
     
 * run options
     *args[0]: localhost:2181,localhost:2182,localhost:2183,localhost:2184,localhost:2185 // Connection String to the zk cluster (sample is for a 5 instances cluster)
     *args[1]: tph1 // uniqueue name of the application instance in the application cluster
     *args[2]: tph //application cluster name
     *args[3]: /tph // application cluster node name
     *args[4]: /tphleader // application leadershipment node name
     *args[5]: 1000 // delays in milliseconds for retries in curator as integer
     *args[6]: 5  // number of retries in curator as integer
     *args[7]: tph1,tph2 // all application peer names that are subject to the application cluster   
 
 so both tph cluster and btm cluster can run simultaneously and it can be tested that they can work independently on the same ZK ensemble( cluster )