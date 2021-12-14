import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.text.*;

class Editor implements ActionListener {
  JTextPane textPane;
  JFrame frame;

  static String QUERY_PATH = "tree-sitter-ruby/queries/highlights.scm";
  static String TYPE_ID_TO_COLOR = "typeIdToColor.txt";
  private static Parser parser;
  private static Query query;
  private static List<Color> typeIdToColor = new ArrayList<>();

  Editor() {
    frame = new JFrame("Editor");

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLocationRelativeTo(null);

    final StyleContext context = StyleContext.getDefaultStyleContext();
    DefaultStyledDocument doc =
        new DefaultStyledDocument() {
          public void insertString(int offset, String str, AttributeSet a)
              throws BadLocationException {
            super.insertString(offset, str, a);
            highlightAll(getText(0, getLength()));
          }

          public void remove(int offset, int length) throws BadLocationException {
            super.remove(offset, length);
            highlightAll(getText(0, getLength()));
          }

          private void highlightAll(String str) {
            byte[] source = str.getBytes(StandardCharsets.UTF_8);
            // Contains triplets written in an array form: (start_byte,end_byte, color_index).
            int[] highlights =
                Treesitter.highlight(
                    source, source.length, parser.get(), query.getQuery(), query.getCursor());
            for (int i = 0; i < highlights.length / 3; i++) {
              AttributeSet attr =
                  context.addAttribute(
                      context.getEmptySet(),
                      StyleConstants.Foreground,
                      typeIdToColor.get(highlights[3 * i + 2]));
              setCharacterAttributes(
                  highlights[3 * i], highlights[3 * i + 1] - highlights[3 * i], attr, false);
            }
          }
        };

    textPane = new JTextPane(doc);

    JMenuBar menuBar = new JMenuBar();
    JMenu menu = new JMenu("File");

    JMenuItem newButton = new JMenuItem("New");
    JMenuItem openButton = new JMenuItem("Open");
    JMenuItem saveButton = new JMenuItem("Save");

    newButton.addActionListener(this);
    openButton.addActionListener(this);
    saveButton.addActionListener(this);

    menu.add(newButton);
    menu.add(openButton);
    menu.add(saveButton);

    menuBar.add(menu);

    textPane.setText(readFile("textExample.txt"));

    frame.setJMenuBar(menuBar);
    frame.add(textPane);
    frame.add(
        new JScrollPane(
            textPane,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    frame.setSize(700, 700);
    frame.setVisible(true);
  }

  // Buttons clicks event handlers.
  public void actionPerformed(ActionEvent event) {
    String buttonName = event.getActionCommand();

    if (buttonName.equals("Save")) {
      JFileChooser fileDialog = new JFileChooser("f:");
      int dialogStatus = fileDialog.showSaveDialog(null);

      if (dialogStatus == JFileChooser.APPROVE_OPTION) {
        File file = new File(fileDialog.getSelectedFile().getAbsolutePath());

        try {
          FileWriter fileWrite = new FileWriter(file, false);
          BufferedWriter writer = new BufferedWriter(fileWrite);

          writer.write(textPane.getText());
          writer.flush();
          writer.close();
        } catch (Exception e) {
          JOptionPane.showMessageDialog(frame, e.getMessage());
        }
      }
    } else if (buttonName.equals("Open")) {
      JFileChooser fileDialog = new JFileChooser("f:");
      if (fileDialog.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        textPane.setText(readFile(fileDialog.getSelectedFile().getAbsolutePath()));
      }
    } else if (buttonName.equals("New")) {
      textPane.setText("");
    }
  }

  private static List<Color> loadColors() {
    String colorsString = readFile(TYPE_ID_TO_COLOR);
    List<Color> colors = new ArrayList<>();
    for (String rgbString : colorsString.split("\n")) {
      int[] rgb = Arrays.stream(rgbString.split(" ")).mapToInt(Integer::parseInt).toArray();
      colors.add(new Color(rgb[0], rgb[1], rgb[2]));
    }
    return colors;
  }

  private static String readFile(String fileName) {
    String queryStr;
    try {
      queryStr = Files.readString(Path.of(fileName), StandardCharsets.UTF_8);
    } catch (IOException e) {
      e.printStackTrace();
      return "";
    }
    return queryStr;
  }

  public static void main(String args[]) {
    System.loadLibrary("jnicode");
    parser = new Parser();

    String queryStr = readFile(QUERY_PATH);
    byte[] queryBytes = queryStr.getBytes(StandardCharsets.UTF_8);
    query = new Query(queryBytes, queryBytes.length);
    typeIdToColor = loadColors();
    new Editor();
  }
}
