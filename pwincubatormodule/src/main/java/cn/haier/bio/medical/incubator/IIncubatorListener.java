package cn.haier.bio.medical.incubator;

public interface IIncubatorListener {
    void onIncubatorConnected();
    void onIncubatorException();
    void onIncubatorSwitchReadModel();
    void onIncubatorSwitchWriteModel();
    void onIncubatorPackageReceived(byte[] message);
}
