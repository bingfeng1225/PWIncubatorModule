package cn.haier.bio.medical.incubator;


import io.netty.buffer.ByteBuf;

class IncubatorTools {
    public static final byte[] HEADER = {(byte)0xAA,(byte)0x55};

    public static boolean checkHeader(byte[] header) {
        for (int i = 0; i < HEADER.length; i++) {
            if(HEADER[i] != header[i]){
                return false;
            }
        }
        return true;
    }

    public static String bytes2HexString(byte[] data) {
        return bytes2HexString(data, false);
    }

    public static String bytes2HexString(byte[] data, boolean hexFlag) {
        return bytes2HexString(data, hexFlag, null);
    }

    public static String bytes2HexString(byte[] data, boolean hexFlag, String separator) {
        if (data == null) {
            throw new IllegalArgumentException("The data can not be blank");
        }
        return bytes2HexString(data, 0, data.length, hexFlag, separator);
    }

    public static String bytes2HexString(byte[] data, int offset, int len) {
        return bytes2HexString(data, offset, len, false);
    }

    public static String bytes2HexString(byte[] data, int offset, int len, boolean hexFlag) {
        return bytes2HexString(data, offset, len, hexFlag, null);
    }

    public static String bytes2HexString(byte[] data, int offset, int len, boolean hexFlag, String separator) {
        if (data == null) {
            throw new IllegalArgumentException("The data can not be blank");
        }
        if (offset < 0 || offset > data.length - 1) {
            throw new IllegalArgumentException("The offset index out of bounds");
        }
        if (len < 0 || offset + len > data.length) {
            throw new IllegalArgumentException("The len can not be < 0 or (offset + len) index out of bounds");
        }
        String format = "%02X";
        if (hexFlag) {
            format = "0x%02X";
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = offset; i < offset + len; i++) {
            buffer.append(String.format(format, data[i]));
            if (separator == null) {
                continue;
            }
            if (i != (offset + len - 1)) {
                buffer.append(separator);
            }
        }
        return buffer.toString();
    }

    public static int indexOf(ByteBuf haystack, byte[] needle) {
        //遍历haystack的每一个字节
        for (int i = haystack.readerIndex(); i < haystack.writerIndex(); i++) {
            int needleIndex;
            int haystackIndex = i;
            /*haystack是否出现了delimiter，注意delimiter是一个ChannelBuffer（byte[]）
            例如对于haystack="ABC\r\nDEF"，needle="\r\n"
            那么当haystackIndex=3时，找到了“\r”，此时needleIndex=0
            继续执行循环，haystackIndex++，needleIndex++，
            找到了“\n”
            至此，整个needle都匹配到了
            程序然后执行到if (needleIndex == needle.capacity())，返回结果
            */
            for (needleIndex = 0; needleIndex < needle.length; needleIndex++) {
                if (haystack.getByte(haystackIndex) != needle[needleIndex]) {
                    break;
                } else {
                    haystackIndex++;
                    if (haystackIndex == haystack.writerIndex() && needleIndex != needle.length - 1) {
                        return -1;
                    }
                }
            }

            if (needleIndex == needle.length) {
                // Found the needle from the haystack!
                return i - haystack.readerIndex();
            }
        }
        return -1;
    }
}
