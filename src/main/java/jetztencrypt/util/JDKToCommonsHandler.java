package jetztencrypt.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JDKToCommonsHandler extends Handler {

	// state

	private final ConcurrentMap<String,Log> logs = new ConcurrentHashMap<>();

	// impl

	@Override
	public void publish(LogRecord record) {
		String name = record.getLoggerName();

		Log log = logs.get(name);
		if( log==null )
			logs.put(name,log=LogFactory.getLog(name));

		String message = record.getMessage();
		Throwable ex = record.getThrown();
		Level level = record.getLevel();

		if( Level.SEVERE==level )
			log.error(message,ex);
		else if( Level.WARNING==level )
			log.warn(message,ex);
		else if( Level.INFO==level )
			log.info(message,ex);
		else if( Level.CONFIG==level )
			log.debug(message,ex);
		else
			log.trace(message,ex);
	}

	@Override public void flush() {}
	@Override public void close() throws SecurityException {}

	// static stuff

	public static final JDKToCommonsHandler DEFAULT = new JDKToCommonsHandler();

	static {
		DEFAULT.setLevel(Level.ALL);
	}

	public static JDKToCommonsHandler rerouteJDKToCommons(Level level) {
		Logger root = Logger.getLogger("");

		{
			Handler[] handlers = root.getHandlers();
			if( handlers!=null )
				for(Handler h:handlers)
					root.removeHandler(h);
		}

		root.addHandler(DEFAULT);
		root.setLevel(level);

		return DEFAULT;
	}

}
