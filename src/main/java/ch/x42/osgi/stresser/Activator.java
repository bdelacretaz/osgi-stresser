package ch.x42.osgi.stresser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.x42.osgi.stresser.TaskBase.STATE;

public class Activator implements BundleActivator, Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private Map<String, TaskBase> tasks = new HashMap<String, TaskBase>();
    private ServerSocket commandSocket;
    private Socket clientSocket;
    private Thread commandThread;
    private boolean active;
    
    public static final long JOIN_TIMEOUT_MSEC = 5000L;
    public static final int COMMAND_PORT = Integer.getInteger("osgi.stresser.command.port", 1234);
    
    @Override
    public void start(BundleContext context) throws Exception {
        tasks.clear();
        closeSockets();
        
        final TaskBase [] tt = {
                new BundlesStartStopTask(context),
                new StartLevelsTask(context),
                new BundleUpdateTask(context),
                new RefreshPackagesTask(context)
        };
        for(TaskBase t : tt) {
            tasks.put(t.getTaskName(), t);
        }
        
        active = true;
        commandThread = new Thread(this, "OSGI stresser command thread");
        commandThread.start();
        log.info("Started, command port={}, tasks={}", COMMAND_PORT, tasks);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        
        log.info("Stopping command thread");
        active=false;
        closeSockets();
        commandThread.interrupt();
        commandThread.join(JOIN_TIMEOUT_MSEC);
        checkNotAlive(commandThread);
        
        for(TaskBase t : tasks.values()) {
            log.info("Stopping task {}, will wait up to {} msec for thread to end", t, JOIN_TIMEOUT_MSEC);
            t.setState(STATE.stopped);
            t.getThread().join(JOIN_TIMEOUT_MSEC);
            checkNotAlive(t.getThread());
        }
    }
    
    private void closeSockets() {
        try {
            if(clientSocket != null) {
                clientSocket.close();
            }
        } catch(IOException ignore) {
        }
        
        try {
            if(commandSocket != null) {
                commandSocket.close();
            }
        } catch(IOException ignore) {
        }
    }
    
    private void checkNotAlive(Thread t) {
        if(t.isAlive()) {
            log.warn("{} did not exit", t);
        } else {
            log.info("{} correctly stopped", t);
        }
    }
    
    private void processCommand(String cmd, PrintWriter out) {
        log.info("Processing command '{}'", cmd);
        final String [] words = cmd.split(" ");
        if(words.length >= 1) {
            final String task = words[0];
            if("*".equals(task)) {
                for(TaskBase t : tasks.values()) {
                    t.processCommand(words, out);
                }
            } else {
                final TaskBase t = tasks.get(task);
                if(t==null) {
                    out.println("Task not found: " + words[0]);
                } else {
                    t.processCommand(words, out);
                }
            }
            
            for(TaskBase tt : tasks.values()) {
                out.println(tt + " state=" + tt.getState());
            }
        } else {
            out.println("Empty command, cannot process");
        }
    }
    
    public void run() {
        try {
            commandSocket = new ServerSocket(COMMAND_PORT);
            while(active) {
                try {
                    clientSocket = commandSocket.accept();
                    final BufferedReader r = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    final PrintWriter w = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                    while(active) {
                        w.write("OSGI stresser> ");
                        w.flush();
                        final String cmd = r.readLine();
                        if(cmd == null || cmd.equals("exit")) {
                            break;
                        }
                        processCommand(cmd, w);
                        w.flush();
                    }
                } catch(IOException ioe) {
                    log.error("Exception in run()", ioe);
                    break;
                } finally {
                    try {
                        if(clientSocket != null) {
                            clientSocket.close();
                        }
                    } catch(IOException ignore) {
                    }
                }
            }
        } catch(IOException ioe) {
            log.error("Unable to open command socket on port " + COMMAND_PORT, ioe);
        } finally {
            closeSockets();
        }
    }
}
