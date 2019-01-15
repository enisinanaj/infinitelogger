package at.jit.logging;

import org.komamitsu.fluency.Fluency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class LogMe {

    private static final Logger logger = LoggerFactory.getLogger(LogMe.class);
    private static Fluency fluency = Fluency.defaultFluency(Arrays.asList(new InetSocketAddress(24231)));

    public static void main(String[] args) {
        Random random = new Random();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                for(int i=1; i<=i+1; i++) {

                    String session = String.valueOf(random.nextInt(99));
                    MDC.put("session", session);
                    MDC.put("loop", String.valueOf(i));

                    // Trace information for the loop run
                    logger.trace("Iteration '{}' and session '{}'", i, session);
                    try {
                        sendTraceToFluentd(session, i);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Log some errors, warns, infos, and debugs
                    if(i % 15 == 0){
                        try {
                            throw new RuntimeException("Bad runtime...");
                        } catch (RuntimeException e) {
                            MDC.put("user_experience", "\uD83E\uDD2C");
                            try {
                                sendErrorToFluentd(e);
                            } catch (IOException fluencyException) {
                                fluencyException.printStackTrace();
                            }
                        }
                    } else if (i % 5 == 0){
                        logger.warn("Investigate tomorrow");
                        try {
                            sendToFluentd("Investigate tomorrow", "WARN");
                        } catch (IOException fluencyException) {
                            fluencyException.printStackTrace();
                        }
                    } else if (i % 3 == 0){
                        logger.info("Collect in info");
                        try {
                            sendToFluentd("Collect in info", "INFO");
                        } catch (IOException fluencyException) {
                            fluencyException.printStackTrace();
                        }
                    } else {
                        logger.debug("Collect debug");
                        try {
                            sendToFluentd("Collect debug", "DEBUG");
                        } catch (IOException fluencyException) {
                            fluencyException.printStackTrace();
                        }
                    }

                    MDC.clear();

                    try {
                        Thread.sleep(3*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        //fluency.close();

        r.run();
    }

    private static void sendToFluentd(String message, String level) throws IOException {
        String tag = "fluency.java.app";
        Map<String, Object> event = new HashMap<String, Object>();
        event.put("message", message);
        event.put("level", level);
        fluency.emit(tag, event);
    }

    private static void sendErrorToFluentd(RuntimeException e) throws IOException {
        String tag = "fluency.java.app";
        Map<String, Object> event = new HashMap<String, Object>();
        event.put("exception", e);
        event.put("message", e.getMessage());
        event.put("level", "ERROR");
        fluency.emit(tag, event);
    }

    private static void sendTraceToFluentd(String session, int loop) throws IOException {
        String tag = "fluency.java.app";
        Map<String, Object> event = new HashMap<String, Object>();
        event.put("session", session);
        event.put("loop", loop);
        event.put("message", "Iteration '" + String.valueOf(loop) + "' and session '" + session + "'");
        event.put("level", "TRACE");
        fluency.emit(tag, event);
    }
}
