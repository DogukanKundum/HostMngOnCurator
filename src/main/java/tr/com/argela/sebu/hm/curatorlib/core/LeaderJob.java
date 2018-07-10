package tr.com.argela.sebu.hm.curatorlib.core;


public interface LeaderJob {
    /**
     * implementer should care about the return of the method;
     * 
     *  if it is true this means that the leadership will be kept for this instance and will not
     *  be given back to an other member unless there is a connection failure with curator 
     *  client management
     *  
     *  if it is false then the leadership will be released and curator will assign a new leader
     *   immediately
     *   
     *   Implementer should catch all possible Exception within the implementation
     * @return
     */
    public boolean doLeaderJob();
}
