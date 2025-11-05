package org.infinitytwo.umbralore.core.logging;

import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.CheckReturnValue;
import org.slf4j.spi.LoggingEventBuilder;

public class Logger {
    protected final org.slf4j.Logger logger;
    
    public Logger(Class<?> c) {
        this.logger = LoggerFactory.getLogger(c);
    }
    
    public String getName() {
        return logger.getName();
    }
    
    public void error(String s) {
        s = "\001b[31m" + s + "\001b[0m";
        
        logger.error(s);
    }
    
    public void debug(String s, Object... objects) {
        logger.debug(s, objects);
    }
    
    public void debug(Marker marker, String s, Throwable throwable) {
        logger.debug(marker, s, throwable);
    }
    
    public void trace(Marker marker, String s, Object o, Object o1) {
        s = s + "\001b[36m" + "\001b[0m";
        
        logger.trace(marker,s, o, o1);
    }
    
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }
    
    public void error(Marker marker, String s, Object o) {
        s = s + "\001b[31m" + "\001b[0m";
        
        logger.error(marker, s, o);
    }
    
    public void warn(Marker marker, String s, Object o) {
        s = s + "\001b[33m" + "\001b[0m";
        
        logger.warn(marker, s, o);
    }
    
    public void warn(String s) {
        s = s + "\001b[33m" + "\001b[0m";
        
        logger.warn(s);
    }
    
    public void trace(String s, Object o) {
        s = s + "\001b[36m" + "\001b[0m";
        
        logger.trace(s, o);
    }
    
    public void error(String s, Object o, Object o1) {
        s = s + "\001b[31m" + "\001b[0m";
        
        logger.error(s, o, o1);
    }
    
    public void info(Marker marker, String s, Object o) {
        logger.info(marker, s, o);
    }
    
    public void debug(String s, Object o) {
        logger.debug(s, o);
    }
    
    public void trace(Marker marker, String s) {
        s = s + "\001b[36m" + "\001b[0m";
        
        logger.trace(marker, s);
    }
    
    public void info(String s) {
        logger.info(s);
    }
    
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }
    
    public void warn(String s, Object... objects) {
        s = s + "\001b[33m" + "\001b[0m";
        
        logger.warn(s, objects);
    }
    
    public void error(Marker marker, String s, Object... objects) {
        s = s + "\001b[31m" + "\001b[0m";
        
        logger.error(marker, s, objects);
    }
    
    @CheckReturnValue
    public LoggingEventBuilder atTrace() {
        return logger.atTrace();
    }
    
    public void debug(Marker marker, String s, Object o) {
        logger.debug(marker, s, o);
    }
    
    public void error(Marker marker, String s, Throwable throwable) {
        s = s + "\001b[31m" + "\001b[0m";
        
        logger.error(marker, s, throwable);
    }
    
    public void info(Marker marker, String s, Object o, Object o1) {
        logger.info(marker, s, o, o1);
    }
    
    public void warn(String s, Object o, Object o1) {
        s = s + "\001b[33m" + "\001b[0m";
        
        logger.warn(s, o, o1);
    }
    
    @CheckReturnValue
    public LoggingEventBuilder atLevel(Level level) {
        return logger.atLevel(level);
    }
    
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }
    
    public void debug(Marker marker, String s, Object o, Object o1) {
        logger.debug(marker, s, o, o1);
    }
    
    public void warn(Marker marker, String s, Throwable throwable) {
        s = s + "\001b[33m" + "\001b[0m";
        
        logger.warn(marker, s, throwable);
    }
    
    public void trace(Marker marker, String s, Throwable throwable) {
        s = s + "\001b[36m" + "\001b[0m";
        
        logger.trace(marker, s, throwable);
    }
    
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }
    
    public void info(String s, Object... objects) {
        logger.info(s, objects);
    }
    
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }
    
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }
    
    public void trace(String s, Object... objects) {
        logger.trace(s, objects);
    }
    
    public void info(Marker marker, String s, Throwable throwable) {
        logger.info(marker, s, throwable);
    }
    
    public void trace(String s, Object o, Object o1) {
        s = s + "\001b[36m" + "\001b[0m";
        
        logger.trace(s, o, o1);
    }
    
    @CheckReturnValue
    public LoggingEventBuilder atInfo() {
        return logger.atInfo();
    }
    
    public void warn(Marker marker, String s) {
        s = s + "\001b[33m" + "\001b[0m";
        
        logger.warn(marker, s);
    }
    
    public void error(Marker marker, String s, Object o, Object o1) {
        s = s + "\001b[31m" + "\001b[0m";
        
        logger.error(marker, s, o, o1);
    }
    
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }
    
    public void debug(String s, Object o, Object o1) {
        logger.debug(s, o, o1);
    }
    
    @CheckReturnValue
    public LoggingEventBuilder atDebug() {
        return logger.atDebug();
    }
    
    public void info(Marker marker, String s) {
        logger.info(marker, s);
    }
    
    public void error(String s, Object o) {
        s = s + "\001b[31m" + "\001b[0m";
        
        logger.error(s, o);
    }
    
    public void trace(String s) {
        s = s + "\001b[36m" + "\001b[0m";
        
        logger.trace(s);
    }
    
    public void trace(Marker marker, String s, Object o) {
        s = s + "\001b[36m" + "\001b[0m";
        
        logger.trace(marker, s, o);
    }
    
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }
    
    public void warn(Marker marker, String s, Object o, Object o1) {
        s = s + "\001b[33m" + "\001b[0m";
        
        logger.warn(marker, s, o, o1);
    }
    
    public void debug(Marker marker, String s) {
        logger.debug(marker, s);
    }
    
    public void warn(String s, Object o) {
        s = s + "\001b[33m" + "\001b[0m";
        
        logger.warn(s, o);
    }
    
    public void error(String s, Object... objects) {
        s = s + "\001b[31m" + "\001b[0m";
        
        logger.error(s, objects);
    }
    
    public boolean isEnabledForLevel(Level level) {
        return logger.isEnabledForLevel(level);
    }
    
    public void debug(String s) {
        logger.debug(s);
    }
    
    public void info(String s, Object o) {
        logger.info(s, o);
    }
    
    public void warn(Marker marker, String s, Object... objects) {
        s = s + "\001b[33m" + "\001b[0m";
        
        logger.warn(marker, s, objects);
    }
    
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }
    
    public void info(String s, Object o, Object o1) {
        logger.info(s, o, o1);
    }
    
    @CheckReturnValue
    public LoggingEventBuilder atError() {
        return logger.atError();
    }
    
    public LoggingEventBuilder makeLoggingEventBuilder(Level level) {
        return logger.makeLoggingEventBuilder(level);
    }
    
    public void error(String s, Throwable throwable) {
        s = s + "\001b[31m" + "\001b[0m";
        
        logger.error(s, throwable);
    }
    
    public void trace(String s, Throwable throwable) {
        s = s + "\001b[36m" + "\001b[0m";
        
        logger.trace(s, throwable);
    }
    
    public void info(Marker marker, String s, Object... objects) {
        logger.info(marker, s, objects);
    }
    
    public void warn(String s, Throwable throwable) {
        s = s + "\001b[33m" + "\001b[0m";
        
        logger.warn(s, throwable);
    }
    
    public void debug(Marker marker, String s, Object... objects) {
        logger.debug(marker, s, objects);
    }
    
    public void trace(Marker marker, String s, Object... objects) {
        s = s + "\001b[36m" + "\001b[0m";
        
        logger.trace(marker, s, objects);
    }
    
    public void debug(String s, Throwable throwable) {
        logger.debug(s, throwable);
    }
    
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }
    
    public void info(String s, Throwable throwable) {
        logger.info(s, throwable);
    }
    
    @CheckReturnValue
    public LoggingEventBuilder atWarn() {
        return logger.atWarn();
    }
    
    public void error(Marker marker, String s) {
        s = s + "\001b[31m" + "\001b[0m";
        
        logger.error(marker, s);
    }
}
