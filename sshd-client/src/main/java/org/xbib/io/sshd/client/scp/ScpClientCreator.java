package org.xbib.io.sshd.client.scp;

import org.xbib.io.sshd.common.scp.ScpFileOpener;
import org.xbib.io.sshd.common.scp.ScpFileOpenerHolder;
import org.xbib.io.sshd.common.scp.ScpTransferEventListener;

/**
 */
public interface ScpClientCreator extends ScpFileOpenerHolder {
    /**
     * Create an SCP client from this session.
     *
     * @return An {@link ScpClient} instance. <B>Note:</B> uses the currently
     * registered {@link ScpTransferEventListener} and {@link ScpFileOpener} if any
     * @see #setScpFileOpener(ScpFileOpener)
     * @see #setScpTransferEventListener(ScpTransferEventListener)
     */
    default ScpClient createScpClient() {
        return createScpClient(getScpFileOpener(), getScpTransferEventListener());
    }

    /**
     * Create an SCP client from this session.
     *
     * @param listener A {@link ScpTransferEventListener} that can be used
     *                 to receive information about the SCP operations - may be {@code null}
     *                 to indicate no more events are required. <B>Note:</B> this listener
     *                 is used <U>instead</U> of any listener set via {@link #setScpTransferEventListener(ScpTransferEventListener)}
     * @return An {@link ScpClient} instance
     */
    default ScpClient createScpClient(ScpTransferEventListener listener) {
        return createScpClient(getScpFileOpener(), listener);
    }

    /**
     * Create an SCP client from this session.
     *
     * @param opener The {@link ScpFileOpener} to use to control how local files
     *               are read/written. If {@code null} then a default opener is used.
     *               <B>Note:</B> this opener is used <U>instead</U> of any instance
     *               set via {@link #setScpFileOpener(ScpFileOpener)}
     * @return An {@link ScpClient} instance
     */
    default ScpClient createScpClient(ScpFileOpener opener) {
        return createScpClient(opener, getScpTransferEventListener());
    }

    /**
     * Create an SCP client from this session.
     *
     * @param opener   The {@link ScpFileOpener} to use to control how local files
     *                 are read/written. If {@code null} then a default opener is used.
     *                 <B>Note:</B> this opener is used <U>instead</U> of any instance
     *                 set via {@link #setScpFileOpener(ScpFileOpener)}
     * @param listener A {@link ScpTransferEventListener} that can be used
     *                 to receive information about the SCP operations - may be {@code null}
     *                 to indicate no more events are required. <B>Note:</B> this listener
     *                 is used <U>instead</U> of any listener set via {@link #setScpTransferEventListener(ScpTransferEventListener)}
     * @return An {@link ScpClient} instance
     */
    ScpClient createScpClient(ScpFileOpener opener, ScpTransferEventListener listener);

    /**
     * @return The last {@link ScpTransferEventListener} set via
     * {@link #setScpTransferEventListener(ScpTransferEventListener)}
     */
    ScpTransferEventListener getScpTransferEventListener();

    /**
     * @param listener A default {@link ScpTransferEventListener} that can be used
     *                 to receive information about the SCP operations - may be {@code null}
     *                 to indicate no more events are required
     * @see #createScpClient(ScpTransferEventListener)
     */
    void setScpTransferEventListener(ScpTransferEventListener listener);
}
