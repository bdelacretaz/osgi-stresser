package ch.x42.osgi.stresser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class BundlesStartStopTask extends TaskBase {
    
    private int counter;
    
    /** The closest this is to 1, the more bundles we stop at every cycle */
    public static final double STOP_BUNDLE_FACTOR = 0.3;
    
    private final static long RANDOM_SEED = 42L;
    private final static long STOP_WAIT_MSEC = 5000L;
    private final static long START_WAIT_MSEC = 5000L;
    private final Set<String> ignoredBundlePatterns = new HashSet<String>();
    private final String mySymbolicName;
    public static final long REFRESH_TIMEOUT_MSEC = 10000L;
    private final PackagesRefresher refresher; 
    
    BundlesStartStopTask(BundleContext bundleContext) {
        super("bu", bundleContext);
        mySymbolicName = bundleContext.getBundle().getSymbolicName();
        refresher = new PackagesRefresher(bundleContext);
        
        log.info("Using random seed {}", RANDOM_SEED);
        
        ignoredBundlePatterns.add("org.osgi");
        ignoredBundlePatterns.add("org.apache.felix");
        ignoredBundlePatterns.add("commons");
        ignoredBundlePatterns.add("ch.x42");
        ignoredBundlePatterns.add("slf4j");
        ignoredBundlePatterns.add("log");
        log.info(
                "Won't touch bundles having one of the following patterns in their symbolic name: {}", 
                ignoredBundlePatterns);
    }
    
    protected void runOneCycle() {
        log.info("Running cycle {}", ++counter);
        
        // Randomly select a bunch of bundles to stop
        final List<Bundle> toStop = new ArrayList<Bundle>();
        for(Bundle b : bundleContext.getBundles()) {
            if(mySymbolicName.equals(b.getSymbolicName())) {
                continue;
            }
            if(ignoreBundle(b)) {
                continue;
            }
            if(b.getState() != Bundle.ACTIVE) {
                continue;
            }
            if(random.nextDouble() > STOP_BUNDLE_FACTOR) {
                continue;
            }
            toStop.add(b);
        }
        
        log.info("Stopping {} bundles (if they are active)", toStop.size());
        
        for(Bundle b : toStop) {
            try {
                if(b.getState() == Bundle.ACTIVE) {
                    b.stop();
                    refresher.refreshPackagesAndWait(REFRESH_TIMEOUT_MSEC);
                }
            } catch(Exception e) {
                log.error("Could not stop " + b, e);
            }
        }
        
        for(Bundle b : toStop) {
            if(waitForState(b, Bundle.ACTIVE, false, STOP_WAIT_MSEC)) {
                log.info("{} stopped", b);
            } else {
                log.warn("State is still {} for {}??", Bundle.ACTIVE, b);
            }
        }
        
        log.info("Restarting all {} stopped bundles", toStop.size());
        for(Bundle b : toStop) {
            try {
                b.start();
                refresher.refreshPackagesAndWait(REFRESH_TIMEOUT_MSEC);
            } catch(Exception e) {
                log.error("Could not start " + b, e);
            }
        }
        
        for(Bundle b : toStop) {
            if(waitForState(b, Bundle.ACTIVE, true, START_WAIT_MSEC)) {
                log.info("{} started", b);
            } else {
                log.warn("State is not {} for {}??", Bundle.ACTIVE, b);
            }
        }
        
        log.info("Cycle {} ends, successfully stopped/started {} bundles", counter, toStop.size());
    }
    
    private boolean waitForState(Bundle b, int expectedState, boolean expectEqual, long timeoutMsec) {
        final long end = System.currentTimeMillis() + timeoutMsec;
        while(System.currentTimeMillis() < end) {
            final boolean isEqual = b.getState() == expectedState; 
            if(expectEqual && isEqual) {
                return true;
            } else if(!expectEqual && !isEqual) {
                return true;
            }
        }
        return false;
    }
    
    private boolean ignoreBundle(Bundle b) {
        for(String pattern : ignoredBundlePatterns) {
            if(b.getSymbolicName().contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String getCurrentOptions() {
        return "ignored symbolic names (patterns)=" + ignoredBundlePatterns;
    }
}