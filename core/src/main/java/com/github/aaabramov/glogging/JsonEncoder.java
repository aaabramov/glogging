package com.github.aaabramov.glogging;

interface JsonEncoder {
    
    String toJson(GcpLoggingEvent event);
    
}