package ch.x42.osgi.stresser;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

public class StartLevelsTask extends TaskBase implements FrameworkListener {
    int counter;
    private final StartLevel startLevel;
    private int currentStartLevel = -1;
    private final long LEVEL_WAIT_MSEC = 5000;
    
    StartLevelsTask(BundleContext bundleContext) {
        super("sl", bundleContext);
        final ServiceReference ref = bundleContext.getServiceReference(StartLevel.class.getName());
        if(ref == null) {
            log.warn("StartLevel service not found");
            startLevel = null;
        } else {
            startLevel = (StartLevel)bundleContext.getService(ref);
            frameworkEvent(null);
        }
        bundleContext.addFrameworkListener(this);
    }
    
    protected void runOneCycle() {
        log.info("Running cycle {}", ++counter);
        
        final int [] levels = { 8, 12, 7, 14, 30 };
        for(int level : levels) {
            log.info("Setting start level {} and waiting up to {} msec to see it", level, LEVEL_WAIT_MSEC);
            startLevel.setStartLevel(level);
            final long end = System.currentTimeMillis() + LEVEL_WAIT_MSEC;
            while(currentStartLevel != level && System.currentTimeMillis() < end) {
                waitMsec(100);
            }
            if(currentStartLevel != level) {
                log.warn("Failed to set start level {}, current level={}", level, currentStartLevel);
            }
        }
    }

    @Override
    public void frameworkEvent(FrameworkEvent event) {
        currentStartLevel = startLevel.getStartLevel();
    }
}