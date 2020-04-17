package cn.haier.bio.medical.incubator;

import cn.qd.peiwen.pwtools.EmptyUtils;

/***
 * 超低温变频、T系列、双系统主控板通讯
 *
 */
public class IncubatorManager {
    private IncubatorSerialPort serialPort;
    private static IncubatorManager manager;

    public static IncubatorManager getInstance() {
        if (manager == null) {
            synchronized (IncubatorManager.class) {
                if (manager == null)
                    manager = new IncubatorManager();
            }
        }
        return manager;
    }

    private IncubatorManager() {

    }

    public void init(String path) {
        if (EmptyUtils.isEmpty(this.serialPort)) {
            this.serialPort = new IncubatorSerialPort();
            this.serialPort.init(path);
        }
    }

    public void enable() {
        if (EmptyUtils.isNotEmpty(this.serialPort)) {
            this.serialPort.enable();
        }
    }

    public void disable() {
        if (EmptyUtils.isNotEmpty(this.serialPort)) {
            this.serialPort.disable();
        }
    }

    public void release() {
        if (EmptyUtils.isNotEmpty(this.serialPort)) {
            this.serialPort.release();
            this.serialPort = null;
        }
    }

    public void sendData(byte[] data) {
        if (EmptyUtils.isNotEmpty(this.serialPort)) {
            this.serialPort.sendData(data);
        }
    }

    public void changeListener(IIncubatorListener listener) {
        if (EmptyUtils.isNotEmpty(this.serialPort)) {
            this.serialPort.changeListener(listener);
        }
    }
}

