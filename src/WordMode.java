import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;


// last words correct then end auto

public class WordMode {
    private enum GameState {
        MODE_SELECTION, WORD_SELECTION, PLAYING_GAME
    }

    private String[] printedWords;
    private boolean gameStarted;
    private JTextPane wordPane;
    private JTextField inputField;
    private long startTime;
    private int wordCount;
    private JLabel stopwatchLabel;
    private EndGameListener endGameListener;
    private JFrame frame;
    private GameState currentState;
    private GameModeSelection gameModeSelection; // Reference to GameModeSelection

    public WordMode(GameModeSelection gameModeSelection) {
        this.gameModeSelection = gameModeSelection;
        this.gameStarted = false;
        this.currentState = GameState.MODE_SELECTION;

        SwingUtilities.invokeLater(() -> {
            createAndShowWordModeGUI();
        });
    }

    public void setEndGameListener(EndGameListener listener) {
        this.endGameListener = listener;
    }

    private void createAndShowWordModeGUI() {
        frame = new JFrame("Word Mode");
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

        if (currentState == GameState.MODE_SELECTION) {
            showWordModeOptions();
        } else if (currentState == GameState.PLAYING_GAME) {
            initializeGame();
        }

        frame.setVisible(true);

        Timer timer = new Timer(1000, e -> updateStopwatch());
        timer.start();
    }

