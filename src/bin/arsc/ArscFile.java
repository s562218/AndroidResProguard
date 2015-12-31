package bin.arsc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import bin.io.ZInput;
import bin.io.ZOutput;

public class ArscFile {
    private short typeTableNext;

//	private static final short CHECK_PACKAGE = 0x0200;

    private ZInput mIn;
    private int packageCount;
    private StringDecoder mTableStrings;
    private ArrayList<String> constantPool = new ArrayList<>();
    private byte[] PackageBytes;
    private Header mHeader;


    public static ArscFile decodeArsc(InputStream in) throws IOException {
        ArscFile arsc = new ArscFile(new ZInput(in));
        arsc.readTable();
        return arsc;
    }

    public static byte[] encodeArsc(ArscFile arsc) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(new ZOutput(baos), arsc);
        byte[] b = baos.toByteArray();
        baos.close();
        return b;
    }

    private static void write(ZOutput out, ArscFile arsc) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZOutput buf = new ZOutput(baos);
        buf.writeInt(arsc.packageCount);
        arsc.mTableStrings.write(list2Strings(arsc.constantPool),
                buf);
        buf.writeFully(arsc.PackageBytes);
        out.writeShort(Header.TYPE_TABLE);
        out.writeShort(arsc.typeTableNext);
        out.writeInt(baos.size() + 8);
        out.writeFully(baos.toByteArray());
        baos.close();
    }


    public String getString(int index) {
        return constantPool.get(index);
    }

    public int getStringSize() {
        return constantPool.size();
    }

    public void setString(int index, String s) {
        constantPool.set(index, s);
    }

    private ArscFile(ZInput in) {
        mIn = in;
    }

    private void readTable() throws IOException {
        //readHeader
        short type = mIn.readShort();
        checkChunk(type, Header.TYPE_TABLE);
        typeTableNext = mIn.readShort();
        mIn.readInt();// chunk size

        packageCount = mIn.readInt();

        mTableStrings = StringDecoder.read(this.mIn);

        int size = mTableStrings.getSize();

        for (int i = 0; i < size; i++) {
            constantPool.add(mTableStrings.getString(i));
//			Log.i("String", mTableStrings.getString(i));
        }
//        value = new ArscValue[constantPool.size()];

        readPackageBytes();
    }

    private void readPackageBytes() throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int num;
        while ((num = mIn.read(buf, 0, 2048)) != -1)
            byteOut.write(buf, 0, num);
        mIn.close();
        PackageBytes = byteOut.toByteArray();
        byteOut.close();
    }

    Header nextChunk() throws IOException {
        return mHeader = Header.read(mIn);
    }

    void checkChunkType(int expectedType) throws IOException {
        if (mHeader.type != expectedType) {
            throw new IOException(String.format("Invalid chunk type: expected=0x%08x, got=0x%08x",
                    expectedType, mHeader.type));
        }
    }

    void nextChunkCheckType(int expectedType) throws IOException {
        nextChunk();
        checkChunkType(expectedType);
    }

    private void checkChunk(short type, short expectedType) throws IOException {
        if (type != expectedType)
            throw new IOException(String.format(
                    "Invalid chunk type: expected=0x%08x, got=0x%08x",
                    new Object[]{expectedType, type}));
    }

    private static String[] list2Strings(ArrayList<String> s) {
        String[] ss = new String[s.size()];
        return s.toArray(ss);
    }

}
