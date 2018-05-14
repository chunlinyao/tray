package qz.ws;

import jssc.SerialPort;
import jssc.SerialPortException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerialProxyServer {
    private final String portName;
    private final String tcpPort;
    private final ExecutorService executorService;

    public SerialProxyServer(String portName, String tcpPort) {
        this.portName = portName;
        this.tcpPort = tcpPort;
        executorService = Executors.newFixedThreadPool(5);
    }

    public void start() {
        executorService.execute(getServerRunable());
    }

    private Runnable getServerRunable() {
        return new Runnable() {

            @Override
            public void run() {
                try(ServerSocket sc = new ServerSocket(Integer.valueOf(tcpPort))) {

                    while(true) {
                        Socket client = sc.accept();
                        executorService.execute(getWorkerRunnable(client));
                    }
                }
                catch(IOException e) {
                    executorService.shutdown();
                }
            }
        };
    }

    private Runnable getWorkerRunnable(final Socket client) {
        return new Runnable() {

            @Override
            public void run() {

                SerialPort serial = null;
                try(InputStream in =
                        client.getInputStream();
                    ) {
                    byte[] buffer = new byte[4096];
                    ByteArrayOutputStream output = new ByteArrayOutputStream(40960);
                    int n;
                    while((n = in.read(buffer)) != -1) {
                        output.write(buffer, 0, n);
                    }

                    serial = new SerialPort(portName);
                    int retryCount = 5;
                    while(retryCount > 0) {
                        try {
                            if (serial.openPort()) {
                                serial.setParams(9600, 8, 1, 0);
                                serial.writeBytes((output).toByteArray());
                            }
                        }
                        catch(SerialPortException e) {
                            if (e.getExceptionType() == SerialPortException.TYPE_PORT_BUSY) {
                                try {
                                    Thread.sleep(250);
                                }
                                catch(InterruptedException e1) {
                                    if (Thread.interrupted()) {
                                        Thread.currentThread().interrupt();
                                    }
                                }
                                retryCount--;
                            } else {
                                retryCount = 0;
                            }
                        }
                    }
                }
                catch(IOException e) {
                    e.printStackTrace();
                } finally {
                    if(serial != null && serial.isOpened()) {
                        try {
                            serial.closePort();
                        }
                        catch(SerialPortException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    public void stop() {
        executorService.shutdownNow();
    }
}
