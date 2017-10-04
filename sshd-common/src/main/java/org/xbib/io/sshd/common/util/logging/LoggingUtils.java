package org.xbib.io.sshd.common.util.logging;

import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.NumberUtils;
import org.xbib.io.sshd.common.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public final class LoggingUtils {

    private LoggingUtils() {
        throw new UnsupportedOperationException("No instance");
    }

    /**
     * Scans using reflection API for all fields that are {@code public static final}
     * that start with the given common prefix (case <U>sensitive</U>) and are of type
     * {@link Number}.
     *
     * @param clazz        The {@link Class} to query
     * @param commonPrefix The expected common prefix
     * @return A {@link Map} of all the matching fields, where key=the field's {@link Integer}
     * value and mapping=the field's name
     * @see #generateMnemonicMap(Class, Predicate)
     */
    public static Map<Integer, String> generateMnemonicMap(Class<?> clazz, final String commonPrefix) {
        return generateMnemonicMap(clazz, f -> {
            String name = f.getName();
            return name.startsWith(commonPrefix);
        });
    }

    /**
     * Scans using reflection API for all <U>numeric {@code public static final}</U> fields
     * that are also accepted by the predicate. Any field that is not such or fail to retrieve
     * its value, or has a duplicate value is <U>silently</U> skipped.
     *
     * @param clazz    The {@link Class} to query
     * @param acceptor The {@link Predicate} used to decide whether to process the {@link Field}
     *                 (besides being a {@link Number} and {@code public static final}).
     * @return A {@link Map} of all the matching fields, where key=the field's {@link Integer}
     * value and mapping=the field's name
     * @see #getMnemonicFields(Class, Predicate)
     */
    public static Map<Integer, String> generateMnemonicMap(Class<?> clazz, Predicate<? super Field> acceptor) {
        Collection<Field> fields = getMnemonicFields(clazz, acceptor);
        if (GenericUtils.isEmpty(fields)) {
            return Collections.emptyMap();
        }

        Map<Integer, String> result = new HashMap<>(fields.size());
        for (Field f : fields) {
            String name = f.getName();
            try {
                Number value = (Number) f.get(null);
                String prev = result.put(NumberUtils.toInteger(value), name);
                if (prev != null) {
                    //noinspection UnnecessaryContinue
                    continue;   // debug breakpoint
                }
            } catch (Exception e) {
                //noinspection UnnecessaryContinue
                continue;   // debug breakpoint
            }
        }

        return result;
    }

    /**
     * Scans using reflection API for all <U>numeric {@code public static final}</U> fields
     * that have a common prefix and whose value is used by several of the other
     * matching fields
     *
     * @param clazz        The {@link Class} to query
     * @param commonPrefix The expected common prefix
     * @return A {@link Map} of all the mnemonic fields names whose value is the same as other
     * fields in this map. The key is the field's name and value is its associated opcode.
     * @see #getAmbiguousMenmonics(Class, Predicate)
     */
    public static Map<String, Integer> getAmbiguousMenmonics(Class<?> clazz, String commonPrefix) {
        return getAmbiguousMenmonics(clazz, f -> {
            String name = f.getName();
            return name.startsWith(commonPrefix);
        });
    }

    /**
     * Scans using reflection API for all <U>numeric {@code public static final}</U> fields
     * that are also accepted by the predicate and whose value is used by several of the other
     * matching fields
     *
     * @param clazz    The {@link Class} to query
     * @param acceptor The {@link Predicate} used to decide whether to process the {@link Field}
     *                 (besides being a {@link Number} and {@code public static final}).
     * @return A {@link Map} of all the mnemonic fields names whose value is the same as other
     * fields in this map. The key is the field's name and value is its associated opcode.
     * @see #getMnemonicFields(Class, Predicate)
     */
    public static Map<String, Integer> getAmbiguousMenmonics(Class<?> clazz, Predicate<? super Field> acceptor) {
        Collection<Field> fields = getMnemonicFields(clazz, acceptor);
        if (GenericUtils.isEmpty(fields)) {
            return Collections.emptyMap();
        }

        Map<String, Integer> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<Integer, List<String>> opcodesMap = new HashMap<>(fields.size());
        for (Field f : fields) {
            String name = f.getName();
            try {
                Number value = (Number) f.get(null);
                Integer key = NumberUtils.toInteger(value);
                List<String> nameList = opcodesMap.computeIfAbsent(key, k -> new ArrayList<>());
                nameList.add(name);

                int numOpcodes = nameList.size();
                if (numOpcodes > 1) {
                    result.put(name, key);
                    if (numOpcodes == 2) {  // add the 1st name as well
                        result.put(nameList.get(0), key);
                    }
                }
            } catch (Exception e) {
                continue;   // debug breakpoint
            }
        }

        return result;
    }

    /**
     * Scans using reflection API for all <U>numeric {@code public static final}</U> fields
     * that are also accepted by the predicate.
     *
     * @param clazz    The {@link Class} to query
     * @param acceptor The {@link Predicate} used to decide whether to process the {@link Field}
     *                 (besides being a {@link Number} and {@code public static final}).
     * @return A {@link Collection} of all the fields that have satisfied all conditions
     */
    public static Collection<Field> getMnemonicFields(Class<?> clazz, Predicate<? super Field> acceptor) {
        return ReflectionUtils.getMatchingFields(clazz, f -> {
            int mods = f.getModifiers();
            if ((!Modifier.isPublic(mods)) || (!Modifier.isStatic(mods)) || (!Modifier.isFinal(mods))) {
                return false;
            }

            Class<?> type = f.getType();
            if (!NumberUtils.isNumericClass(type)) {
                return false;
            }

            return acceptor.test(f);
        });
    }

    /**
     * Verifies if the given level is above the required threshold for logging.
     *
     * @param level     The {@link Level} to evaluate
     * @param threshold The threshold {@link Level}
     * @return {@code true} if the evaluated level is above the required
     * threshold.
     * <B>Note(s):</B>
     * <UL>
     * <LI>
     * If either argument is {@code null} then result is {@code false}.
     * </LI>
     * <LI>
     * If the evaluated level is {@link Level#OFF} then result is {@code false}
     * regardless of the threshold.
     * </LI>
     * <LI>
     * If the threshold is {@link Level#ALL} and the evaluated level is
     * <U>not</U> {@link Level#OFF} the result is {@code true}.
     * </LI>
     * <LI>
     * Otherwise, the evaluated level {@link Level#intValue()} must be
     * greater or equal to the threshold.
     * </LI>
     * </UL>
     */
    public static boolean isLoggable(Level level, Level threshold) {
        if ((level == null) || (threshold == null)) {
            return false;
        } else if (Level.OFF.equals(level) || Level.OFF.equals(threshold)) {
            return false;
        } else if (Level.ALL.equals(threshold)) {
            return true;
        } else {
            return level.intValue() >= threshold.intValue();
        }
    }

    public static SimplifiedLog wrap(final Logger logger) {
        if (logger == null) {
            return SimplifiedLog.EMPTY;
        } else {
            return new SimplifiedLog() {
                @Override
                public void log(Level level, Object message, Throwable t) {
                    if (isEnabled(level)) {
                        logMessage(logger, level, message, t);
                    }

                }

                @Override
                public boolean isEnabled(Level level) {
                    return isLoggable(logger, level);
                }
            };
        }
    }

    // NOTE: assume that level enabled has been checked !!!
    public static void logMessage(Logger logger, Level level, Object message, Throwable t) {
        if ((logger == null) || (level == null) || Level.OFF.equals(level)) {
            return;
        } else if (Level.SEVERE.equals(level)) {
            logger.log(Level.SEVERE, Objects.toString(message), t);
        } else if (Level.WARNING.equals(level)) {
            logger.log(Level.WARNING, Objects.toString(message), t);
        } else if (Level.INFO.equals(level) || Level.ALL.equals(level)) {
            logger.log(Level.INFO, Objects.toString(message), t);
        } else if (Level.CONFIG.equals(level) || Level.FINE.equals(level)) {
            logger.log(Level.FINE, Objects.toString(message), t);
        } else {
            logger.log(Level.FINEST, Objects.toString(message), t);
        }
    }

    /**
     * @param logger The {@link Logger} instance - ignored if {@code null}
     * @param level  The validate log {@link Level} - ignored if {@code null}
     * @return <P>{@code true} if the level is enabled for the logger.
     */
    public static boolean isLoggable(Logger logger, Level level) {
        if ((logger == null) || (level == null) || Level.OFF.equals(level)) {
            return false;
        } else {
            return logger.isLoggable(level);
        }
    }
}
