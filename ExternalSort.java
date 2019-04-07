// -- In the name of Allah -- \\

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class AAA {

    static Scanner s = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        try {
        System.out.println("Please enter file address:");
        String inputFileName = s.nextLine();
        System.out.println("Sorting started.\nCreating chunks...");
        Sorter sorter = new Sorter(inputFileName);
        sorter.sort();
        } catch(Exception e) {
            System.out.println("An Error occured please re try!!! ");
        }
    }
}

class Sorter {

    private final String workFolder, inputFileName, loc, output;
    private int fileNumber = 1;
    private final int chunkSize = 10_000_000;
    private double lastTime;
    private ArrayList<BufferedReader> readers = new ArrayList<>();

    public Sorter(String inputFileAddress) {
        this.inputFileName = inputFileAddress;
        workFolder = inputFileName.substring(0, inputFileName.lastIndexOf("\\") == -1 ? inputFileName.length() :  inputFileName.lastIndexOf("\\") ) + "\\";
        createDirectories();
        this.loc = workFolder + "ExternalSortTempFolder\\";
        this.output = workFolder + "ExternalSortOutputFolder\\out.txt" ;
    }

    private void createDirectories() {
        new File(workFolder + "ExternalSortTempFolder\\").mkdir();
        new File(workFolder + "ExternalSortOutputFolder\\").mkdir();
    }

    public void sort() throws Exception {
        double startTime = System.currentTimeMillis();
        lastTime = startTime;
        createChunks();
        System.out.println(getTimeOf("Chunks were created in "));
        merge();
        System.out.println(getTimeOf("Chunks were merged in "));
        double cur = System.currentTimeMillis();
        System.out.println(String.format("File Sorted Seccessfully in %.3f seconds.\nDeleting temp files...", (cur - startTime) / 1000));
        System.out.println(deleteTempFiles() ? "temp files were deleted successfully." : "temp files were not deleted successfully");
        long inputSize = new File(inputFileName).length();
        long outputFileNameSize = new File(output).length();
        System.out.println("input file size: " + inputSize + " bytes");
        System.out.println("output file size: " + outputFileNameSize + " bytes");
    }

    public boolean deleteTempFiles() {
        boolean res = true;
        File[] temps = new File(loc).listFiles();
        for (File temp : temps) {
            res &= temp.delete();
        }
        return res && new File(loc).delete();
    }

    private Long pop(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        return Long.parseLong(line);
    }

    public void createChunks() throws IOException {
        // -- reading from original input file --
        BufferedReader reader = new BufferedReader(new FileReader(inputFileName));
        boolean fileIsFinished = false;
        outer:while (!fileIsFinished) {
            // -- for saving data we read --
            LongArray chunk = new LongArray(chunkSize);
            String c;
            for (int i = 0; i < chunkSize; i++) {
                c = reader.readLine();
                if (c == null) {
                    fileIsFinished = true;
                    break;
                }
                chunk.add(Long.parseLong(c));
            }
            chunk.sort();
            String addr = getNewFileName();
            writInFile(chunk, new File(addr), false);
            chunk = null;
            System.gc();
            // -- create an add a 'myBufferedReader' to 'readers' to user later --
            if (new File(addr).exists()) {
                BufferedReader br = new BufferedReader(new FileReader(addr));
                readers.add(br);
            }
        }
        reader.close();
    }

    public void merge() throws Exception {
        // -- output BufferedWriter --
        BufferedWriter writer = new BufferedWriter(new FileWriter(output));
        SortedArrayList res = new SortedArrayList();
        for (int i = 0; i < readers.size(); i++) {
            Long l = pop(readers.get(i));
            if (l == null) {
                continue;
            }
            res.add(new MyLong(l, readers.get(i)));
        }
        readers = null;
        System.gc();
        // -- until still there is a chunk that is not finished --
        while (!res.isEmpty()) {

            MyLong min = res.get(0);
            res.remove((short) 0);
            // -- min is going to be written in the output file --
            writer.write(min.l + "");
            writer.write("\n");
            String line = min.reader.readLine();
            if (line == null) {
                min.reader.close();
                continue;
            }
            res.add(new MyLong(Long.parseLong(line), min.reader));
        }
        writer.close();
    }

    private String getNewFileName() {
        return loc + "chunk" + (fileNumber++) + ".txt";
    }

    private void writInFile(LongArray l, File f, boolean append) throws IOException {
        if (l.isEmpty()) {
            return;
        }
        BufferedWriter b = new BufferedWriter(new FileWriter(f, append));
        for (int i = 0; i < l.size(); i++) {
            b.write(l.get(i) + "");
            b.write("\n");
        }
        b.close();
        b = null;
        System.gc();
    }

    private String getTimeOf(String str) {
        double t = System.currentTimeMillis();
        String result = str + ": " + (t - lastTime) / 1000 + " seconds.";
        lastTime = t;
        return result;
    }

    private class LongArray {

        private final long[] array;
        private int size;

        public LongArray(int length) {
            array = new long[length];
        }

        public void add(long l) {
            if (size == array.length) {
                throw new ArrayIndexOutOfBoundsException();
            }
            array[size++] = l;
        }

        public void remove(int index) {
            if (index < 0 || index >= size) {
                throw new ArrayIndexOutOfBoundsException();
            }
            for (int i = index; i < array.length - 1; i++) {
                array[i] = array[i + 1];
            }
            size--;
        }

        public long get(int index) {
            if (index < 0 || index >= size) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return array[index];
        }

        public void sort() {
            Arrays.sort(array, 0, size);
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }
    }
}



class SortedArrayList {

    private final ArrayList<MyLong> array;

    public SortedArrayList() {
        array = new ArrayList<>();
    }

    public boolean isEmpty() {
        return array.isEmpty();
    }

    public void add(MyLong e) {
        int left, right, mid;

        left = 0;
        right = array.size();

        while (left < right) {
            mid = (left + right) / 2;
            int result = (array.get(mid).l < e.l) ? -1 : (array.get(mid).l == e.l) ? 0 : 1;

            if (result > 0) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        array.add(left, e);
    }

    public MyLong get(int i) {
        return array.get(i);
    }

    public void remove(short i) {
        array.remove(i);
    }
}

class MyLong {

    public long l;
    public BufferedReader reader;

    public MyLong(long l, BufferedReader reader) {
        this.l = l;
        this.reader = reader;
    }

    public String toString() {
        return l + "";
    }
}
