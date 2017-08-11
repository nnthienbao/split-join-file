import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;

public class MainForm implements ShowProgress {
    private JPanel panelMain;
    private JTextField textFieldShowPath;
    private JButton chooseFileButton;
    private JButton splitButton;
    private JButton joinButton;
    private JRadioButton radioButtonPath;
    private JRadioButton radioButtonSize;
    private JPanel panelChoosePath;
    private JPanel panelChooseSize;
    private JSpinner spinnerTotalPart;
    private JSpinner spinnerSizePerPart;
    private JPanel panelWrapTypeSplit;
    private JProgressBar progressBarComplete;
    private JButton buttonCancel;

    private File fileSplit = null;

    private SplitJoinFiler splitJoinFiler = new SplitJoinFiler();

    private FileFilter fileFilterJoin = new FileFilter() {
        @Override
        public boolean accept(File f) {
            if(f.isDirectory()) return true;
            String fileName = f.getName();
            return fileName.endsWith(".000");
        }

        @Override
        public String getDescription() {
            return "Split file (*.000)";
        }
    };

    private Thread backgroundThread = null;

    public MainForm() {
        radioButtonPath.addActionListener(e -> {
            CardLayout card = (CardLayout) panelWrapTypeSplit.getLayout();
            card.show(panelWrapTypeSplit, "CardChoosePath");
        });
        radioButtonSize.addActionListener(e -> {
            CardLayout card = (CardLayout) panelWrapTypeSplit.getLayout();
            card.show(panelWrapTypeSplit, "CardChooseSize");
        });
        chooseFileButton.addActionListener(e -> {
            fileSplit = chooseFile(panelMain.getParent(), null, true,
                    "OK", "Choose file to split");
            if(fileSplit != null) {
                textFieldShowPath.setText(fileSplit.getAbsolutePath());
            }
        });
        splitButton.addActionListener(e -> {
            if(fileSplit == null) {
                JOptionPane.showMessageDialog(panelMain.getParent(),
                        "Please choose file to split",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File fileDirDes = chooseDirToSave(panelMain.getParent());
            if(fileDirDes != null && fileSplit != null) {
                String destination = fileDirDes.getAbsolutePath();
                if(radioButtonPath.isSelected()) {
                    int totalPath = (int) spinnerTotalPart.getValue();
                    backgroundThread = new Thread(() -> {
                            if(splitJoinFiler.splitByNumPart(fileSplit, totalPath,
                                    destination, this)) {
                                JOptionPane.showMessageDialog(panelMain.getParent(),
                                        "Split success", "Info",
                                        JOptionPane.INFORMATION_MESSAGE);
                                disableWorkingMode();
                            }

                    });
                    enableWorkingMode();
                    backgroundThread.start();
                } else {
                    long sizePerFile = (int) spinnerSizePerPart.getValue() * 1024 * 1024;
                    backgroundThread = new Thread(() -> {
                        if(splitJoinFiler.splitBySizePerFile(fileSplit, sizePerFile,
                                destination, this)) {
                            JOptionPane.showMessageDialog(panelMain.getParent(),
                                    "Split success", "Info",
                                    JOptionPane.INFORMATION_MESSAGE);
                            disableWorkingMode();
                        }
                    });
                    enableWorkingMode();
                    backgroundThread.start();
                }
            }
        });
        joinButton.addActionListener(e -> {
            File fileJoin = chooseFile(panelMain.getParent(), fileFilterJoin,
                    false, "Join", "Choose file to join");
            if(fileJoin != null) {
                backgroundThread = new Thread(() -> {
                    if(splitJoinFiler.join(fileJoin, this)) {
                        JOptionPane.showMessageDialog(panelMain.getParent(),
                                "Join success", "Info",
                                JOptionPane.INFORMATION_MESSAGE);
                        disableWorkingMode();
                    }
                });
                enableWorkingMode();
                backgroundThread.start();
            }
        });
        buttonCancel.addActionListener(e -> {
            if(backgroundThread != null) {
                splitJoinFiler.stopIfWorking();
                try {
                    backgroundThread.join();
                    JOptionPane.showMessageDialog(panelMain.getParent(),
                            "Cancel success", "Info",
                            JOptionPane.INFORMATION_MESSAGE);
                    disableWorkingMode();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    public static void main(String args[]) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        JFrame frame = new JFrame("Split Join File Simple");
        frame.setContentPane(new MainForm().panelMain);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void createUIComponents() {
        SpinnerModel spinnerModel =
                new SpinnerNumberModel(2, 2, 1000, 1);
        spinnerTotalPart = new JSpinner(spinnerModel);

        spinnerModel =
                new SpinnerNumberModel(1, 1, 10000, 1);
        spinnerSizePerPart = new JSpinner(spinnerModel);
    }

    private File chooseFile(Component parrent, FileFilter fileFilter, boolean acceptAllFile,
                            String approveButtonText, String title) {
        final JFileChooser fileChooser = new JFileChooser();
        if(fileFilter != null) fileChooser.setFileFilter(fileFilter);
        fileChooser.setAcceptAllFileFilterUsed(acceptAllFile);
        fileChooser.setDialogTitle(title);

        int returnVal = fileChooser.showDialog(parrent, approveButtonText);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            return file;
        }
        return null;
    }

    private File chooseDirToSave(Component parrent) {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose dir to save");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if(f.isDirectory()) return true;
                return false;
            }

            @Override
            public String getDescription() {
                return "Directory only";
            }
        });
        int returnVal = fileChooser.showOpenDialog(parrent);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            return file;
        }
        return null;
    }

    private void enableWorkingMode() {
        chooseFileButton.setEnabled(false);
        joinButton.setEnabled(false);
        splitButton.setEnabled(false);
        buttonCancel.setEnabled(true);
    }

    private void disableWorkingMode() {
        chooseFileButton.setEnabled(true);
        joinButton.setEnabled(true);
        splitButton.setEnabled(true);
        buttonCancel.setEnabled(false);
        setProgress(0);
    }

    @Override
    public void setProgress(int progress) {
        progressBarComplete.setValue(progress);
        progressBarComplete.setString(progress + "%");
    }
}











