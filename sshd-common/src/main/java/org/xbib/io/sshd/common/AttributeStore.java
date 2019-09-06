package org.xbib.io.sshd.common;

import org.xbib.io.sshd.common.channel.Channel;
import org.xbib.io.sshd.common.session.Session;

import java.util.Objects;

/**
 * Provides the capability to attach in-memory attributes to the entity.
 */
public interface AttributeStore {
    /**
     * @param <T>     The generic attribute type
     * @param manager The {@link FactoryManager} - ignored if {@code null}
     * @param key     The attribute key - never {@code null}
     * @return Associated value - {@code null} if not found
     */
    static <T> T resolveAttribute(FactoryManager manager, AttributeKey<T> key) {
        Objects.requireNonNull(key, "No key");
        return (manager == null) ? null : manager.getAttribute(key);
    }

    /**
     * Attempts to use the session's attribute, if not found then tries the factory manager
     *
     * @param <T>     The generic attribute type
     * @param session The {@link Session} - ignored if {@code null}
     * @param key     The attribute key - never {@code null}
     * @return Associated value - {@code null} if not found
     * @see Session#getFactoryManager()
     * @see #resolveAttribute(FactoryManager, AttributeKey)
     */
    static <T> T resolveAttribute(Session session, AttributeKey<T> key) {
        Objects.requireNonNull(key, "No key");
        if (session == null) {
            return null;
        }

        T value = session.getAttribute(key);
        return (value != null) ? value : resolveAttribute(session.getFactoryManager(), key);
    }

    /**
     * Attempts to use the channel attribute, if not found then tries the session
     *
     * @param <T>     The generic attribute type
     * @param channel The {@link Channel} - ignored if {@code null}
     * @param key     The attribute key - never {@code null}
     * @return Associated value - {@code null} if not found
     * @see Session#getFactoryManager()
     * @see #resolveAttribute(Session, AttributeKey)
     */
    static <T> T resolveAttribute(Channel channel, AttributeKey<T> key) {
        Objects.requireNonNull(key, "No key");
        if (channel == null) {
            return null;
        }

        T value = channel.getAttribute(key);
        return (value != null) ? value : resolveAttribute(channel.getSession(), key);
    }

    /**
     * Returns the value of the user-defined attribute.
     *
     * @param <T> The generic attribute type
     * @param key The key of the attribute; must not be {@code null}.
     * @return {@code null} if there is no value associated with the specified key
     */
    <T> T getAttribute(AttributeKey<T> key);

    /**
     * Sets a user-defined attribute.
     *
     * @param <T>   The generic attribute type
     * @param key   The key of the attribute; must not be {@code null}.
     * @param value The value of the attribute; must not be {@code null}.
     * @return The old value of the attribute; {@code null} if it is new.
     */
    <T> T setAttribute(AttributeKey<T> key, T value);

    /**
     * Removes the user-defined attribute
     *
     * @param <T> The generic attribute type
     * @param key The key of the attribute; must not be {@code null}.
     * @return The removed value; {@code null} if no previous value
     */
    <T> T removeAttribute(AttributeKey<T> key);

    /**
     * Attempts to resolve the associated value by going up the store's
     * hierarchy (if any)
     *
     * @param <T> The generic attribute type
     * @param key The key of the attribute; must not be {@code null}.
     * @return {@code null} if there is no value associated with the specified key
     */
    <T> T resolveAttribute(AttributeKey<T> key);

    /**
     * Type safe key for storage of user attributes. Typically it is used as a static
     * variable that is shared between the producer and the consumer. To further
     * restrict access the setting or getting it from the store one can add static
     * {@code get/set methods} e.g:
     * <pre>
     * public static final AttributeKey&lt;MyValue&gt; MY_KEY = new AttributeKey&lt;MyValue&gt;();
     *
     * public static MyValue getMyValue(Session s) {
     *   return s.getAttribute(MY_KEY);
     * }
     *
     * public static void setMyValue(Session s, MyValue value) {
     *   s.setAttribute(MY_KEY, value);
     * }
     * </pre>
     *
     * @param <T> type of value stored in the attribute.
     */
    class AttributeKey<T> {
        public AttributeKey() {
            super();
        }
    }
}
