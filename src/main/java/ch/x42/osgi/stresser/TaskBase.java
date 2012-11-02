package ch.x42.osgi.stresser;

import java.io.PrintWriter;
import java.util.Random;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TaskBase implements Runnable {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final Thread thread;
    
    enum STATE {
        stopped,
        oneShot,
        running,
        paused,
    }
    
    private STATE state;
    protected final Random random;
    protected final BundleContext bundleContext;
    protected final String taskName;
    
    TaskBase(String taskName, BundleContext ctx) {
        this.bundleContext = ctx;
        this.taskName = taskName;
        state = STATE.paused;
        thread = new Thread(this, this.toString());
        thread.start();
        random = new Random(42);
    }
    
    protected abstract void runOneCycle() throws Exception;
    
    protected long getMsecBetweenCycles() {
        // By default, wait between zero and one second
        return -1000;
    }
    
    @Override
    public String toString() {
        return taskName + " task";
    }
    
    void setState(STATE s) {
        state = s;
        synchronized (this) {
            notifyAll();
        }
    }
    
    STATE getState() {
        return state;
    }
    
    String getTaskName() {
        return taskName;
    }
    
    Thread getThread() {
        return thread;
    }
    
    protected void processCommand(String [] cmd, PrintWriter out) {
        final String verb = cmd.length >= 2 ? cmd[1] : "MISSING_VERB";
        if(verb.equals("r")) {
            setState(STATE.running);
        } else if(verb.equals("p")) {
            setState(STATE.paused);
        } else if(verb.equals("o")) {
            setState(STATE.oneShot);
        } else {
            out.println("Unknown command verb: " + verb);
        }
    }
    
    public final void run() {
        log.info("{}: execution thread starts", this);
        while(state != STATE.stopped) {
            if(state == STATE.paused) {
                synchronized (this) {
                    try {
                        log.info("{}: execution paused", this);
                        wait();
                    } catch(InterruptedException ignore) {
                    }
                }
            }

            if(state != STATE.running && state != STATE.oneShot) {
                continue;
            }
            
            try {
                log.error(this + ": running one cycle");
                runOneCycle();
            } catch(Exception e) {
                log.error(this + ": Exception in runOneCycle", e);
            }
            
            if(state == STATE.running) {
                final long toWait = getMsecBetweenCycles();
                log.info("Waiting {} msec before next cycle...", toWait);
                waitMsec(toWait);
            } else if(state == STATE.oneShot) {
                state = STATE.paused;
            }
        }
        log.info("{}: execution thread ends", this);
    }
    
    /** Wait a few msec. If msec is negative, random wait 
     *  up to its positive value.
     */
    protected void waitMsec(long msec) {
        if(msec < 0) {
            msec = (long)(random.nextFloat() * -msec);
        }
        
        try {
            Thread.sleep(msec);
        } catch(InterruptedException ignore) {
        }
    }

}
