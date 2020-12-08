/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mvndaemon.mvnd.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Collects system properties and environment variables used by mvnd client or server.
 *
 * Duration properties such as {@link #MVND_IDLE_TIMEOUT}, {@link #MVND_KEEP_ALIVE},
 * {@link #MVND_EXPIRATION_CHECK_DELAY} or {@link #MVND_LOG_PURGE_PERIOD} are expressed
 * in a human readable format such as <code>2h30m</code>, <code>600ms</code> or <code>10 seconds</code>.
 * The available units are <i>d/day/days</i>, <i>h/hour/hours</i>, <i>m/min/minute/minutes</i>,
 * <i>s/sec/second/seconds</i> and <i>ms/millis/msec/milliseconds</i>.
 */
public enum Environment {

    //
    // Log properties
    //

    /**
     * The location of the Logback configuration file the daemon should use to configure its logging.
     */
    LOGBACK_CONFIGURATION_FILE("logback.configurationFile", null, null, OptionType.PATH, Flags.NONE),

    //
    // System properties
    //
    /** Java home for starting the daemon */
    JAVA_HOME("java.home", "JAVA_HOME", null, OptionType.PATH, Flags.NONE),
    /**
     * The daemon installation directory. The client normally sets this according to where its <code>mvnd</code>
     * executable is located
     */
    MVND_HOME("mvnd.home", "MVND_HOME", null, OptionType.PATH, Flags.NONE),
    /** The user home directory */
    USER_HOME("user.home", null, null, OptionType.PATH, Flags.NONE),
    /** The current working directory */
    USER_DIR("user.dir", null, null, OptionType.PATH, Flags.NONE),

    //
    // Maven properties
    //
    /** The path to the Maven local repository */
    MAVEN_REPO_LOCAL("maven.repo.local", null, null, OptionType.PATH, Flags.NONE),
    /** The location of the maven settings file */
    MAVEN_SETTINGS("maven.settings", null, null, OptionType.PATH, Flags.NONE, "-s", "--settings"),
    /** The root directory of the current multi module Maven project */
    MAVEN_MULTIMODULE_PROJECT_DIRECTORY("maven.multiModuleProjectDirectory", null, null, OptionType.PATH, Flags.NONE),

    //
    // mvnd properties
    //

    /**
     * The location of the user supplied <code>mvnd.properties</code> file.
     */
    MVND_PROPERTIES_PATH("mvnd.propertiesPath", "MVND_PROPERTIES_PATH", null, OptionType.PATH, Flags.NONE),
    /**
     * The directory under which the daemon stores its registry, log files, etc.
     * Default: <code>${user.home}/.m2/mvnd</code>
     */
    MVND_DAEMON_STORAGE("mvnd.daemonStorage", null, null, OptionType.PATH, Flags.NONE),
    /**
     * The path to the daemon registry.
     * Default: <code>${mvnd.daemonStorage}/registry.bin</code>
     */
    MVND_REGISTRY("mvnd.registry", null, null, OptionType.PATH, Flags.NONE),
    /**
     * If <code>true</code> the log messages are displayed continuously like with stock Maven; otherwise buffer the
     * messages and output at the end of the build, grouped by module. Passing <code>-B</code> or
     * <code>--batch-mode</code> on the command line enables this too for the given build.
     */
    MVND_NO_BUFERING("mvnd.noBuffering", null, Boolean.FALSE, OptionType.BOOLEAN, Flags.NONE),
    /**
     * The number of log lines to display for each Maven module that is built in parallel. The value can be increased
     * or decreased by pressing + or - key during the build respectively. This option has no effect with
     * <code>-Dmvnd.noBuffering=true</code>, <code>-B</code> or <code>--batch-mode</code>.
     */
    MVND_ROLLING_WINDOW_SIZE("mvnd.rollingWindowSize", null, "0", OptionType.INTEGER, Flags.NONE),
    /**
     * Daemon log files older than this value will be removed automatically.
     */
    MVND_LOG_PURGE_PERIOD("mvnd.logPurgePeriod", null, "7 days", OptionType.DURATION, Flags.NONE),
    /**
     * If <code>true</code>, the client and daemon will run in the same JVM that exits when the build is finished;
     * otherwise the client starts/connects to a long living daemon process. This option is only available with
     * non-native clients and is useful mostly for debugging.
     */
    MVND_NO_DAEMON("mvnd.noDaemon", "MVND_NO_DAEMON", Boolean.FALSE, OptionType.BOOLEAN, Flags.DISCRIMINATING),
    /**
     * If <code>true</code>, the daemon will be launched in debug mode with the following JVM argument:
     * <code>-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000</code>; otherwise the debug argument is
     * not passed to the daemon.
     */
    MVND_DEBUG("mvnd.debug", null, Boolean.FALSE, OptionType.BOOLEAN, Flags.DISCRIMINATING),
    /**
     * A time period after which an unused daemon will terminate by itself.
     */
    MVND_IDLE_TIMEOUT("mvnd.idleTimeout", null, "3 hours", OptionType.DURATION, Flags.DISCRIMINATING),
    /**
     * If the daemon does not send any message to the client in this period of time, send a keep-alive message so that
     * the client knows that the daemon is still alive.
     */
    MVND_KEEP_ALIVE("mvnd.keepAlive", null, "100 ms", OptionType.DURATION, Flags.DISCRIMINATING),
    /**
     * The maximum number of keep alive messages that can be missed by the client before the client considers the daemon
     * to be dead.
     */
    MVND_MAX_LOST_KEEP_ALIVE("mvnd.maxLostKeepAlive", null, 30, OptionType.INTEGER, Flags.NONE),
    /**
     * The minimum number of threads to use when constructing the default <code>-T</code> parameter for the daemon.
     * This value is ignored if the user passes <code>-T</code>, <code>--threads</code> or <code>-Dmvnd.threads</code>
     * on the command line or if he sets <code>mvnd.threads</code> in <code>~/.m2/mvnd.properties</code>.
     */
    MVND_MIN_THREADS("mvnd.minThreads", null, 1, OptionType.INTEGER, Flags.NONE),
    /**
     * The number of threads to pass to the daemon; same syntax as Maven's <code>-T</code>/<code>--threads</code>
     * option.
     */
    MVND_THREADS("mvnd.threads", null, null, OptionType.STRING, Flags.NONE, "-T", "--threads"),
    /**
     * The builder implementation the daemon should use
     */
    MVND_BUILDER("mvnd.builder", null, "smart", OptionType.STRING, Flags.NONE, "-b", "--builder"),
    /**
     * An ID for a newly started daemon
     */
    MVND_UID("mvnd.uid", null, null, OptionType.STRING, Flags.INTERNAL),
    /**
     * Internal option to specify the maven extension classpath
     */
    MVND_EXT_CLASSPATH("mvnd.extClasspath", null, null, OptionType.STRING, Flags.DISCRIMINATING | Flags.INTERNAL),
    /**
     * Internal option to specify the list of maven extension to register
     */
    MVND_CORE_EXTENSIONS("mvnd.coreExtensions", null, null, OptionType.STRING, Flags.DISCRIMINATING | Flags.INTERNAL),
    /**
     * The <code>-Xms</code> value to pass to the daemon
     */
    MVND_MIN_HEAP_SIZE("mvnd.minHeapSize", null, "128M", OptionType.MEMORY_SIZE, Flags.DISCRIMINATING),
    /**
     * The <code>-Xmx</code> value to pass to the daemon
     */
    MVND_MAX_HEAP_SIZE("mvnd.maxHeapSize", null, "2G", OptionType.MEMORY_SIZE, Flags.DISCRIMINATING),
    /**
     * Additional JVM args to pass to the daemon
     */
    MVND_JVM_ARGS("mvnd.jvmArgs", null, null, OptionType.STRING, Flags.DISCRIMINATING | Flags.OPTIONAL),
    /**
     * If <code>true</code>, the <code>-ea</code> option will be passed to the daemon; otherwise the <code>-ea</code>
     * option is not passed to the daemon.
     */
    MVND_ENABLE_ASSERTIONS("mvnd.enableAssertions", null, Boolean.FALSE, OptionType.BOOLEAN, Flags.DISCRIMINATING),
    /**
     * The daemon will check this often whether it should exit.
     */
    MVND_EXPIRATION_CHECK_DELAY("mvnd.expirationCheckDelay", null, "10 seconds", OptionType.DURATION, Flags.DISCRIMINATING),
    /**
     * Period after which idle duplicate daemons will be shut down. Duplicate daemons are daemons with the same set of
     * discriminating start parameters.
     */
    MVND_DUPLICATE_DAEMON_GRACE_PERIOD("mvnd.duplicateDaemonGracePeriod", null, "10 seconds", OptionType.DURATION,
            Flags.DISCRIMINATING),
            ;

    static Properties properties;

    public static void setProperties(Properties properties) {
        Environment.properties = properties;
    }

    public static String getProperty(String property) {
        Properties props = Environment.properties;
        if (props == null) {
            props = System.getProperties();
        }
        return props.getProperty(property);
    }

    private final String property;
    private final String environmentVariable;
    private final String default_;
    private final int flags;
    private final OptionType type;
    private final List<String> options;

    Environment(String property, String environmentVariable, Object default_, OptionType type, int flags,
            String... options) {
        this.property = Objects.requireNonNull(property);
        this.environmentVariable = environmentVariable;
        this.default_ = default_ != null ? default_.toString() : null;
        this.flags = flags;
        this.type = type;
        this.options = options.length == 0 ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(options));
    }

    public String getProperty() {
        return property;
    }

    public String getEnvironmentVariable() {
        return environmentVariable;
    }

    public String getDefault() {
        return default_;
    }

    public List<String> getOptions() {
        return options;
    }

    public OptionType getType() {
        return type;
    }

    public boolean isDiscriminating() {
        return (flags & Flags.DISCRIMINATING) != 0;
    }

    public boolean isInternal() {
        return (flags & Flags.INTERNAL) != 0;
    }

    public boolean isOptional() {
        return (flags & Flags.OPTIONAL) != 0;
    }

    public String asString() {
        String val = getProperty(property);
        if (val == null) {
            throw new IllegalStateException("The system property " + property + " is missing");
        }
        return val;
    }

    public Optional<String> asOptional() {
        String val = getProperty(property);
        if (val != null) {
            return Optional.of(val);
        } else if (isOptional()) {
            return Optional.empty();
        } else {
            throw new IllegalStateException("The system property " + property + " is missing");
        }
    }

    public int asInt() {
        return Integer.parseInt(asString());
    }

    public boolean asBoolean() {
        return Boolean.parseBoolean(asString());
    }

    public Path asPath() {
        String result = asString();
        if (Os.current().isCygwin()) {
            result = cygpath(result);
        }
        return Paths.get(result);
    }

    public Duration asDuration() {
        return TimeUtils.toDuration(asString());
    }

    public String asDaemonOpt(String value) {
        return property + "=" + type.normalize(value);
    }

    public void appendAsCommandLineOption(Consumer<String> args, String value) {
        if (!options.isEmpty()) {
            args.accept(options.get(0));
            args.accept(type.normalize(value));
        } else {
            args.accept("-D" + property + "=" + type.normalize(value));
        }
    }

    public boolean hasCommandOption(Collection<String> args) {
        final String[] prefixes;
        if (options.isEmpty()) {
            prefixes = new String[] { "-D" + property + "=" };
        } else if (property != null) {
            prefixes = new String[options.size() + 1];
            options.toArray(prefixes);
            prefixes[options.size()] = "-D" + property + "=";
        } else {
            prefixes = options.toArray(new String[options.size()]);
        }
        return args.stream().anyMatch(arg -> Stream.of(prefixes).anyMatch(arg::startsWith));
    }

    public static String cygpath(String result) {
        String path = result.replace('/', '\\');
        if (path.matches("\\\\cygdrive\\\\[a-z]\\\\.*")) {
            String s = path.substring("\\cygdrive\\".length());
            result = s.substring(0, 1).toUpperCase(Locale.ENGLISH) + ":" + s.substring(1);
        }
        return result;
    }

    public static boolean isNative() {
        return "executable".equals(System.getProperty("org.graalvm.nativeimage.kind"));
    }

    public static Stream<DocumentedEnumEntry<Environment>> documentedEntries() {
        Properties props = new Properties();
        Environment[] values = values();
        final String cliOptionsPath = values[0].getClass().getSimpleName() + ".javadoc.properties";
        try (InputStream in = Environment.class.getResourceAsStream(cliOptionsPath)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + cliOptionsPath, e);
        }
        return Stream.of(values)
                .filter(env -> !env.isInternal())
                .sorted(Comparator.comparing(Environment::getProperty))
                .map(env -> new DocumentedEnumEntry<>(env, props.getProperty(env.name())));
    }

    public static class DocumentedEnumEntry<E> {

        private final E entry;
        private final String javaDoc;

        public DocumentedEnumEntry(E entry, String javaDoc) {
            this.entry = entry;
            this.javaDoc = javaDoc;
        }

        public E getEntry() {
            return entry;
        }

        public String getJavaDoc() {
            return javaDoc;
        }
    }

    static class Flags {
        private static final int NONE = 0b0;
        private static final int DISCRIMINATING = 0b1;
        private static final int INTERNAL = 0b10;
        public static final int OPTIONAL = 0b100;
    }

}