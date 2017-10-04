package org.xbib.io.sshd.common.scp;

/**
 *
 */
public interface ScpFileOpenerHolder {
    /**
     * @return The last {@link ScpFileOpener} set via call
     * to {@link #setScpFileOpener(ScpFileOpener)}
     */
    ScpFileOpener getScpFileOpener();

    /**
     * @param opener The default {@link ScpFileOpener} to use - if {@code null}
     *               then a default opener is used
     */
    void setScpFileOpener(ScpFileOpener opener);
}
