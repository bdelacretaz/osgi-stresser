package ch.x42.osgi.stresser;

import java.io.PrintWriter;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

public class RefreshPackagesTask extends TaskBase implements FrameworkListener {
    private int counter;
    private int refreshCount;
    private long waitMsec = 5000;
    
    RefreshPackagesTask(BundleContext bundleContext) {
        super("rp", bundleContext);
        bundleContext.addFrameworkListener(this);
    }
    
    protected void runOneCycle() {
        PackageAdmin packageAdmin = null;
        final ServiceReference ref = bundleContext.getServiceReference(PackageAdmin.class.getName());
        if(ref == null) {
            log.warn("StartLevel service not found");
            return;
        } else {
            packageAdmin = (PackageAdmin)bundleContext.getService(ref);
        }
        
        log.info("Running cycle {}, refreshing packages", ++counter);
        final int oldRefreshCount = refreshCount;
        packageAdmin.refreshPackages(null);
        
        final long end = System.currentTimeMillis() + waitMsec;
        while(refreshCount == oldRefreshCount && System.currentTimeMillis() < end) {
            waitMsec(100);
        }
        
        if(refreshCount > oldRefreshCount) {
            log.info("Successfully refreshed packages");
        } else {
            log.warn("No refresh packages event seen after {} msec", waitMsec);
        }
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

    @Override
    public void frameworkEvent(FrameworkEvent event) {
        if(event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
            refreshCount++;
        }
    }
    
    protected long getMsecBetweenCycles() {
        return waitMsec;
    }
}