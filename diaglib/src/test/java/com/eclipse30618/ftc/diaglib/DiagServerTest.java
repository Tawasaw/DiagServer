package com.eclipse30618.ftc.diaglib;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.Test;

public class DiagServerTest {
    @Test
    public void closeReleasesListeningPort() throws Exception {
        int port = freePort();
        FieldCatalog catalog = new FieldCatalog();
        catalog.add("battery_voltage", "double", "V");

        DiagServer server = new DiagServer(port, catalog);
        server.start();
        waitForServer(port);

        server.close();
        waitForPortReleased(port);

        try (ServerSocket rebound = new ServerSocket()) {
            rebound.setReuseAddress(true);
            rebound.bind(new InetSocketAddress("127.0.0.1", port));
        }
    }

    @Test
    public void protocolCanListFieldsAndStreamSanitizedData() throws Exception {
        int port = freePort();
        FieldCatalog catalog = new FieldCatalog();
        catalog.add("status", "string", "");
        DiagServer server = new DiagServer(port, catalog);
        server.start();
        waitForServer(port);

        try (Socket socket = new Socket("127.0.0.1", port);
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer =
                        new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            socket.setSoTimeout(2000);
            writer.write("HOWDY\n");
            writer.flush();
            String response = reader.readLine();
            org.junit.Assert.assertTrue(response.startsWith("DESCRIBE "));
            org.json.JSONObject descriptor = new org.json.JSONObject(response.substring(9));
            assertEquals(2, descriptor.getInt("protocol_version"));
            assertEquals("status", descriptor.getJSONArray("fields").getJSONObject(0).getString("name"));
            assertEquals(0, descriptor.getJSONArray("controls").length());
            assertEquals(0, descriptor.getJSONArray("cameras").length());

            writer.write("FIELDS\n");
            writer.flush();
            assertEquals("FIELDS status,string,", reader.readLine());

            // Install the sample before subscribing: a subscribed client may
            // receive the latest snapshot immediately after the OK response.
            DiagSnapshotBuilder builder = new DiagSnapshotBuilder(1);
            builder.set(0, "a,b\nc");
            server.setSnapshot(builder.build());

            writer.write("SUB ALL rate=100\n");
            writer.flush();
            assertEquals("OK", reader.readLine());

            assertEquals("DATA a;b c", reader.readLine());
        } finally {
            server.close();
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket server = new ServerSocket(0)) {
            return server.getLocalPort();
        }
    }

    private static void waitForServer(int port) throws Exception {
        long deadline = System.currentTimeMillis() + 2000;
        IOException last = null;
        while (System.currentTimeMillis() < deadline) {
            try (Socket ignored = new Socket("127.0.0.1", port)) {
                return;
            } catch (IOException e) {
                last = e;
                Thread.sleep(10);
            }
        }
        throw last;
    }

    private static void waitForPortReleased(int port) throws Exception {
        long deadline = System.currentTimeMillis() + 2000;
        IOException last = null;
        while (System.currentTimeMillis() < deadline) {
            try (ServerSocket server = new ServerSocket()) {
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress("127.0.0.1", port));
                return;
            } catch (IOException e) {
                last = e;
                Thread.sleep(10);
            }
        }
        throw last;
    }
}
