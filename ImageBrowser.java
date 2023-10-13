import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

public class ImageBrowser extends JFrame {

    private JPanel thumbnailPanel;
    private JLabel displayLabel;
    Comparator<File> comparator = (file1, file2) -> {
        int number1 = extractImageNumber(file1.getName());
        int number2 = extractImageNumber(file2.getName());
        return Integer.compare(number1, number2);
    };

    private ArrayList<File> imageFiles = new ArrayList<>();

    static Map<BufferedImage, Integer[]> map = new HashMap<BufferedImage, Integer[]>();
    // change the folderPath variable to the absolute path of the GUI Demo directory
    // in your system
    static String folderPath = "/Users/Documents/GUI Demo/images";
    static String outpuString = "output.txt";

    // 
    private static Integer[][] intensityMatrix;
    File currentFile;
    //Stores the distance between image files (Intensity)
    Double[][] manhattanDistances;
    //Stores the distance between image files (Color Code)
    Double[][] colorManhattanDistances;
    File[] curSortedFiles;
    File[] curColorSortedFiles;

    private static Integer[][] colorCodeMatrix;

    public ImageBrowser() {

        setTitle("Image Browser");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loadImageFiles(folderPath);
        int rows = imageFiles.size() / 3;

        GridLayout gridLayout = new GridLayout(rows, 3); // creating GridLayout object
        gridLayout.setHgap(10); // setting horizontal gap to 10
        gridLayout.setVgap(10);// setting vertical gap to 10

        thumbnailPanel = new JPanel(gridLayout);

        JScrollPane scrollPane = new JScrollPane(thumbnailPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(10);

        displayLabel = new JLabel();
        displayLabel.setHorizontalAlignment(JLabel.CENTER);

        JButton colorCodeBasedRetrieval = new JButton("Retrieve by Color Code");
        colorCodeBasedRetrieval.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                colorCodeCompareImages(); // This should populate curColorSortedFiles
                displayColorCodeThumbnails(curColorSortedFiles); // This should display the thumbnails of curSortedFiles
            }
        });

        JButton intensityBasedRetrieval = new JButton("Retrieve by Intensity");
        intensityBasedRetrieval.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                compareImages(); // This should populate curSortedFiles
                displayThumbnails(curSortedFiles); // This should display the thumbnails of curSortedFiles
            }
        });
        
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(displayLabel, BorderLayout.CENTER);
        rightPanel.add(intensityBasedRetrieval, BorderLayout.SOUTH);
        rightPanel.add(colorCodeBasedRetrieval, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, rightPanel);
        splitPane.setDividerLocation(450);
        add(splitPane);
        displayThumbnails();

    }

    //method to display the sorted files as thumbnails
    private void displayThumbnails(File[] filesToDisplay) {
        thumbnailPanel.removeAll(); // Clear existing thumbnails
        if (filesToDisplay != null && filesToDisplay.length > 0) {
            for (File file : filesToDisplay) {
                ImageIcon imageIcon = new ImageIcon(new ImageIcon(file.getAbsolutePath()).getImage()
                        .getScaledInstance(100, 100, Image.SCALE_DEFAULT));
                JLabel label = new JLabel(imageIcon);
                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        currentFile = file;
                        displayImage(file);
                        System.out.println(currentFile.getName());
                    }
                });
                thumbnailPanel.add(label);
            }
        } else {
            System.out.println("No images to display!");
        }
        thumbnailPanel.revalidate();
        thumbnailPanel.repaint();
    }

    /* populates the left side of the GUI with the ranked files (by Color Code) */
    private void displayColorCodeThumbnails(File[] filesToDisplay) {
        System.out.println("got to the color code thumbnails");
        thumbnailPanel.removeAll(); // Clear existing thumbnails
        if (filesToDisplay != null && filesToDisplay.length > 0) {
            for (File file : filesToDisplay) {
                ImageIcon imageIcon = new ImageIcon(new ImageIcon(file.getAbsolutePath()).getImage()
                        .getScaledInstance(100, 100, Image.SCALE_DEFAULT));
                JLabel label = new JLabel(imageIcon);
                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        currentFile = file;
                        displayImage(file);
                        System.out.println(currentFile.getName());
                    }
                });
                thumbnailPanel.add(label);
            }
        } else {
            System.out.println("No images to display!");
        }
        thumbnailPanel.revalidate();
        thumbnailPanel.repaint();

    }

    /* to get the int value of the image file name for example 1.jpg will return 1 so i can use it to 
    sort the image files arraylist using a custom comparator*/
    private static int extractImageNumber(String filename) {
        try {
            return Integer.parseInt(filename.substring(0, filename.lastIndexOf('.')));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid image filename format: " + filename);
        }
    }

    /* Loads all od the image files and populates an arrayList called imageFiles */
    private void loadImageFiles(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Invalid folder path: " + folderPath);
            return;
        }
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith("jpg"));
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    imageFiles.add(file);
                }
            }
            Collections.sort(imageFiles, comparator);
            int numOfImages = imageFiles.size();
            intensityMatrix = new Integer[numOfImages][26];
            colorCodeMatrix = new Integer[numOfImages][64];

            for (int i = 0; i < intensityMatrix.length; i++) {
                for (int j = 0; j < intensityMatrix[i].length; j++) {
                    intensityMatrix[i][j] = 0;
                }
            }

            for (int i = 0; i < colorCodeMatrix.length; i++) {
                for (int j = 0; j < colorCodeMatrix[i].length; j++) {
                    colorCodeMatrix[i][j] = 0;
                }
            }
        } else {
            System.out.println("No JPG files found in: " + folderPath);
        }
    }

    /* This populates the color code feature matrix */
    public void loadColorCode(String folderPath) {

        if (imageFiles != null) {

            try (FileOutputStream output = new FileOutputStream("ColorCodeOutput.txt");
                    Writer colorOutputStreamWriter = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
                int imageCount = 0;
                for (File file : imageFiles) {

                    try {
                        BufferedImage image = ImageIO.read(file);
                        int height = image.getHeight();
                        int width = image.getWidth();

                        colorOutputStreamWriter.write("Image " + file.getName() + "\n\n");

                        Integer[] bin = new Integer[64];
                        int pixelCount = 0;

                        for (int i = 0; i < bin.length; i++) {
                            bin[i] = 0;
                        }

                        for (int i = 0; i < height; i++) {

                            for (int j = 0; j < width; j++) {

                                 int rgb = image.getRGB(j, i);
                                 int red = (rgb >> 16) & 0xFF;
                                 int green = (rgb >> 8) & 0xFF;
                                 int blue = rgb & 0xFF;

                                bin[rgbToDecimal(red, green, blue)]++;
                                pixelCount++;
                            }
                        }

                        for (int i = 0; i < 64; i++) {
                            colorCodeMatrix[imageCount][i] = bin[i];
                        }

                        for (int i = 0; i < bin.length; i++) {
                            colorOutputStreamWriter.write("bin " + i + " = " + bin[i] + "\n");
                        }

                        colorOutputStreamWriter.write("\n" +
                                "\n-------------------------------------------------------------\n");
                        imageCount++;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
    /* a helper method for loadColorCode to return a int value for and rgb value */
     public int rgbToDecimal(int red, int green, int blue) {
         int rr = red >> 6;
         int gg = green >> 6;
         int bb = blue >> 6;
         return (rr << 4) | (gg << 2) | bb;
     }

     /* this method populates the intensity matrix */
    public void loadIntensity(String folderPath) {

        if (imageFiles != null) {

            try (FileOutputStream output = new FileOutputStream("output.txt");
                    Writer outputStreamWriter = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
                int imageCount = 0;
                for (File file : imageFiles) {

                    try {
                        BufferedImage image = ImageIO.read(file);
                        int height = image.getHeight();
                        int width = image.getWidth();

                        outputStreamWriter.write("Image " + file.getName() + "\n\n");

                        Integer[] bin = new Integer[26];
                        int pixelCount = 0;

                        for (int i = 0; i < bin.length; i++) {
                            bin[i] = 0;
                        }

                        for (int i = 0; i < height; i++) {

                            for (int j = 0; j < width; j++) {

                                int rgb = image.getRGB(j, i);
                                int red = (rgb >> 16) & 0xFF;
                                int green = (rgb >> 8) & 0xFF;
                                int blue = rgb & 0xFF;

                                double intensity = (0.299 * red) + (0.587 * green) + (0.114 * blue);
                                //gets the index to increment
                                int histbin = histBin(intensity);
                                bin[histbin]++;

                                pixelCount++;
                            }
                        }

                        bin[0] = pixelCount;
                        for (int i = 0; i < 26; i++) {
                            intensityMatrix[imageCount][i] = bin[i];
                        }

                        for (int i = 0; i < bin.length; i++) {
                            outputStreamWriter.write("bin " + i + " = " + bin[i] + "\n");
                        }

                        map.put(image, bin);
                        outputStreamWriter.write("\n" +
                                "\n-------------------------------------------------------------\n");
                        imageCount++;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    /* helper method that find the index of the orginal file */
    public int findInOriginal(Double distance, Double[] distanceValues) {
        int index = -1;

        for (int i = 0; i < imageFiles.size(); i++) {
            if (distanceValues != null) {
                if (distance == distanceValues[i]) {
                    index = i;
                }
            }

        }

        return index;
    }

    

    /* Prints the Intensity matrix to a file */
    public static void printMatrixToFile(Double[][] matrix) {
        try (FileOutputStream out = new FileOutputStream("manhattanDistanceOutput.txt");
                Writer outputStreamWriter = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    // Format number with 3 decimal places and at least 7 characters wide
                    outputStreamWriter.write(String.format("%7.3f | ", matrix[i][j]));
                }
                outputStreamWriter.write("\n");
            }
            System.out.println(matrix.length);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* method to determine which index (bin) to put the intensity value in */
    private static int histBin(Double intensity) {
        if (intensity >= 0 && intensity <= 10) {
            return 1;
        } else if (intensity > 10 && intensity <= 20) {
            return 2;
        } else if (intensity > 20 && intensity <= 30) {
            return 3;
        } else if (intensity > 30 && intensity <= 40) {
            return 4;
        } else if (intensity > 40 && intensity <= 50) {
            return 5;
        } else if (intensity > 50 && intensity <= 60) {
            return 6;
        } else if (intensity > 60 && intensity <= 70) {
            return 7;
        } else if (intensity > 70 && intensity <= 80) {
            return 8;
        } else if (intensity > 80 && intensity <= 90) {
            return 9;
        } else if (intensity > 90 && intensity <= 100) {
            return 10;
        } else if (intensity > 100 && intensity <= 110) {
            return 11;
        } else if (intensity > 110 && intensity <= 120) {
            return 12;
        } else if (intensity > 120 && intensity <= 130) {
            return 13;
        } else if (intensity > 130 && intensity <= 140) {
            return 14;
        } else if (intensity > 140 && intensity <= 150) {
            return 15;
        } else if (intensity > 150 && intensity <= 160) {
            return 16;
        } else if (intensity > 160 && intensity <= 170) {
            return 17;
        } else if (intensity > 170 && intensity <= 180) {
            return 18;
        } else if (intensity > 180 && intensity <= 190) {
            return 19;
        } else if (intensity > 190 && intensity <= 200) {
            return 20;
        } else if (intensity > 200 && intensity <= 210) {
            return 21;
        } else if (intensity > 210 && intensity <= 220) {
            return 22;
        } else if (intensity > 220 && intensity <= 230) {
            return 23;
        } else if (intensity > 230 && intensity <= 240) {
            return 24;
        } else {
            return 25;
        }
    }

    /* populates the intensity manahattan distance matrix with values */
    private void comparisons() {
        int numOfImages = imageFiles.size();
        manhattanDistances = new Double[numOfImages][numOfImages];

        loadIntensity(folderPath);

        for (int i = 0; i < numOfImages; i++) {
            for (int j = i; j < numOfImages; j++) { // Adjusting the start index of j

                if (i == j) {
                    manhattanDistances[i][j] = 0.0;
                } else {
                    double distance = calculateManhattanDistance(intensityMatrix[i], intensityMatrix[j]);
                    // Since distance[i][j] == distance[j][i]
                    manhattanDistances[i][j] = distance;
                    manhattanDistances[j][i] = distance;
                }
            }
        }

        printMatrixToFile(manhattanDistances);

    }
    
    /* populates the color code manahattan distance matrix with values */
    private void colorComparisons() {
        int numOfImages = imageFiles.size();
        colorManhattanDistances = new Double[numOfImages][numOfImages];

        loadColorCode(folderPath);

        for (int i = 0; i < numOfImages; i++) {
            for (int j = i; j < numOfImages; j++) { // Adjusting the start index of j

                if (i == j) {
                    colorManhattanDistances[i][j] = 0.0;
                } else {
                    double distance = calculateManhattanDistance(colorCodeMatrix[i], colorCodeMatrix[j]);
                    // Since distance[i][j] == distance[j][i]
                    colorManhattanDistances[i][j] = distance;
                    colorManhattanDistances[j][i] = distance;
                }
            }
        }

        printColorMatrixToFile(colorManhattanDistances);
    }

    /* populates the curSorted files array with a ranked list of files that are similar to the current selected file
     * (Ranked by Intensity)
     */
    public void compareImages() {
        comparisons();
        //gets the row number of the specific image in the intensity manhattan distance matrix
        int row = extractImageNumber(currentFile.getName()) - 1;
        System.out.println(row);

        //copy of the specific row retrieved by the previous line
        Double[] imageCount = new Double[imageFiles.size()];

        //copies the values of the original row into the duplicate array imageCount
        for (int i = 0; i < imageFiles.size(); i++) {
            if (manhattanDistances[row][i] == null) {
                imageCount[i] = 0.0;
            } else {
                imageCount[i] = manhattanDistances[row][i];
            }

        }

        //sorts the duplicate array image count
        Arrays.sort(imageCount);

        //defines the file array that stores the files in sorted order
        curSortedFiles = new File[imageCount.length];
        Double[] original = manhattanDistances[row];

        for (int i = 0; i < curSortedFiles.length; i++) {
            // gets the original index of the file from the sorted list of distances
            int originalIndex = findInOriginal(imageCount[i], original);
            curSortedFiles[i] = imageFiles.get(originalIndex);
        }

    }

    public static void printMatrix(Integer[][] matrix) {

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + " | ");
            }
            System.out.println();
        }

        System.out.println(matrix.length);
    }

    /* Populates the curColorSortedFiles array with a sorted list of files that are similar to the selected file 
     * (Ranked by Color Code)
    */
    public void colorCodeCompareImages() {
        colorComparisons();

        //gets the row number of the specific image in the colorCode manhattan distance matrix
        int row = extractImageNumber(currentFile.getName()) - 1;

        //copy of the specific row retrieved by the previous line
        Double[] imageCount = new Double[imageFiles.size()];
        
        //copies the values of the original row into the duplicate array imageCount
        for (int i = 0; i < imageFiles.size(); i++) {
            
            imageCount[i] = colorManhattanDistances[row][i];

        }
        
        //sorts the duplicate array image count
        Arrays.sort(imageCount);

        //defines the file array that stores the files in sorted order
        curColorSortedFiles = new File[imageCount.length];
        Double[] original = colorManhattanDistances[row];

        for (int i = 0; i < curColorSortedFiles.length; i++) {
            // gets the original index of the file from the sorted list of distances
            int originalIndex = findInOriginal(imageCount[i], original);
            curColorSortedFiles[i] = imageFiles.get(originalIndex);
        }

        for (int i = 0; i < curColorSortedFiles.length; i++) {
            System.out.println(curColorSortedFiles[i].getName());
        }
    }

    /* prints the color code matrix to an external file */
    private void printColorMatrixToFile(Double[][] matrix) {
        try (FileOutputStream out = new FileOutputStream("colorMan.txt", true); // Appending, optional
                Writer outputStreamWriter = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

            if (matrix == null || matrix.length == 0) {
                System.out.println("Matrix is empty or null.");
                return;
            }

            for (int i = 0; i < matrix.length; i++) {
                if (matrix[i] == null) {
                    System.out.println("Row " + i + " is null.");
                    continue;
                }
                for (int j = 0; j < matrix[i].length; j++) {
                    if (matrix[i][j] == null) {
                        outputStreamWriter.write(String.format("%7s | ", "NULL"));
                    } else {
                        outputStreamWriter.write(String.format("%7.3f | ", matrix[i][j]));
                    }
                }
                outputStreamWriter.write("\n");
            }
            outputStreamWriter.flush(); // Explicitly flushing
            System.out.println("Matrix printed. Size: " + matrix.length);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occurred while writing to the file.");
        }
    }

    /* helper method to calculate manhattan distance between two images' intensity values */
    private double calculateManhattanDistance(Integer[] vectorA, Integer[] vectorB) {
        double sum = 0.0;
        int pixels = 256 * 384; // Assuming constant image size, adjust as per actual size

        for (int i = 0; i < vectorA.length; i++) {
            sum += Math.abs(((double) vectorA[i] / pixels) - ((double) vectorB[i] / pixels));
        }

        return Math.round(1000.0 * sum) / 1000.0;
    }

    /* Populates the left side of the GUI with thumbnails of all the images in the directory */
    private void displayThumbnails() {
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (File file : imageFiles) {
                ImageIcon imageIcon = new ImageIcon(new ImageIcon(file.getAbsolutePath()).getImage()
                        .getScaledInstance(100, 100, Image.SCALE_DEFAULT));
                JLabel label = new JLabel(imageIcon);
                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        currentFile = file;
                        displayImage(file);
                        System.out.println(currentFile.getName());
                    }
                });
                thumbnailPanel.add(label);
            }
        } else {
            System.out.println("No images to display!");
        }
        thumbnailPanel.revalidate();
        thumbnailPanel.repaint();
    }

    /* this is to display the selected file on the right side of the GUI window */
    private void displayImage(File file) {
        currentFile = file;
        if (file != null && file.exists()) {
            ImageIcon imageIcon = new ImageIcon(new ImageIcon(file.getAbsolutePath()).getImage()
                    .getScaledInstance(480, 360, Image.SCALE_DEFAULT));
            displayLabel.setIcon(imageIcon);
        }
    }


    public static void main(String[] args) {
        try {
            ImageBrowser a = new ImageBrowser();
            System.out.println("RGB value is : " + a.rgbToDecimal(255, 0, 255));
            SwingUtilities.invokeLater(() -> new ImageBrowser().setVisible(true));
            a.colorCodeCompareImages();

            a.compareImages();

            System.out.println();

        } catch (NullPointerException e) {
            System.out.println("Please select an image from the left side of the GUI window (click on a thumbnail)");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
