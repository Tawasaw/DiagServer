package com.eclipse30618.ftc.diaglib;

/**
 * Helper that starts/stops Limelight TCP proxies with one call.
 */
public final class LimelightTunnelManager implements AutoCloseable {
    private static final String DEFAULT_USB_IP = "172.29.0.1";
    private static final String DEFAULT_BIND_HOST = "0.0.0.0";
    private static final int DATA_PORT = 5800;
    private static final int RAW_MJPEG_PORT = 5802;
    private static final int MJPEG_PORT = 5801;
    private static final int WEBSOCKET_PORT = 5805;
    private static final int API_PORT = 5807;

    private final LimelightTunnel mjpegTunnel;
    private final LimelightTunnel dataTunnel;
    private final LimelightTunnel rawMjpegTunnel;
    private final LimelightTunnel websocketTunnel;
    private final LimelightTunnel apiTunnel;

    /**
     * Build a manager with already-constructed tunnels.
     */
    private LimelightTunnelManager(
            LimelightTunnel mjpegTunnel,
            LimelightTunnel dataTunnel,
            LimelightTunnel rawMjpegTunnel,
            LimelightTunnel websocketTunnel,
            LimelightTunnel apiTunnel) {
        this.mjpegTunnel = mjpegTunnel;
        this.dataTunnel = dataTunnel;
        this.rawMjpegTunnel = rawMjpegTunnel;
        this.websocketTunnel = websocketTunnel;
        this.apiTunnel = apiTunnel;
    }

    /**
     * Create a manager wired to the default Limelight USB IP and ports.
     */
    public static LimelightTunnelManager createDefault() {
        return new LimelightTunnelManager(
                new LimelightTunnel(DEFAULT_BIND_HOST, MJPEG_PORT, DEFAULT_USB_IP, MJPEG_PORT),
                new LimelightTunnel(DEFAULT_BIND_HOST, DATA_PORT, DEFAULT_USB_IP, DATA_PORT),
                new LimelightTunnel(DEFAULT_BIND_HOST, RAW_MJPEG_PORT, DEFAULT_USB_IP, RAW_MJPEG_PORT),
                new LimelightTunnel(
                        DEFAULT_BIND_HOST, WEBSOCKET_PORT, DEFAULT_USB_IP, WEBSOCKET_PORT),
                new LimelightTunnel(DEFAULT_BIND_HOST, API_PORT, DEFAULT_USB_IP, API_PORT));
    }

    /**
     * Start all tunnels (GUI, annotated video, raw video, websocket, and API).
     */
    public void start() {
        try {
            mjpegTunnel.start();
            dataTunnel.start();
            rawMjpegTunnel.start();
            websocketTunnel.start();
            apiTunnel.start();
        } catch (Exception ignored) {
        }
    }

    /**
     * Stop all tunnels.
     */
    @Override
    public void close() {
        mjpegTunnel.close();
        dataTunnel.close();
        rawMjpegTunnel.close();
        websocketTunnel.close();
        apiTunnel.close();
    }
}
