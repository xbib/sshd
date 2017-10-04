package org.xbib.io.sshd.server.subsystem.sftp;

import org.xbib.io.sshd.common.subsystem.sftp.SftpConstants;
import org.xbib.io.sshd.common.util.GenericUtils;
import org.xbib.io.sshd.common.util.ObjectBuilder;
import org.xbib.io.sshd.common.util.threads.ExecutorServiceConfigurer;
import org.xbib.io.sshd.server.Command;
import org.xbib.io.sshd.server.subsystem.SubsystemFactory;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 *
 */
public class SftpSubsystemFactory
        extends AbstractSftpEventListenerManager
        implements SubsystemFactory, ExecutorServiceConfigurer, SftpEventListenerManager, SftpFileSystemAccessorManager {
    public static final String NAME = SftpConstants.SFTP_SUBSYSTEM_NAME;
    public static final UnsupportedAttributePolicy DEFAULT_POLICY = UnsupportedAttributePolicy.Warn;
    private ExecutorService executors;
    private boolean shutdownExecutor;
    private UnsupportedAttributePolicy policy = DEFAULT_POLICY;
    private SftpFileSystemAccessor fileSystemAccessor = SftpFileSystemAccessor.DEFAULT;
    public SftpSubsystemFactory() {
        super();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executors;
    }

    /**
     * @param service The {@link ExecutorService} to be used by the {@link org.xbib.io.sshd.server.subsystem.sftp.SftpSubsystem}
     *                command when starting execution. If {@code null} then a single-threaded ad-hoc service is used.
     */
    @Override
    public void setExecutorService(ExecutorService service) {
        executors = service;
    }

    @Override
    public boolean isShutdownOnExit() {
        return shutdownExecutor;
    }

    /**
     * @param shutdownOnExit If {@code true} the {@link ExecutorService#shutdownNow()}
     *                       will be called when subsystem terminates - unless it is the ad-hoc service, which
     *                       will be shutdown regardless
     */
    @Override
    public void setShutdownOnExit(boolean shutdownOnExit) {
        shutdownExecutor = shutdownOnExit;
    }

    public UnsupportedAttributePolicy getUnsupportedAttributePolicy() {
        return policy;
    }

    /**
     * @param p The {@link UnsupportedAttributePolicy} to use if failed to access
     *          some local file attributes - never {@code null}
     */
    public void setUnsupportedAttributePolicy(UnsupportedAttributePolicy p) {
        policy = Objects.requireNonNull(p, "No policy");
    }

    @Override
    public SftpFileSystemAccessor getFileSystemAccessor() {
        return fileSystemAccessor;
    }

    @Override
    public void setFileSystemAccessor(SftpFileSystemAccessor accessor) {
        fileSystemAccessor = Objects.requireNonNull(accessor, "No accessor");
    }

    @Override
    public Command create() {
        org.xbib.io.sshd.server.subsystem.sftp.SftpSubsystem subsystem =
                new SftpSubsystem(getExecutorService(), isShutdownOnExit(), getUnsupportedAttributePolicy(), getFileSystemAccessor());
        GenericUtils.forEach(getRegisteredListeners(), subsystem::addSftpEventListener);
        return subsystem;
    }

    public static class Builder extends AbstractSftpEventListenerManager implements ObjectBuilder<SftpSubsystemFactory> {
        private ExecutorService executors;
        private boolean shutdownExecutor;
        private UnsupportedAttributePolicy policy = DEFAULT_POLICY;
        private SftpFileSystemAccessor fileSystemAccessor = SftpFileSystemAccessor.DEFAULT;

        public Builder() {
            super();
        }

        public Builder withExecutorService(ExecutorService service) {
            executors = service;
            return this;
        }

        public Builder withShutdownOnExit(boolean shutdown) {
            shutdownExecutor = shutdown;
            return this;
        }

        public Builder withUnsupportedAttributePolicy(UnsupportedAttributePolicy p) {
            policy = Objects.requireNonNull(p, "No policy");
            return this;
        }

        public Builder withFileSystemAccessor(SftpFileSystemAccessor accessor) {
            fileSystemAccessor = Objects.requireNonNull(accessor, "No accessor");
            return this;
        }

        @Override
        public SftpSubsystemFactory build() {
            SftpSubsystemFactory factory = new SftpSubsystemFactory();
            factory.setExecutorService(executors);
            factory.setShutdownOnExit(shutdownExecutor);
            factory.setUnsupportedAttributePolicy(policy);
            factory.setFileSystemAccessor(fileSystemAccessor);
            GenericUtils.forEach(getRegisteredListeners(), factory::addSftpEventListener);
            return factory;
        }
    }
}
