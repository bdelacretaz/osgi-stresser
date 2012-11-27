package ch.x42.osgi.stresser;

import java.io.PrintWriter;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class StopSingleBundleTask extends TaskBase {
    private int counter;
    private long waitMsec;
    private static final String SYMBOLIC_NAME = "org.apache.sling.junit.core";
    
    StopSingleBundleTask(BundleContext bundleContext) {
        super("ss", bundleContext);
    }
    
    protected void runOneCycle() throws Exception {
        log.info("Running cycle {}", ++counter);
        
        // Stop and restart our bundle
        Bundle t = null;
        for(Bundle b : bundleContext.getBundles()) {
            if(SYMBOLIC_NAME.equals(b.getSymbolicName())) {
                t = b;
                break;
            }
        }
        
        if(t == null) {
            log.info("Bundle {} not found", SYMBOLIC_NAME);
        } else {
            if(t.getState() == Bundle.ACTIVE) {
                log.info("Stopping bundle {}", SYMBOLIC_NAME);
                t.stop();
            }
            log.info("Starting bundle {}", SYMBOLIC_NAME);
            t.start();
        }
    }

    @Override
    protected void processCommand(String[] cmd, PrintWriter out) {
        super.processCommand(cmd, out);
        if(cmd.length > 2) {
            final String val = cmd[2];
            try {
                waitMsec = Long.parseLong(val);
            } catch(NumberFormatException nfe) {
                out.println("Invalid waitMsec value '" + val + "', should be a Long");
            }
            log.info("waitMsec set to {}", waitMsec);
        }
    }

    protected long getMsecBetweenCycles() {
            return waitMsec;
    }
    
    @Override
    public String getCurrentOptions() {
        return "bundle to stop and restart=" + SYMBOLIC_NAME;
    }
}