    private void updateStopwatch() {
        if (gameStarted) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            updateStopwatchLabel(elapsedTime / 1000); // Convert milliseconds to seconds
        } else {
            // If the game is not started, reset the stopwatch
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
        } else {
            stopwatchLabel.setText("00:00");
        }
    }

    private JButton createModeButton(String buttonText, int wordCount) {
        JButton button = new JButton(buttonText);
        button.addActionListener(e -> {
            this.wordCount = wordCount;
            currentState = GameState.PLAYING_GAME;
            frame.getContentPane().removeAll();
            initializeGame();
            loadWords();  // Load words and start the game directly
            frame.revalidate();
            frame.repaint();
        });
        return button;
    }

    public void showWordModeOptions() {
        frame.getContentPane().removeAll();

        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));

        panel.add(createModeButton("10 Words", 10));
        panel.add(createModeButton("25 Words", 25));
        panel.add(createModeButton("50 Words", 50));
        panel.add(createModeButton("100 Words", 100));

        frame.add(panel, BorderLayout.CENTER);

        frame.setSize(600, 400); // Set a fixed size
        frame.setVisible(true);
    }

    private void loadWords() {
        Random random = new Random();
        List<String> words = readWordsFromFile();

        printedWords = new String[wordCount];
        StringBuilder wordDisplay = new StringBuilder();

        for (int i = 0; i < wordCount; i++) {
            String randomWord = getRandomWord(words, random);
            printedWords[i] = randomWord;
            wordDisplay.append(randomWord).append(" ");
        }

        // Initialize the start time when loading words
        startTime = System.currentTimeMillis();

        showWordsWithColor(wordDisplay.toString(), Color.DARK_GRAY);
    }

    private String getRandomWord(List<String> words, Random random) {
        return words.get(random.nextInt(words.size()));
    }


    private void initializeGame() {
        frame.getContentPane().removeAll();

        stopwatchLabel = new JLabel("Press any key to start.");
        stopwatchLabel.setFont(stopwatchLabel.getFont().deriveFont(20.0f));
        stopwatchLabel.setHorizontalAlignment(SwingConstants.CENTER);
        frame.add(stopwatchLabel, BorderLayout.NORTH);

        wordPane = new JTextPane();
        wordPane.setEditable(false);
        wordPane.setFont(wordPane.getFont().deriveFont(16.0f));
        frame.add(new JScrollPane(wordPane), BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.setFont(inputField.getFont().deriveFont(16.0f));

        // Add a listener to consume Enter key presses
        inputField.addActionListener(e -> {
            if (!gameStarted) {
                gameStarted = true;
                startTime = System.currentTimeMillis();
                loadWords(); // Load words when the game starts

                // Start the timer here, after the game has started
                Timer timer = new Timer(1000, timerEvent -> updateStopwatch());
                timer.start();
            } else {
                endWordModeGame();
            }
        });

        // Move the document listener setup outside the ActionListener
        inputField.getDocument().addDocumentListener(new InputDocumentListener());

        frame.add(inputField, BorderLayout.SOUTH);

        frame.revalidate();
        frame.repaint();

        gameStarted = false;

        // Initialize the start time when the game is initialized
        initializeStartTime();

        // Load words only once when the game starts
        loadWords();
    }


    private void endWordModeGame() {
        if (gameStarted) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            updateStopwatchLabel(elapsedTime);

            gameStarted = false;
            inputField.setEnabled(false);

            // Rest of the code for calculating accuracy and WPM remains unchanged
            accuracyAndWPM(inputField.getText().split(" "), elapsedTime);

            inputField.setText("");
            wordPane.setText("");

            inputField.setEnabled(true);

            // Notify the end-game listener
            if (endGameListener != null) {
                int choice = JOptionPane.showConfirmDialog(null, "Do you want to play the WORD MODE again?", "Play Again", JOptionPane.YES_NO_OPTION);

                if (choice == JOptionPane.YES_OPTION) {
                    // If yes, directly show word count choice
                    currentState = GameState.WORD_SELECTION; // Set to WORD_SELECTION state
                    frame.getContentPane().removeAll();
                    showWordModeOptions(); // Show word count choice
                    frame.revalidate();
                    frame.repaint();
                } else {
                    // If no, return to mode selection
                    currentState = GameState.MODE_SELECTION;
                    frame.getContentPane().removeAll();
                    gameModeSelection.showModeSelectionPage();
                    frame.revalidate();
                    frame.repaint();
                }
            }
        }
    }



    private List<String> readWordsFromFile() {
        List<String> words = new java.util.ArrayList<>();
        try (Scanner fileSc = new Scanner(new File("src/word.txt"))) {
            while (fileSc.hasNextLine()) {
                words.add(fileSc.nextLine());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return words;
    }

    private void showWordsWithColor(String text, Color color) {
        StyledDocument doc = wordPane.getStyledDocument();
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, color);

        try {
            doc.remove(0, doc.getLength());
            doc.insertString(0, text, set);
        } catch (BadLocationException e) {
            e.printStackTrace();
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
                    gameStarted = true;
                    startTime = System.currentTimeMillis();

                    // Start the timer here, after the game has started
                    Timer timer = new Timer(1000, timerEvent -> updateStopwatch());
                    timer.start();
                }

                updateColorsForTypedText(typedText);

                // Check if the typed text is equal to or contains the last word
                if (typedText.contains(printedWords[printedWords.length - 1])) {
                    endWordModeGame();
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

    private void accuracyAndWPM(String[] userType, long elapsedTime) {
        StyledDocument doc = wordPane.getStyledDocument();
        int correctCharacters = 0;
        int totalCharacters = 0;
        int cursorPosition = 0;

        for (int i = 0; i < Math.min(userType.length, printedWords.length); i++) {
            String userWord = userType[i];
            String printedWord = printedWords[i];

            int minLength = Math.min(userWord.length(), printedWord.length());

            for (int j = 0; j < minLength; j++) {
                totalCharacters++;
                if (userWord.charAt(j) == printedWord.charAt(j)) {
                    correctCharacters++;
                }
            }

            totalCharacters += (userWord.length() < printedWord.length()) ? 1 : 0;
        }

        double accuracy = ((double) correctCharacters / totalCharacters) * 100;
        double wpm = (totalCharacters / 5.0) / (elapsedTime / 60000.0);

        String timeUsedInfo = String.format("Time Used: %02d:%02d", elapsedTime / 60000, (elapsedTime % 60000) / 1000);
        String mistakesInfo = String.format("Mistakes: %d", totalCharacters - correctCharacters);
        String accuracyInfo = String.format("Accuracy: %.2f%%", accuracy);
        String wpmInfo = String.format("WPM: %.2f", wpm);

        JOptionPane.showMessageDialog(null,
                String.format("Game Over!\n%s\n%s\n%s\n%s", timeUsedInfo, accuracyInfo, wpmInfo, mistakesInfo),
                "Game Over", JOptionPane.INFORMATION_MESSAGE);

        // Notify the end-game listener
        if (endGameListener != null) {
            int choice = JOptionPane.showConfirmDialog(null, "Do you want to play the WORD MODE again?", "Play Again", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                // If yes, directly show word count choice
                currentState = GameState.WORD_SELECTION; // Set to WORD_SELECTION state
                frame.getContentPane().removeAll();
                showWordModeOptions(); // Show word count choice
                frame.revalidate();
                frame.repaint();
            } else {
                // If no, return to mode selection
                currentState = GameState.MODE_SELECTION;
                frame.getContentPane().removeAll();
                gameModeSelection.showModeSelectionPage();
                frame.revalidate();
                frame.repaint();
            }
        }
    }

    private void initializeStartTime() {
        startTime = System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameModeSelection gameModeSelection = new GameModeSelection();
            WordMode wordMode = new WordMode(gameModeSelection);
        });
    }

    // EndGameListener interface for handling game termination logic
    public interface EndGameListener {
        void onEndGame();
    }
}

