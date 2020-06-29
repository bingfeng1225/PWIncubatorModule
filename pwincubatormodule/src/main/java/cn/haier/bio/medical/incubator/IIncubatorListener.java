package cn.haier.bio.medical.incubator;

public interface IIncubatorListener {
    void onIncubatorConnected();
    void onIncubatorSwitchReadModel();
    void onIncubatorSwitchWriteModel();
    void onIncubatorPrint(String message);
    void onIncubatorException(Throwable throwable);
    void onIncubatorPackageReceived(byte[] message);
}
