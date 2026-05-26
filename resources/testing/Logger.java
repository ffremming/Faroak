package resources.testing;

/**
 * Minimal tagged + levelled logger for the test harness. Avoids pulling in a
 * full logging library: tests run from a single JVM and want stdout, not
 * structured logging.
 *
 * Usage:
 *   private static final Logger LOG = Logger.forClass(MyProbe.class);
 *   LOG.info("starting probe");
 *   LOG.warn("slow path: " + ms + "ms");
 */
public final class Logger {

    public enum Level { DEBUG, INFO, WARN, ERROR }

    private static volatile Level threshold = Level.INFO;

    private final String tag;

    private Logger(String tag) { this.tag = tag; }

    public static Logger forClass(Class<?> c)   { return new Logger(c.getSimpleName()); }
    public static Logger forTag(String tag)     { return new Logger(tag); }
    public static void   setThreshold(Level l)  { threshold = l; }

    public void debug(String msg) { log(Level.DEBUG, msg); }
    public void info(String msg)  { log(Level.INFO,  msg); }
    public void warn(String msg)  { log(Level.WARN,  msg); }
    public void error(String msg) { log(Level.ERROR, msg); }

    public void info(String fmt, Object... args)  { log(Level.INFO,  String.format(fmt, args)); }
    public void warn(String fmt, Object... args)  { log(Level.WARN,  String.format(fmt, args)); }
    public void debug(String fmt, Object... args) { log(Level.DEBUG, String.format(fmt, args)); }

    private void log(Level level, String msg) {
        if (level.ordinal() < threshold.ordinal()) return;
        System.out.println(String.format("[%-18s] %-5s %s", tag, level, msg));
    }
}