// enter start, enter end not auto
/* import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class WordMode {
    private enum GameState {
        MODE_SELECTION, WORD_SELECTION, PLAYING_GAME
    }

    private String[] printedWords;
    private boolean gameStarted;
    private JTextPane wordPane;
    private JTextField inputField;
    private long startTime;
    private int wordCount;
    private JLabel stopwatchLabel;
    private EndGameListener endGameListener;
    private JFrame frame;
    private GameState currentState;
    private GameModeSelection gameModeSelection; // Reference to GameModeSelection

    public WordMode(GameModeSelection gameModeSelection) {
        this.gameModeSelection = gameModeSelection;
        this.gameStarted = false;
        this.currentState = GameState.MODE_SELECTION;

        SwingUtilities.invokeLater(() -> {
            createAndShowWordModeGUI();
        });
    }

    public void setEndGameListener(EndGameListener listener) {
        this.endGameListener = listener;
    }

    private void createAndShowWordModeGUI() {
        frame = new JFrame("Word Mode");
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

        if (currentState == GameState.MODE_SELECTION) {
            showWordModeOptions();
        } else if (currentState == GameState.PLAYING_GAME) {
            initializeGame();
        }

        frame.setVisible(true);

        Timer timer = new Timer(1000, e -> updateStopwatch());
        timer.start();
    }

    private void updateStopwatch() {
        if (gameStarted) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            updateStopwatchLabel(elapsedTime / 1000); // Convert milliseconds to seconds
        } else {
            // If the game is not started, reset the stopwatch
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
        } else {
            stopwatchLabel.setText("00:00");
        }
    }

    private JButton createModeButton(String buttonText, int wordCount) {
        JButton button = new JButton(buttonText);
        button.addActionListener(e -> {
            this.wordCount = wordCount;
            currentState = GameState.PLAYING_GAME;
            frame.getContentPane().removeAll();
            initializeGame();
            loadWords();  // Load words and start the game directly
            frame.revalidate();
            frame.repaint();
        });
        return button;
    }

    public void showWordModeOptions() {
        frame.getContentPane().removeAll();

        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));

        panel.add(createModeButton("10 Words", 10));
        panel.add(createModeButton("25 Words", 25));
        panel.add(createModeButton("50 Words", 50));
        panel.add(createModeButton("100 Words", 100));

        frame.add(panel, BorderLayout.CENTER);

        frame.setSize(600, 400); // Set a fixed size
        frame.setVisible(true);
    }

    private void loadWords() {
        Random random = new Random();
        List<String> words = readWordsFromFile();

        printedWords = new String[wordCount];
        StringBuilder wordDisplay = new StringBuilder();

        for (int i = 0; i < wordCount; i++) {
            String randomWord = getRandomWord(words, random);
            printedWords[i] = randomWord;
            wordDisplay.append(randomWord).append(" ");
        }

        // Initialize the start time when loading words
        startTime = System.currentTimeMillis();

        showWordsWithColor(wordDisplay.toString(), Color.DARK_GRAY);
    }

    private String getRandomWord(List<String> words, Random random) {
        return words.get(random.nextInt(words.size()));
    }

    public void startGame() {
        // Instead of providing a default word count, show the word count options
        currentState = GameState.WORD_SELECTION;
        frame.getContentPane().removeAll();
        showWordModeOptions();
        frame.revalidate();
        frame.repaint();
    }

    private void initializeGame() {
        frame.getContentPane().removeAll();

        stopwatchLabel = new JLabel("00:00");
        stopwatchLabel.setFont(stopwatchLabel.getFont().deriveFont(20.0f));
        stopwatchLabel.setHorizontalAlignment(SwingConstants.CENTER);
        frame.add(stopwatchLabel, BorderLayout.NORTH);

        wordPane = new JTextPane();
        wordPane.setEditable(false);
        wordPane.setFont(wordPane.getFont().deriveFont(16.0f));
        frame.add(new JScrollPane(wordPane), BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.setFont(inputField.getFont().deriveFont(16.0f));

        // Add a listener to consume Enter key presses
        inputField.addActionListener(e -> {
            if (!gameStarted) {
                gameStarted = true;
                startTime = System.currentTimeMillis();
                loadWords(); // Load words when the game starts

                // Start the timer here, after the game has started
                Timer timer = new Timer(1000, timerEvent -> updateStopwatch());
                timer.start();
            } else {
                endWordModeGame();
            }
        });

        // Move the document listener setup outside the ActionListener
        inputField.getDocument().addDocumentListener(new InputDocumentListener());

        frame.add(inputField, BorderLayout.SOUTH);

        frame.revalidate();
        frame.repaint();

        gameStarted = false;

        // Initialize the start time when the game is initialized
        initializeStartTime();
    }

    private void endWordModeGame() {
        if (gameStarted) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            updateStopwatchLabel(elapsedTime);

            gameStarted = false;
            inputField.setEnabled(false);

            // Rest of the code for calculating accuracy and WPM remains unchanged
            accuracyAndWPM(inputField.getText().split(" "), elapsedTime);

            inputField.setText("");
            wordPane.setText("");

            inputField.setEnabled(true);

            // Notify the end-game listener
            if (endGameListener != null) {
                int choice = JOptionPane.showConfirmDialog(null, "Do you want to play the WORD MODE again?", "Play Again", JOptionPane.YES_NO_OPTION);

                if (choice == JOptionPane.YES_OPTION) {
                    // If yes, directly show word count choice
                    currentState = GameState.WORD_SELECTION; // Set to WORD_SELECTION state
                    frame.getContentPane().removeAll();
                    showWordModeOptions(); // Show word count choice
                    frame.revalidate();
                    frame.repaint();
                } else {
                    // If no, return to mode selection
                    currentState = GameState.MODE_SELECTION;
                    frame.getContentPane().removeAll();
                    gameModeSelection.showModeSelectionPage();
                    frame.revalidate();
                    frame.repaint();
                }
            }
        }
    }



    private List<String> readWordsFromFile() {
        List<String> words = new java.util.ArrayList<>();
        try (Scanner fileSc = new Scanner(new File("src/word.txt"))) {
            while (fileSc.hasNextLine()) {
                words.add(fileSc.nextLine());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return words;
    }

    private void showWordsWithColor(String text, Color color) {
        StyledDocument doc = wordPane.getStyledDocument();
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, color);

        try {
            doc.remove(0, doc.getLength());
            doc.insertString(0, text, set);
        } catch (BadLocationException e) {
            e.printStackTrace();
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

            if (!gameStarted && typedText.endsWith(" ")) {
                gameStarted = true;
                startGame();
                initializeStartTime();
            }

            updateColorsForTypedText(typedText);
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
    }

    private void accuracyAndWPM(String[] userType, long elapsedTime) {
        StyledDocument doc = wordPane.getStyledDocument();
        int correctCharacters = 0;
        int totalCharacters = 0;
        int cursorPosition = 0;

        for (int i = 0; i < Math.min(userType.length, printedWords.length); i++) {
            String userWord = userType[i];
            String printedWord = printedWords[i];

            int minLength = Math.min(userWord.length(), printedWord.length());

            for (int j = 0; j < minLength; j++) {
                totalCharacters++;
                if (userWord.charAt(j) == printedWord.charAt(j)) {
                    correctCharacters++;
                }
            }

            totalCharacters += (userWord.length() < printedWord.length()) ? 1 : 0;
        }

        double accuracy = ((double) correctCharacters / totalCharacters) * 100;
        double wpm = (totalCharacters / 5.0) / (elapsedTime / 60000.0);

        String timeUsedInfo = String.format("Time Used: %02d:%02d", elapsedTime / 60000, (elapsedTime % 60000) / 1000);
        String mistakesInfo = String.format("Mistakes: %d", totalCharacters - correctCharacters);
        String accuracyInfo = String.format("Accuracy: %.2f%%", accuracy);
        String wpmInfo = String.format("WPM: %.2f", wpm);

        JOptionPane.showMessageDialog(null,
                String.format("Game Over!\n%s\n%s\n%s\n%s", timeUsedInfo, accuracyInfo, wpmInfo, mistakesInfo),
                "Game Over", JOptionPane.INFORMATION_MESSAGE);

        endWordModeGame();
        }

    private void initializeStartTime() {
        startTime = System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameModeSelection gameModeSelection = new GameModeSelection();
            WordMode wordMode = new WordMode(gameModeSelection);
        });
    }

    // EndGameListener interface for handling game termination logic
    public interface EndGameListener {
        void onEndGame();
    }
}
 */

// num of characters is same then end auto