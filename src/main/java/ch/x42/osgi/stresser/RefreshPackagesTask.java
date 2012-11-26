package ch.x42.osgi.stresser;

import java.io.PrintWriter;

import org.osgi.framework.BundleContext;

public class RefreshPackagesTask extends TaskBase{
    private long waitMsec = 5000;
    public static final long REFRESH_TIMEOUT_MSEC = 10000L;
    private final PackagesRefresher refresher; 
    
    RefreshPackagesTask(BundleContext bundleContext) {
        super("rp", bundleContext);
        refresher = new PackagesRefresher(bundleContext);
    }
    
    protected void runOneCycle() {
        refresher.refreshPackagesAndWait(REFRESH_TIMEOUT_MSEC);
    }
    
    protected void processCommand(String [] cmd, PrintWriter out) {
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
}