package qz.ws;

import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.auth.Certificate;
import qz.communication.*;
import qz.printer.status.StatusSession;
import qz.printer.status.StatusMonitor;
import qz.utils.FileWatcher;
import qz.utils.ImeUtilities;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

public class SocketConnection {

    private static final Logger log = LoggerFactory.getLogger(SocketConnection.class);


    private Certificate certificate;

    private DeviceListener deviceListener;
    private StatusSession statusListener;

    // serial port -> open SerialIO
    private final HashMap<String,SerialIO> openSerialPorts = new HashMap<>();

    // absolute path -> open file listener
    private final HashMap<Path,FileIO> openFiles = new HashMap<>();

    // DeviceOptions -> open DeviceIO
    private final HashMap<DeviceOptions,DeviceIO> openDevices = new HashMap<>();
    private ImeState savedImeState;


    public SocketConnection(Certificate cert) {
        certificate = cert;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate newCert) {
        certificate = newCert;
    }


    public void addSerialPort(String port, SerialIO io) {
        openSerialPorts.put(port, io);
    }

    public SerialIO getSerialPort(String port) {
        return openSerialPorts.get(port);
    }

    public void removeSerialPort(String port) {
        openSerialPorts.remove(port);
    }


    public boolean isDeviceListening() {
        return deviceListener != null;
    }

    public void startDeviceListening(DeviceListener listener) {
        deviceListener = listener;
    }

    public void stopDeviceListening() {
        if (deviceListener != null) {
            deviceListener.close();
        }
        deviceListener = null;
    }

    public synchronized boolean hasStatusListener() {
        return statusListener != null;
    }

    public synchronized void startStatusListener(StatusSession listener) {
        statusListener = listener;
    }

    public synchronized void stopStatusListener() {
        StatusMonitor.closeListener(this);
        statusListener = null;
    }

    public synchronized StatusSession getStatusListener() {
        return statusListener;
    }


    public void addFileListener(Path absolute, FileIO listener) {
        openFiles.put(absolute, listener);
    }

    public FileIO getFileListener(Path absolute) {
        return openFiles.get(absolute);
    }

    public void removeFileListener(Path absolute) {
        openFiles.remove(absolute);
    }

    public void removeAllFileListeners() {
        for(Path path : openFiles.keySet()) {
            openFiles.get(path).close();
            FileWatcher.deregisterWatch(openFiles.get(path));
        }

        openFiles.clear();
    }


    public void addDevice(DeviceOptions dOpts, DeviceIO io) {
        openDevices.put(dOpts, io);
    }

    public DeviceIO getDevice(DeviceOptions dOpts) {
        return openDevices.get(dOpts);
    }

    public void removeDevice(DeviceOptions dOpts) {
        openDevices.remove(dOpts);
    }

    public synchronized void openDevice(DeviceIO device, DeviceOptions dOpts) throws DeviceException {
        device.open();
        addDevice(dOpts, device);
    }

    /**
     * Explicitly closes all open serial and usb connections setup through this object
     */
    public synchronized void disconnect() throws SerialPortException, DeviceException, IOException {
        log.info("Closing all communication channels for {}", certificate.getCommonName());

        for(SerialIO sio : openSerialPorts.values()) {
            sio.close();
        }

        for(DeviceIO dio : openDevices.values()) {
            dio.setStreaming(false);
            dio.close();
        }

        removeAllFileListeners();
        stopDeviceListening();
        stopStatusListener();
    }

    static class ImeState {
        boolean capsLock;
        boolean active;
    }
    public synchronized void restoreImeState() {
        if (savedImeState != null) {
            ImeUtilities.setCapsLock(savedImeState.capsLock);
            ImeUtilities.setImeOpen(savedImeState.active);
            savedImeState = null;
        }
    }

    public void capsUnlock() {
        ImeUtilities.setCapsLock(false);
    }

    public void imeInactive() {
        ImeUtilities.setImeOpen(false);
    }

    public void imeActive() {
        ImeUtilities.setImeOpen(true);
    }

    public synchronized void saveImeState() {
        if (savedImeState == null) {
            savedImeState = new ImeState();
            savedImeState.capsLock = ImeUtilities.getCapsLock();
            savedImeState.active = ImeUtilities.getImeOpen();
        }
    }

    public void capsLock() {
        ImeUtilities.setCapsLock(true);
    }
}
