import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class QuoteMode {
    private enum GameState {
        MODE_SELECTION, PLAYING_GAME
    }

    private List<QuoteAndSource> quoteAndSourceList;
    private boolean gameStarted;
    private JTextPane quotePane;
    private JTextPane wordPane;
    private String[] printedWords;
    private JTextField inputField;
    private long startTime;
    private JLabel stopwatchLabel;
    private JLabel sourceLabel;
    private EndGameListener endGameListener;
    private JFrame frame;
    private GameState currentState;
    private GameModeSelection gameModeSelection;

    public QuoteMode(GameModeSelection gameModeSelection) {
        this.gameModeSelection = gameModeSelection;
        this.gameStarted = false;
        this.currentState = GameState.MODE_SELECTION;

        SwingUtilities.invokeLater(() -> {
            createAndShowQuoteModeGUI();
        });
    }

    public void setEndGameListener(EndGameListener listener) {
        this.endGameListener = listener;
    }

    private void createAndShowQuoteModeGUI() {
        frame = new JFrame("Quote Mode");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        try {
            Image iconImage = new ImageIcon(new File("src/typing.png").toURI().toURL()).getImage();
            frame.setIconImage(iconImage);
        } catch (IOException e) {
            e.printStackTrace();
        }

        frame.setVisible(true);

        Timer timer = new Timer(1000, e -> updateStopwatch());
        timer.start();

        currentState = GameState.PLAYING_GAME;
        frame.getContentPane().removeAll();

        quoteAndSourceList = readQuotesAndSourcesFromFile();
        initializeGame();
        loadRandomQuote();

        frame.revalidate();
        frame.repaint();
    }

    private void updateStopwatch() {
        if (gameStarted) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            updateStopwatchLabel(elapsedTime / 1000);
        } else {
            updateStopwatchLabel(0);
        }
    }

    private void updateStopwatchLabel(long elapsedTime) {
        long totalSeconds = elapsedTime;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (gameStarted) {
            String formattedTime = String.format("%02d:%02d", minutes, seconds);
            stopwatchLabel.setText(formattedTime);
        }
    }

    private List<QuoteAndSource> readQuotesAndSourcesFromFile() {
        List<QuoteAndSource> quotes = new ArrayList<>();

        try (Scanner fileSc = new Scanner(new File("src/quote.txt"))) {
            while (fileSc.hasNextLine()) {
                String line = fileSc.nextLine();
                String[] parts = line.split("\\|");

                if (parts.length == 2) {
                    String quote = parts[0];
                    String source = parts[1];
                    QuoteAndSource quoteAndSource = new QuoteAndSource(quote, source);
                    quotes.add(quoteAndSource);
                } else {
                    System.err.println("Invalid line format: " + line);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return quotes;
    }

    private void loadRandomQuote() {
        Random random = new Random();
        Collections.shuffle(quoteAndSourceList);
        QuoteAndSource quoteAndSource = quoteAndSourceList.get(0);

        startTime = System.currentTimeMillis();
        showQuote(quoteAndSource);
        showWordsWithColor(quoteAndSource.getQuote(), Color.BLUE);
        printedWords = quoteAndSource.getQuote().split("\\s");
    }

    private void showQuote(QuoteAndSource quoteAndSource) {
        StyledDocument doc = quotePane.getStyledDocument();
        SimpleAttributeSet quoteSet = new SimpleAttributeSet();
        StyleConstants.setForeground(quoteSet, Color.DARK_GRAY);

        try {
            doc.remove(0, doc.getLength());
            doc.insertString(0, quoteAndSource.getQuote(), quoteSet);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void showWordsWithColor(String text, Color color) {
        StyledDocument doc = wordPane.getStyledDocument();
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, color);

        String[] words = text.split("\\s");
        int cursorPosition = 0;

        for (String word : words) {
            try {
                doc.insertString(cursorPosition, word + " ", set);
                cursorPosition += word.length() + 1; // +1 to account for the space
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateColorsForTypedText(String typedText) {
        StyledDocument doc = wordPane.getStyledDocument();
        int cursorPosition = 0;
        int charIndex = 0;

        for (int i = 0; i < printedWords.length; i++) {
            String printedWord = printedWords[i];
            SimpleAttributeSet originalSet = new SimpleAttributeSet();
            StyleConstants.setForeground(originalSet, Color.DARK_GRAY);

            for (int j = 0; j < printedWord.length(); j++) {
                SimpleAttributeSet set = new SimpleAttributeSet();
                if (charIndex < typedText.length()) {
                    if (typedText.charAt(charIndex) == printedWord.charAt(j)) {
                        StyleConstants.setForeground(set, Color.GREEN);
                    } else {
                        StyleConstants.setForeground(set, Color.RED);
                    }
                    charIndex++;
                } else {
                    StyleConstants.setForeground(set, Color.DARK_GRAY);
                }

                doc.setCharacterAttributes(cursorPosition, 1, set, false);
                cursorPosition++;
            }

            if (i < printedWords.length - 1) {
                if (charIndex < typedText.length() && typedText.charAt(charIndex) == ' ') {
                    StyleConstants.setForeground(originalSet, Color.GREEN);
                    charIndex++;
                } else {
                    StyleConstants.setForeground(originalSet, Color.DARK_GRAY);
                }
                doc.setCharacterAttributes(cursorPosition, 1, originalSet, false);
                cursorPosition++;
            }
        }
    }

    private void initializeGame() {
        frame.getContentPane().removeAll();

        stopwatchLabel = new JLabel("Press any key to start. Press ENTER after finishing typing the quote.");
        stopwatchLabel.setFont(stopwatchLabel.getFont().deriveFont(16.0f));
        stopwatchLabel.setHorizontalAlignment(SwingConstants.CENTER);
        frame.add(stopwatchLabel, BorderLayout.NORTH);

        quotePane = new JTextPane();
        quotePane.setEditable(false);
        quotePane.setFont(quotePane.getFont().deriveFont(16.0f));
        frame.add(new JScrollPane(quotePane), BorderLayout.CENTER);

        sourceLabel = new JLabel("Source: ");
        sourceLabel.setFont(sourceLabel.getFont().deriveFont(14.0f));
        sourceLabel.setForeground(Color.BLUE);
        frame.add(sourceLabel, BorderLayout.SOUTH);

        wordPane = new JTextPane();
        wordPane.setEditable(false);
        wordPane.setFont(wordPane.getFont().deriveFont(16.0f));
        frame.add(new JScrollPane(wordPane), BorderLayout.SOUTH);

        inputField = new JTextField();
        inputField.setFont(inputField.getFont().deriveFont(16.0f));
        inputField.getDocument().addDocumentListener(new InputDocumentListener());

        inputField.addActionListener(e -> {
            if (!gameStarted) {
                gameStarted = true;
                startTime = System.currentTimeMillis();
                updateStopwatchLabel(0);
                loadRandomQuote();
                Timer timer = new Timer(1000, timerEvent -> updateStopwatch());
                timer.start();
            } else {
                endQuoteModeGame();
            }
        });

        frame.add(inputField, BorderLayout.SOUTH);

        frame.revalidate();
        frame.repaint();

        gameStarted = false;
        initializeStartTime();
    }

    private void endQuoteModeGame() {
        if (gameStarted) {
            gameStarted = false;

            long elapsedTime = System.currentTimeMillis() - startTime;
            updateStopwatchLabel(elapsedTime);
            inputField.setEnabled(false);

            accuracyAndWPM(inputField.getText(), elapsedTime, quoteAndSourceList.get(0));

            inputField.setText("");
            quotePane.setText("");

            inputField.setEnabled(true);
            startTime = System.currentTimeMillis();
            updateStopwatchLabel(0);

            if (endGameListener != null) {
                int choice = JOptionPane.showConfirmDialog(null, "Do you want to play the QUOTE MODE again?", "Play Again", JOptionPane.YES_NO_OPTION);

                if (choice == JOptionPane.YES_OPTION) {
                    currentState = GameState.PLAYING_GAME;
                    frame.getContentPane().removeAll();
                    initializeGame();
                    loadRandomQuote();
                    frame.revalidate();
                    frame.repaint();
                } else {
                    currentState = GameState.MODE_SELECTION;
                    frame.getContentPane().removeAll();
                    gameModeSelection.showModeSelectionPage();
                    frame.revalidate();
                    frame.repaint();
                }
            }
        }
    }

    private class InputDocumentListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            handleTextChange();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            handleTextChange();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            // Plain text components don't fire these events
        }

        private void handleTextChange() {
            String typedText = inputField.getText().trim();

            if (!gameStarted) {
                updateStopwatchLabel(0); // Reset the stopwatch label
            }

            if (typedText.endsWith("\n")) {
                // Remove the Enter key character from the typed text
                typedText = typedText.substring(0, typedText.length() - 1);
                inputField.setText(typedText);

                endQuoteModeGame();
            }

            if (!gameStarted) {
                gameStarted = true;
                startTime = System.currentTimeMillis();

                // Start the timer here, after the game has started
                Timer timer = new Timer(1000, timerEvent -> updateStopwatch());
                timer.start();
            }

            updateColorsForTypedText(typedText);
        }
    }

    private void accuracyAndWPM(String userType, long elapsedTime, QuoteAndSource quoteAndSource) {
        StyledDocument doc = quotePane.getStyledDocument();

        String correctText = quoteAndSource.getQuote();
        String typedText = userType;

        int mistakes = 0;

        int minLength = Math.min(correctText.length(), typedText.length());

        for (int i = 0; i < minLength; i++) {
            if (correctText.charAt(i) != typedText.charAt(i)) {
                mistakes++;
            }
        }

        // If one text is longer than the other, count the extra characters as mistakes
        mistakes += Math.abs(correctText.length() - typedText.length());

        // Count backspaces as mistakes
        int backspaceCount = Math.abs(correctText.length() - typedText.length());
        mistakes += backspaceCount;

        int totalCharacters = correctText.length();
        double accuracy = ((double) (totalCharacters - mistakes) / totalCharacters) * 100;
        double wpm = (totalCharacters / 5.0) / (elapsedTime / 60000.0);

        String timeUsedInfo = String.format("Time Used: %02d:%02d", elapsedTime / 60000, (elapsedTime % 60000) / 1000);
        String mistakesInfo = String.format("Mistakes: %d", mistakes);
        String accuracyInfo = String.format("Accuracy: %.2f%%", accuracy);
        String wpmInfo = String.format("WPM: %.2f", wpm);
        String sourceInfo = "Source: " + quoteAndSource.getSource();

        JOptionPane.showMessageDialog(null,
                String.format("Game Over!\n%s\n%s\n%s\n%s\n%s", timeUsedInfo, accuracyInfo, wpmInfo, mistakesInfo, sourceInfo),
                "Game Over", JOptionPane.INFORMATION_MESSAGE);

        storeResult("accuracyQuote.txt", String.format("%.2f%%", accuracy));
        storeResult("wpmQuote.txt", String.format("%.2f", wpm));

        endQuoteModeGame();
    }


    private void storeResult(String fileName, String result) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(result);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeStartTime() {
        startTime = System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameModeSelection gameModeSelection = new GameModeSelection();
            QuoteMode quoteMode = new QuoteMode(gameModeSelection);
        });
    }

    public interface EndGameListener {
        void onEndGame();
    }

    public class QuoteAndSource {
        private final String quote;
        private final String source;

        public QuoteAndSource(String quote, String source) {
            this.quote = quote;
            this.source = source;
        }

        public String getQuote() {
            return quote;
        }

        public String getSource() {
            return source;
        }
    }
}
