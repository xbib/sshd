package org.xbib.io.sshd.common;

import org.xbib.io.sshd.common.util.GenericUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 *
 */
@FunctionalInterface
public interface NamedResource {

    /**
     * Returns the value of {@link #getName()} - or {@code null} if argument is {@code null}
     */
    Function<NamedResource, String> NAME_EXTRACTOR = input -> input == null ? null : input.getName();

    /**
     * Compares 2 {@link NamedResource}s according to their {@link #getName()}
     * value case <U>insensitive</U>
     */
    Comparator<NamedResource> BY_NAME_COMPARATOR = Comparator.comparing(NAME_EXTRACTOR, String.CASE_INSENSITIVE_ORDER);

    /**
     * @param resources The named resources
     * @return A {@link List} of all the factories names - in same order
     * as they appear in the input collection
     */
    static List<String> getNameList(Collection<? extends NamedResource> resources) {
        return GenericUtils.map(resources, NamedResource::getName);
    }

    /**
     * @param resources list of available resources
     * @return A comma separated list of factory names
     */
    static String getNames(Collection<? extends NamedResource> resources) {
        return GenericUtils.join(getNameList(resources), ',');
    }

    /**
     * Remove the resource identified by the name from the list.
     *
     * @param <R>       The generic resource type
     * @param name      Name of the resource - ignored if {@code null}/empty
     * @param c         The {@link Comparator} to decide whether the {@link NamedResource#getName()}
     *                  matches the <tt>name</tt> parameter
     * @param resources The {@link NamedResource} to check - ignored if {@code null}/empty
     * @return the removed resource from the list or {@code null} if not in the list
     */
    static <R extends NamedResource> R removeByName(String name, Comparator<? super String> c, Collection<? extends R> resources) {
        R r = findByName(name, c, resources);
        if (r != null) {
            resources.remove(r);
        }
        return r;
    }

    /**
     * @param <R>       The generic resource type
     * @param name      Name of the resource - ignored if {@code null}/empty
     * @param c         The {@link Comparator} to decide whether the {@link NamedResource#getName()}
     *                  matches the <tt>name</tt> parameter
     * @param resources The {@link NamedResource} to check - ignored if {@code null}/empty
     * @return The <U>first</U> resource whose name matches the parameter (by invoking
     * {@link Comparator#compare(Object, Object)} - {@code null} if no match found
     */
    static <R extends NamedResource> R findByName(String name, Comparator<? super String> c, Collection<? extends R> resources) {
        return GenericUtils.isEmpty(name)
                ? null
                : GenericUtils.stream(resources)
                .filter(r -> c.compare(name, r.getName()) == 0)
                .findFirst()
                .orElse(null);
    }

    /**
     * @return The resource name
     */
    String getName();
}
