package ch.x42.osgi.stresser;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

public class PackagesRefresher implements FrameworkListener {
    private volatile int refreshCount;
    private final BundleContext bundleContext;
    private final Waiter waiter = new Waiter();
    
    PackagesRefresher(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.bundleContext.addFrameworkListener(this);
    }
    
    void cleanup() {
        this.bundleContext.removeFrameworkListener(this);
    }
    
    boolean refreshPackagesAndWait(long maxMsecToWait) {
        PackageAdmin packageAdmin = null;
        final ServiceReference ref = bundleContext.getServiceReference(PackageAdmin.class.getName());
        if(ref == null) {
            throw new IllegalStateException("StartLevel service not found");
        }
        packageAdmin = (PackageAdmin)bundleContext.getService(ref);
        final int oldRefreshCount = refreshCount;
        
        try {
            packageAdmin.refreshPackages(null);
            
            final long end = System.currentTimeMillis() + maxMsecToWait;
            while(refreshCount == oldRefreshCount && System.currentTimeMillis() < end) {
                waiter.waitMsec(100);
            }
        } finally {
            bundleContext.ungetService(ref);
        }
        
        return refreshCount > oldRefreshCount;
    }
    
    @Override
    public void frameworkEvent(FrameworkEvent event) {
        if(event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
            refreshCount++;
        }
    }   
}