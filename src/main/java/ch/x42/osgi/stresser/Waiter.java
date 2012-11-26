package ch.x42.osgi.stresser;

import java.util.Random;

class Waiter {
    
    private final Random random = new Random(42);
    
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
