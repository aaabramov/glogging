package io.github.aaabramov.glogging;

import org.slf4j.*;

import java.util.Random;


public class App {
    
    private final static Logger logger = LoggerFactory.getLogger(App.class);
    
    public static void main(String[] args) throws Exception {
        
        while (true) {
            
            MDC.put("name", "Andrii");
            
            logger.debug("debug");
            logger.info("info");
            logger.warn("warn");
            if (new Random().nextBoolean()) {
                logger.error("error", new RuntimeException("BOOM"));
            } else {
                logger.error("error");
            }
            MDC.clear();
            Thread.sleep(3000);
        }
        
    }
}
