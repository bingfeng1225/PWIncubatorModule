package cn.haier.bio.medical.incubator;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;

import cn.qd.peiwen.serialport.PWSerialPortHelper;
import cn.qd.peiwen.serialport.PWSerialPortListener;
import cn.qd.peiwen.serialport.PWSerialPortState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class IncubatorSerialPort implements PWSerialPortListener {
    private ByteBuf buffer;
    private HandlerThread thread;
    private IncubatorHandler handler;
    private PWSerialPortHelper helper;

    private boolean enabled = false;
    private WeakReference<IIncubatorListener> listener;

    public IncubatorSerialPort() {
    }

    public void init(String path) {
        this.createHandler();
        this.createHelper(path);
        this.createBuffer();
    }

    public void enable() {
        if (this.isInitialized() && !this.enabled) {
            this.enabled = true;
            this.helper.open();
        }
    }

    public void disable() {
        if (this.isInitialized() && this.enabled) {
            this.enabled = false;
            this.helper.close();
        }
    }

    public void release() {
        this.listener = null;
        this.destoryHandler();
        this.destoryHelper();
        this.destoryBuffer();
    }

    public void sendData(byte[] data) {
        if (this.isInitialized() && this.enabled) {
            Message msg = Message.obtain();
            msg.what = 0;
            msg.obj = data;
            this.handler.sendMessage(msg);
        }
    }

    public void changeListener(IIncubatorListener listener) {
        this.listener = new WeakReference<>(listener);
    }

    private boolean isInitialized() {
        if (this.handler == null) {
            return false;
        }
        if (this.helper == null) {
            return false;
        }
        if (this.buffer == null) {
            return false;
        }
        return true;
    }

    private void createHelper(String path) {
        if (this.helper == null) {
            this.helper = new PWSerialPortHelper("IncubatorSerialPort");
            this.helper.setTimeout(9);
            this.helper.setPath(path);
            this.helper.setBaudrate(9600);
            this.helper.init(this);
        }
    }

    private void destoryHelper() {
        if (null != this.helper) {
            this.helper.release();
            this.helper = null;
        }
    }

    private void createHandler() {
        if (this.thread == null && this.handler == null) {
            this.thread = new HandlerThread("IncubatorSerialPort");
            this.thread.start();
            this.handler = new IncubatorHandler(this.thread.getLooper());
        }
    }

    private void destoryHandler() {
        if (null != this.thread) {
            this.thread.quitSafely();
            this.thread = null;
            this.handler = null;
        }
    }

    private void createBuffer() {
        if (this.buffer == null) {
            this.buffer = Unpooled.buffer(4);
        }
    }

    private void destoryBuffer() {
        if (null != this.buffer) {
            this.buffer.release();
            this.buffer = null;
        }
    }

    private void write(byte[] data) {
        if (!this.isInitialized() || !this.enabled) {
            return;
        }
        this.helper.writeAndFlush(data);
        IncubatorSerialPort.this.switchReadModel();
        this.loggerPrint("IncubatorSerialPort Send:" + IncubatorTools.bytes2HexString(data, true, ", "));
    }

    public void switchReadModel() {
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onIncubatorSwitchReadModel();
        }
    }

    public void switchWriteModel() {
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onIncubatorSwitchWriteModel();
        }
    }

    private void loggerPrint(String message){
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onIncubatorPrint(message);
        }
    }

    private boolean ignorePackage() {
        boolean result = false;
        int index = IncubatorTools.indexOf(this.buffer, IncubatorTools.HEADER);
        if (index != -1) {
            result = true;
            byte[] data = new byte[index];
            this.buffer.readBytes(data, 0, data.length);
            this.buffer.discardReadBytes();
            this.loggerPrint("IncubatorSerialPort 指令丢弃:" + IncubatorTools.bytes2HexString(data, true, ", "));
        }
        return result;
    }


    @Override
    public void onConnected(PWSerialPortHelper helper) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        this.buffer.clear();
        this.switchWriteModel();
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onIncubatorConnected();
        }
    }

    @Override
    public void onReadThreadReleased(PWSerialPortHelper helper) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onIncubatorPrint("IncubatorSerialPort read thread released");
        }
    }

    @Override
    public void onException(PWSerialPortHelper helper, Throwable throwable) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onIncubatorException(throwable);
        }
    }

    @Override
    public void onStateChanged(PWSerialPortHelper helper, PWSerialPortState state) {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        if (null != this.listener && null != this.listener.get()) {
            this.listener.get().onIncubatorPrint("IncubatorSerialPort state changed: " + state.name());
        }
    }

    @Override
    public void onByteReceived(PWSerialPortHelper helper, byte[] buffer, int length) throws IOException {
        if (!this.isInitialized() || !helper.equals(this.helper)) {
            return;
        }
        this.buffer.writeBytes(buffer, 0, length);

        while (this.buffer.readableBytes() >= 3) {
            byte[] header = new byte[IncubatorTools.HEADER.length];
            this.buffer.getBytes(0, header);

            if (!IncubatorTools.checkHeader(header)) {
                if (this.ignorePackage()) {
                    continue;
                } else {
                    break;
                }
            }
            int lenth = 0xFF & this.buffer.getByte(2);

            if (this.buffer.readableBytes() < lenth + 3) {
                break;
            }

            byte[] data = new byte[lenth + 3];
            this.buffer.readBytes(data, 0, data.length);
            this.buffer.discardReadBytes();
            this.loggerPrint("IncubatorSerialPort Recv:" + IncubatorTools.bytes2HexString(data, true, ", "));
            this.switchWriteModel();
            if (null != this.listener && null != this.listener.get()) {
                this.listener.get().onIncubatorPackageReceived(data);
            }
        }
    }

    private class IncubatorHandler extends Handler {
        public IncubatorHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0: {
                    byte[] message = (byte[]) msg.obj;
                    if (null != message && message.length > 0) {
                        IncubatorSerialPort.this.write(message);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }
}
