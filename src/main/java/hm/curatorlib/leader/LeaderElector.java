package hm.curatorlib.leader;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tr.com.argela.sebu.hm.curatorlib.core.LeaderJob;

public class LeaderElector extends LeaderSelectorListenerAdapter implements Closeable {

    private String              name;
    private LeaderSelector      leaderSelector;
    private static final Logger logger           = LogManager.getLogger();
    private boolean             amILeader        = false;
    private Lock                lock             = new ReentrantLock();
    private Condition           giveUpLeaderShip = lock.newCondition();
    private boolean             closed           = true;
    private LeaderJob           lj               = null;

    public LeaderElector(CuratorFramework client, String path, String name, LeaderJob lj) {
        this.name = name;
        this.lj = lj;
        this.leaderSelector = new LeaderSelector(client, path, this);
        this.leaderSelector.setId(name);
        this.leaderSelector.autoRequeue();
    }

    public boolean isClosed() {
        return closed;
    }

    public void startLeaderSelector() throws IOException {
        logger.info("leader selector will start!");
        this.leaderSelector.start();
        closed = false;
    }

    @Override
    public void close() {
        try {
            this.leaderSelector.close();
        } catch (Exception e) {

        }
        closed = true;
    }

    @Override
    public void takeLeadership(CuratorFramework client) {
        if (this.leaderSelector.hasLeadership()) {
            this.lock.lock();
            try {
                logger.info(" Taken leadership: {}", this.leaderSelector.getId());
                this.amILeader = true;
                boolean keepLeaderShip = this.lj.doLeaderJob();
                if (keepLeaderShip) {
                    try {
                        if(!Thread.currentThread().isInterrupted())
                            this.giveUpLeaderShip.await();
                    } catch (InterruptedException e) {

                    }
                }
            } finally {
                this.lock.unlock();
            }
        }
        this.amILeader = false;
        logger.warn(" I AM DONE WITH THE LEADERSHIP --- ID: {}", this.leaderSelector.getId());
    }

    public boolean amILeader() {
        return this.amILeader;
    }

    public void giveUpLeaderShip() {
        this.giveUpLeaderShip.signal();
    }
}
