import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;

import bin.arsc.ArscFile;
import bin.zip.ZipEntry;
import bin.zip.ZipFile;
import bin.zip.ZipOutputStream;

public class ResProguard {
	
	public static void main(String[] args) {
//		proguard(new File("C:\\Users\\Bin\\Desktop\\com.chaoxing.fanya.aphone-2.apk"));
		for (String s : args)
			proguard(new File(s));
	}
	
	private static void proguard(File file) {
        ZipFile zipFile = null;
        ZipOutputStream zos = null;
        File outFile = null;
        File mapping = null;
        FileOutputStream fos = null;
        try {
            zipFile = new ZipFile(file);
            HashMap<String, String> map = new HashMap<>();
            Enumeration<ZipEntry> entryEnumeration = zipFile.getEntries();
            while (entryEnumeration.hasMoreElements()) {
                ZipEntry z = entryEnumeration.nextElement();
                if (z.isDirectory())
                    continue;
                if(z.getName().startsWith("res/"))
                    map.put(z.getName(), z.getName());
            }
            //读取resources.arsc数据
            ZipEntry arsc = zipFile.getEntry("resources.arsc");
            if (arsc == null || arsc.isDirectory())
                throw new IOException("resources.arsc not found");
            byte[] data = new byte[(int) arsc.getSize()];
            InputStream is = zipFile.getInputStream(arsc);
            int start = 0;
            int len;
            while (start < data.length &&(len = is.read(data, start, data.length - start)) > 0)
                start += len;
            if (start != data.length)
                throw new IOException("Read resources.arsc error");
            ArscFile arscFile = ArscFile.decodeArsc(new ByteArrayInputStream(data));
            //重命名res内文件
            Name n = new Name();
            for (int i = 0; i < arscFile.getStringSize(); i++) {
                String s = arscFile.getString(i);
                if (s.startsWith("res/") && map.containsKey(s)) {
                    String newName = "r/" + n.getName();
                    //必须保留文件后缀，否则出错
                    if (s.toLowerCase().endsWith(".9.png"))
                        newName += ".9.png";
                    else {
                        int idx = s.lastIndexOf('.');
                        if(idx != -1)
                            newName += s.substring(idx);
                    }
                    n.next();
                    arscFile.setString(i, newName);
                    map.put(s, newName);
                }
            }
            //写出压缩包
            String name = file.getName();
            int i = name.lastIndexOf('.');
            if (i != -1) {
                mapping = new File(file.getParentFile(), name.substring(0, i) + "_mapping.txt");
                name = name.substring(0, i) + "_r" + name.substring(i);
            } else {
                mapping = new File(file.getParentFile(), name + "_mapping.txt");
                name += "_r";
            }
            outFile = new File(file.getParentFile(), name);

            zos = new ZipOutputStream(outFile);
            //resources.arsc最好不要进行压缩
            zos.setMethod(ZipOutputStream.STORED);
            zos.putNextEntry("resources.arsc");
            zos.write(ArscFile.encodeArsc(arscFile));

            entryEnumeration = zipFile.getEntries();
            fos = new FileOutputStream(mapping);
            while (entryEnumeration.hasMoreElements()) {
                ZipEntry z = entryEnumeration.nextElement();
                //文件夹不需要写出
                if (z.isDirectory() || z.getName().equals("resources.arsc") || z.getName().startsWith("META-INF/"))
                    continue;
                if (map.containsKey(z.getName())) {
                	String na = map.get(z.getName());
                	fos.write((z.getName() + " -> " + na + "\r\n").getBytes());
                    z.setName(na);
                }
                
                //复制原始压缩数据，无需解压再压缩
                zos.copyZipEntry(z, zipFile);
            }
            fos.close();
            zos.close();
            zipFile.close();
        } catch (Throwable e) {
            e.printStackTrace();
            try {
            	if (zipFile != null)
                    zipFile.close();
            } catch (IOException ioe){}
            try {
                if (zos != null)
                    zos.close();
            } catch (IOException ioe){}
            try {
                if (fos != null)
                	fos.close();
            } catch (IOException ioe){}
            if (outFile != null && outFile.exists())
                outFile.delete();
            if (mapping != null && mapping.exists())
            	mapping.delete();
        }
    }

    private static class Name {
        Name parent = null;
        char c = '0';

        String getName() {
            return parent == null ? String.valueOf(c) : parent.getName() + c;
        }
        void next(){
            if (c == '9')
                c = 'A';
            else if (c == 'Z')
                c = 'a';
            else if(c == 'z') {
                c = '0';
                if (parent == null)
                    parent = new Name();
                else
                    parent.next();
            } else
                c++;
        }
    }
}
