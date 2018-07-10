package tr.com.argela.sebu.hm.curatorlib.core;

import java.util.List;

public interface PeerStateChanges {

    public static boolean UP   = true;
    public static boolean DOWN = false;

    /**
     * The IDs of the peers that went up are given back to application with this interface
     *   
     *   Implementer should catch all possible Exception within the implementation
     * @return
     */
    public void peersTurnedUp(List<String> ids);

    /**
     * The IDs of the peers that went down are given back to application with this interface
     *   
     *   Implementer should catch all possible Exception within the implementation
     * @return
     */
    public void peersTurnedDown(List<String> ids);

}
