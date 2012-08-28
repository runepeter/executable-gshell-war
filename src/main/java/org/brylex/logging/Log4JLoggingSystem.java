package org.brylex.logging;

import org.apache.log4j.LogManager;
import org.slf4j.LoggerFactory;
import org.sonatype.gshell.logging.Component;
import org.sonatype.gshell.logging.Level;
import org.sonatype.gshell.logging.Logger;
import org.sonatype.gshell.logging.LoggingSystem;

import javax.inject.Singleton;
import java.util.*;

@Singleton
public class Log4JLoggingSystem implements LoggingSystem
{
    private final Map<String, Level> levelMap;

    public Log4JLoggingSystem()
    {
        Object factoryRef = LoggerFactory.getILoggerFactory();
        if (!"org.slf4j.impl.Log4jLoggerFactory".equals(factoryRef.getClass().getName()))
        {
            throw new IllegalStateException("SLF4J is NOT configured to use log4j (but [" + factoryRef.getClass().getName() + "])");
        }

        org.apache.log4j.Level[] source = {
                org.apache.log4j.Level.ALL,
                org.apache.log4j.Level.TRACE,
                org.apache.log4j.Level.DEBUG,
                org.apache.log4j.Level.INFO,
                org.apache.log4j.Level.WARN,
                org.apache.log4j.Level.ERROR,
                org.apache.log4j.Level.FATAL,
                org.apache.log4j.Level.OFF
        };

        Map<String, Level> map = new HashMap<String, Level>(source.length);
        for (org.apache.log4j.Level level : source)
        {
            map.put(level.toString().toUpperCase(), new LevelImpl(level));
        }

        this.levelMap = Collections.unmodifiableMap(map);
    }

    public Level getLevel(final String name)
    {
        assert name != null;
        Level level = levelMap.get(name.toUpperCase());
        if (level == null)
        {
            throw new RuntimeException("Invalid level name: " + name);
        }
        return level;
    }

    public Collection<? extends Level> getLevels()
    {
        return levelMap.values();
    }

    public Logger getLogger(String loggerName)
    {
        return new LoggerImpl(org.apache.log4j.Logger.getLogger(loggerName));
    }

    public Collection<String> getLoggerNames()
    {
        List<String> list = new ArrayList<String>();

        Enumeration enumeration = LogManager.getCurrentLoggers();
        while (enumeration.hasMoreElements())
        {
            org.apache.log4j.Logger logger = (org.apache.log4j.Logger) enumeration.nextElement();
            list.add(logger.getName());
        }

        return Collections.unmodifiableCollection(list);
    }

    public Collection<? extends Component> getComponents()
    {
        return Collections.emptySet();
    }

    private class LevelImpl implements Level
    {
        private final org.apache.log4j.Level level;

        public LevelImpl(final org.apache.log4j.Level level)
        {
            this.level = level;
        }

        public String getName()
        {
            return level.toString();
        }

        @Override
        public int hashCode()
        {
            return getName().hashCode();
        }

        @Override
        public String toString()
        {
            return getName();
        }

        private org.apache.log4j.Level getLog4jLevel()
        {
            return level;
        }
    }

    private class LoggerImpl implements Logger
    {
        private final org.apache.log4j.Logger targetLogger;

        public LoggerImpl(org.apache.log4j.Logger targetLogger)
        {
            this.targetLogger = targetLogger;
        }

        public String getName()
        {
            return targetLogger.getName();
        }

        public Level getLevel()
        {
            org.apache.log4j.Level log4jLevel = targetLogger.getLevel();

            return levelMap.get(log4jLevel.toString().toUpperCase());
        }

        public void setLevel(Level level)
        {
            targetLogger.setLevel(((LevelImpl) level).getLog4jLevel());
        }

        public void setLevel(String levelName)
        {
            setLevel(levelMap.get(levelName));
        }

        public boolean isRoot()
        {
            return org.apache.log4j.Logger.getRootLogger().equals(targetLogger);
        }

        @Override
        public int hashCode()
        {
            return getName().hashCode();
        }

        @Override
        public String toString()
        {
            return getName();
        }

    }

}
