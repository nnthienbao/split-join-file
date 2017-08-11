
import java.io.*;

public class SplitJoinFiler {

    private long totalByteNeed = 0;
    private long totalByteHasDone = 0;

    private BufferedInputStream in = null;
    private BufferedOutputStream out = null;

    public boolean splitByNumPart(File file, int totalPart, String destination, ShowProgress showProgress) {
        long totalSize = file.length();
        long sizePerFile;
        sizePerFile = totalSize / totalPart + totalSize % totalPart;

        return splitBySizePerFile(file, sizePerFile, destination, showProgress);
    }

    public boolean splitBySizePerFile(File file, long sizePerFile, String destination, ShowProgress showProgress) {
        long totalSize = file.length();
        int totalPart = (int) (totalSize / sizePerFile);
        if(totalSize % sizePerFile > 0) totalPart++;
        long sizeLastFile = totalSize - sizePerFile * (totalPart - 1);
        String originalFileName = file.getName();

        totalByteNeed = totalSize;
        totalByteHasDone = 0;

        int buffer = 1024 * 8;

        try {
            in = new BufferedInputStream(new FileInputStream(file));
            for(int i = 0; i < totalPart - 1; i++) {
                out = new BufferedOutputStream(
                        new FileOutputStream(makeSplitAbsolutePath(destination, originalFileName, i)));
                writeStream(sizePerFile, buffer, in, out, showProgress);
                out.close();
            }
            out = new BufferedOutputStream(
                    new FileOutputStream(makeSplitAbsolutePath(destination, originalFileName, totalPart - 1)));
            writeStream(sizeLastFile, buffer, in, out, showProgress);
            out.close();

            in.close();

        } catch (IOException e) {
            return false;
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return true;
    }

    private void readAndWriteByte(BufferedInputStream in, BufferedOutputStream out,
                                  int numByte) throws IOException {
        byte b[] = new byte[numByte];
        in.read(b);
        out.write(b);
    }

    private void writeStream(long sizeOfInStream, int buffer, BufferedInputStream in,
                                    BufferedOutputStream out, ShowProgress showProgress) throws IOException {
        if(sizeOfInStream <= buffer) {
            readAndWriteByte(in, out, (int) sizeOfInStream);
        } else {
            int totalLoop = (int) (sizeOfInStream / buffer);
            int remainByte = (int) (sizeOfInStream % buffer);
            for(int j = 0; j < totalLoop; j++) {
                readAndWriteByte(in, out, buffer);
                totalByteHasDone += buffer;
                if(showProgress != null) {
                    showProgress.setProgress((int) (totalByteHasDone * 100 / totalByteNeed));
                }
            }
            if(remainByte > 0) {
                readAndWriteByte(in, out, remainByte);
                totalByteHasDone += remainByte;
                if(showProgress != null) {
                    showProgress.setProgress((int) (totalByteHasDone * 100 / totalByteNeed));
                }
            }
        }
    }

    public boolean join(File file, ShowProgress showProgress) {

        File dir = file.getParentFile();
        String originalFileName = subEtxFileName(file.getName());

        int buffer = 1024 * 8;

        totalByteNeed = 0;
        totalByteHasDone = 0;

        int totalPart= dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if(isFileSplit(pathname.getName(), originalFileName)) {
                    totalByteNeed += pathname.length();
                    return true;
                }
                return false;
            }
        }).length;

        try {
            File joinFile = new File(dir.getAbsolutePath() + "/" + originalFileName);
            joinFile.createNewFile();
            out = new BufferedOutputStream(new FileOutputStream(joinFile));

            for(int i = 0; i < totalPart; i++) {
                File splitFile = new File(makeSplitAbsolutePath(dir.getAbsolutePath(), originalFileName, i));
                long splitFileSize = splitFile.length();
                in = new BufferedInputStream(new FileInputStream(splitFile));

                writeStream(splitFileSize, buffer, in, out, showProgress);
                in.close();
            }
            out.close();
        } catch (IOException e) {
            return false;
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private String makeSplitAbsolutePath(String dir, String originalFileName, int number) {
        return dir + "/" + originalFileName + "." + String.format("%03d", number);
    }

    private String subEtxFileName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    private boolean isFileSplit(String fileNameSplit, String originalFileName) {
        if(fileNameSplit.matches(originalFileName + ".[0-9][0-9][0-9]")) {
            return true;
        }
        return false;
    }

    public void stopIfWorking() {
        try {
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
