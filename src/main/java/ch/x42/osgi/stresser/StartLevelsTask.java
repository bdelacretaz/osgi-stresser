package ch.x42.osgi.stresser;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

public class StartLevelsTask extends TaskBase implements FrameworkListener {
    int counter;
    private final StartLevel startLevel;
    private int currentStartLevel = -1;
    private final long LEVEL_WAIT_MSEC = 10000;
    private final List<Integer> levels = new ArrayList<Integer>();
    
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
        log.info("Running cycle {}, will set start levels {}", ++counter, levels);
        
        for(int level : levels) {
            log.info("Setting start level {} (from {}) and waiting up to {} msec to see it", 
                    new Object[] { level, currentStartLevel, LEVEL_WAIT_MSEC });
            startLevel.setStartLevel(level);
            final long end = System.currentTimeMillis() + LEVEL_WAIT_MSEC;
            while(currentStartLevel != level && System.currentTimeMillis() < end) {
                waitMsec(100);
            }
            if(currentStartLevel == level) {
                log.info("Start level is now {}", currentStartLevel);
            } else {
                log.warn("Failed to set start level {}, current level={}", level, currentStartLevel);
            }
        }
    }
    
    protected void processCommand(String [] cmd, PrintWriter out) {
        super.processCommand(cmd, out);
        if(cmd.length > 2) {
            levels.clear();
            for(int i=2; i < cmd.length; i++) {
                final String val = cmd[i];
                try {
                    levels.add(Integer.parseInt(val));
                } catch(NumberFormatException nfe) {
                    out.println("Invalid start level '" + val + "', should be an Integer");
                }
            }
            out.println("List of start levels set to " + levels);
        }
    }

    @Override
    public void frameworkEvent(FrameworkEvent event) {
        currentStartLevel = startLevel.getStartLevel();
    }
}