package bin.arsc;

import java.io.IOException;

import bin.io.ZInput;

public class Header {
    public final short type;
    public final int chunkSize;

    public Header(short type, int size) {
        this.type = type;
        this.chunkSize = size;
    }

    public static Header read(ZInput in) throws IOException {
        short type;
        try {
            type = in.readShort();
        } catch (IOException ex) {
            return new Header(TYPE_NONE, 0);
        }
        in.skipBytes(2);
        return new Header(type, in.readInt());
    }

    public final static short TYPE_NONE = -1, TYPE_TABLE = 0x0002,
            TYPE_PACKAGE = 0x0200, TYPE_TYPE = 0x0202, TYPE_LIBRARY = 0x0203,
            TYPE_CONFIG = 0x0201;
}
