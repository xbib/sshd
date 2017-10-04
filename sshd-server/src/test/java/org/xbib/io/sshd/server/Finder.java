package org.xbib.io.sshd.server;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * A finder for traversing a NIO file system.
 */
public class Finder {

    private final FileSystem fileSystem;

    private final EnumSet<FileVisitOption> opts;

    private List<PathFile> input = new LinkedList<>();

    private Comparator<PathFile> comparator;

    public Finder(FileSystem fileSystem) {
        this(fileSystem, EnumSet.of(FileVisitOption.FOLLOW_LINKS));
    }

    public Finder(FileSystem fileSystem, EnumSet<FileVisitOption> opts) {
        this.fileSystem = fileSystem;
        this.opts = opts;
    }

    public Finder find(String path, String pattern) throws IOException {
        return find(null, null, fileSystem.getPath(path), pattern);
    }

    public Finder find(String base, String basePattern, String path, String pattern) throws IOException {
        return find(fileSystem.getPath(base), basePattern,
                fileSystem.getPath(path), pattern);
    }

    /**
     * Find the most recent version of a file/archive.
     *
     * @param base the path of the base directory
     * @param basePattern a pattern to match directory entries in the base directory or null to match '*'
     * @param path the path of the file/archive if no recent path can be found in the base directory
     * @param pattern the file name pattern to match
     * @return this Finder
     * @throws IOException
     */
    public Finder find(Path base, String basePattern, Path path, String pattern) throws IOException {
        return find(base, basePattern, path, pattern, null);
    }

    public Finder find(Path base, String basePattern, Path path, String pattern, FileTime modifiedSince)
            throws IOException {
        if (base != null) {
            final PathMatcher baseMatcher = base.getFileSystem()
                    .getPathMatcher("glob:" + (basePattern != null ? basePattern : "*"));
            Set<Path> recent = new TreeSet<>((p1, p2) -> p2.toString().compareTo(p1.toString()));
            List<Path> dir = Files.find(base, 1,
                    (p, a) -> p.toFile().isDirectory() && baseMatcher.matches(p.getFileName()),
                    FileVisitOption.FOLLOW_LINKS)
                    .collect(Collectors.toList());
            recent.addAll(dir);
            if (recent.isEmpty()) {
                return this;
            }
            path = recent.iterator().next();
        }
        String systemAndPattern = "glob:" + (pattern != null ? pattern : "*");
        PathMatcher pathMatcher = path.getFileSystem().getPathMatcher(systemAndPattern);
        Files.walkFileTree(path, opts, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                if (pathMatcher.matches(path.getFileName())) {
                    if (modifiedSince == null || attrs.lastModifiedTime().toMillis() > modifiedSince.toMillis()) {
                        input.add(new PathFile(path, attrs));
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return this;
    }

    public Finder sortBy(String mode) {
        if ("lastmodified".equals(mode)) {
            this.comparator = Comparator.comparing(p -> p.getAttributes().lastModifiedTime());
        } else if ("name".equals(mode)) {
            this.comparator = Comparator.comparing(p -> p.getPath().toString());
        }
        return this;
    }

    public Finder order(String mode) {
        if ("desc".equals(mode)) {
            this.comparator = Collections.reverseOrder(comparator);
        }
        return this;
    }

    public Queue<PathFile> getPathFiles() {
        if (comparator != null) {
            Collections.sort(input, comparator);
        }
        return new ConcurrentLinkedQueue<>(input);
    }

    public Queue<URI> getURIs() {
        return getURIs(-1);
    }

    public Queue<URI> getURIs(int max) {
        return getPathFiles().stream()
                .map(p -> p.getPath().toAbsolutePath().toUri())
                .limit(max < 0 ? input.size() : max)
                .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));
    }

    class PathFile {
        private Path path;
        private BasicFileAttributes attr;

        public PathFile(Path path, BasicFileAttributes attr) {
            this.path = path;
            this.attr = attr;
        }

        public Path getPath() {
            return path;
        }

        public BasicFileAttributes getAttributes() {
            return attr;
        }

        @Override
        public String toString() {
            return path.toString() + "[" + attr.toString() + "]";
        }
    }

}
