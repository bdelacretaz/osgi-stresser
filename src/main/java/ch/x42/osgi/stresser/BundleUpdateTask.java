package ch.x42.osgi.stresser;

import java.io.InputStream;
import java.io.PrintWriter;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class BundleUpdateTask extends TaskBase {
    private int counter;
    private long waitMsec;
    private static final String BUNDLE_PATH = "/update-bundles/org.apache.sling.junit.core.jar";
    private static final String SYMBOLIC_NAME = "org.apache.sling.junit.core";
    private static final String LOCATION = "BundleUpdateTask://" + SYMBOLIC_NAME;
    
    BundleUpdateTask(BundleContext bundleContext) {
        super("up", bundleContext);
    }
    
    protected void runOneCycle() throws Exception {
        log.info("Running cycle {}", ++counter);
        
        // If our bundle is already installed, update it
        Bundle t = null;
        for(Bundle b : bundleContext.getBundles()) {
            if(SYMBOLIC_NAME.equals(b.getSymbolicName())) {
                t = b;
                break;
            }
        }
        
        final InputStream s = getClass().getResourceAsStream(BUNDLE_PATH);
        try {
            if(t == null) {
                t = bundleContext.installBundle(LOCATION, s);
                t.start();
                log.info("{} installed, state={}", SYMBOLIC_NAME, t.getState());
            } else {
                t.update(s);
                if(t.getState() != Bundle.ACTIVE) {
                    t.start();
                }
                log.info("{} updated, state={}", SYMBOLIC_NAME, t.getState());
            }
        } finally {
            s.close();
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
        return "bundle to update=" + SYMBOLIC_NAME;
    }
}