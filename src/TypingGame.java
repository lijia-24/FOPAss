import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class TypingGame {
    private static final int TOTAL_WORDS = 10;
    private static final int INITIAL_TIME = 30;
    private int incorrectWordCount;
    private int timerDuration;
    private boolean includePunctuation;
    private String[] printedWords;
    private boolean gameStarted;
    private boolean scoreDialogShown;
    private JLabel timerLabel;
    private JTextPane wordPane;
    private JTextField inputField;
    private Timer timer;
    private int remainingTime;

    private enum GameType {SAME_TEXT, DIFFERENT_TEXT}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TypingGame typingGame = new TypingGame();
            typingGame.showSettingsPage();
        });
    }

    private void showSettingsPage() {
        SettingsPage settingsPage = new SettingsPage();
        settingsPage.createAndShowSettings();
    }

    private void createAndShowGameGUI() {
        JFrame frame = new JFrame("Type-A-Thon");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);  // Larger window size
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        try {
            Image iconImage = new ImageIcon(new File("src/typing.png").toURI().toURL()).getImage();
            frame.setIconImage(iconImage);
        } catch (IOException e) {
            e.printStackTrace();
        }

        includePunctuation = false;
        gameStarted = false;

        timerLabel = new JLabel("Press any key to start", SwingConstants.CENTER);
        timerLabel.setFont(timerLabel.getFont().deriveFont(16.0f));
        frame.add(timerLabel, BorderLayout.NORTH);

        wordPane = new JTextPane();
        wordPane.setEditable(false);
        wordPane.setFont(wordPane.getFont().deriveFont(16.0f));
        frame.add(new JScrollPane(wordPane), BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.setFont(inputField.getFont().deriveFont(16.0f));
        inputField.getDocument().addDocumentListener(new InputDocumentListener());
        frame.add(inputField, BorderLayout.SOUTH);

        frame.setVisible(true);

        loadWords();
    }

    private void loadWords() {
        Random random = new Random();
        List<String> words = readWordsFromFile();

        printedWords = new String[TOTAL_WORDS];
        StringBuilder wordDisplay = new StringBuilder();

        for (int i = 0; i < TOTAL_WORDS; i++) {
            String randomWord = getRandomWord(words, random, includePunctuation);
            printedWords[i] = randomWord;
            wordDisplay.append(randomWord).append(" ");
        }

        remainingTime = timerDuration;

        showWordsWithColor(wordDisplay.toString(), Color.DARK_GRAY);
    }

    private void startGame() {
        remainingTime = timerDuration;
        updateTimerLabel();

        if (timer != null) {
            timer.stop();
        }

        timer = new Timer(1000, new TimerActionListener());
        timer.start();

        inputField.requestFocus();
    }

    private String getRandomWord(List<String> words, Random random, boolean includePunctuation) {
        String word = words.get(random.nextInt(words.size()));

        if (includePunctuation && random.nextDouble() < 0.4) {
            String[] punctuation = {",", ".", "!", "?", "\"", "\""};
            word += punctuation[random.nextInt(punctuation.length)];
        }

        return word;
    }

    private void updateTimerLabel() {
        timerLabel.setText("Time remaining: " + remainingTime + " seconds (of " + timerDuration + " seconds)");
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
        doc.setCharacterAttributes(0, doc.getLength(), set, true);
        wordPane.setText(text);
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

    private class TimerActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameStarted) {
                if (remainingTime > 0) {
                    remainingTime--;
                    updateTimerLabel();
                } else {
                    timer.stop();
                    timer = null;
                    inputField.setEnabled(false);

                    if (!scoreDialogShown) {
                        String[] userType = inputField.getText().split(" ");
                        accuracyAndWPM(userType);

                        scoreDialogShown = true;

                        inputField.setText("");
                        wordPane.setText("");

                        gameStarted = false;
                        inputField.setEnabled(true);

                        if (askUserIfWantsToPlayAgain()) {
                            scoreDialogShown = false;

                            GameType userChoice = askUserForGameType();
                            if (userChoice == GameType.SAME_TEXT) {
                                startGameWithSameText();
                            } else {
                                showSettingsPage();
                            }
                        } else {
                            System.exit(0);
                        }
                    }
                }
            }
        }

        private boolean askUserIfWantsToPlayAgain() {
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "Do you want to play again?",
                    "Play Again",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Yes", "No"},
                    "Yes"
            );

            return choice == JOptionPane.YES_OPTION;
        }
    }

    private GameType askUserForGameType() {
        int choice = JOptionPane.showOptionDialog(
                null,
                "Do you want to play again with the same text or different text?",
                "Play Again",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"Same Text", "Different Text"},
                "Same Text"
        );

        return (choice == JOptionPane.YES_OPTION) ? GameType.SAME_TEXT : GameType.DIFFERENT_TEXT;
    }

    private void startGameWithSameText() {
        showWordsWithColor(String.join(" ", printedWords), Color.DARK_GRAY);

        remainingTime = INITIAL_TIME;
        updateTimerLabel();

        timer = new Timer(1000, new TimerActionListener());
        timer.start();
    }

    private void accuracyAndWPM(String[] userType) {
        StyledDocument doc = wordPane.getStyledDocument();
        int correctCharacters = 0;
        int totalCharacters = 0;
        int cursorPosition = 0;
        incorrectWordCount = 0;

        for (int i = 0; i < Math.min(userType.length, printedWords.length); i++) {
            String userWord = userType[i];
            String printedWord = printedWords[i];

            int minLength = Math.min(userWord.length(), printedWord.length());

            for (int j = 0; j < minLength; j++) {
                totalCharacters++;
                if (userWord.charAt(j) == printedWord.charAt(j)) {
                    correctCharacters++;
                } else {
                    incorrectWordCount++;
                }
            }

            totalCharacters += (userWord.length() < printedWord.length()) ? 1 : 0;
        }

        double accuracy = ((double) correctCharacters / totalCharacters) * 100;
        double wpm = (totalCharacters / 5.0) / (INITIAL_TIME / 60.0);

        JOptionPane.showMessageDialog(null,
                String.format("Game Over!\nAccuracy: %.2f%%\nWPM: %.2f\nMistakes: %d",
                        accuracy, wpm, incorrectWordCount),
                "Game Over", JOptionPane.INFORMATION_MESSAGE);
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
            if (!gameStarted) {
                gameStarted = true;
                startGame();
            }

            String typedText = inputField.getText();
            updateColorsForTypedText(typedText);
        }
    }

    private class SettingsPage {
        private JFrame settingsFrame;
        private JSlider timerSlider;
        private JCheckBox punctuationCheckBox;

        public void createAndShowSettings() {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            settingsFrame = new JFrame("Settings");
            settingsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            settingsFrame.setSize(400, 400);  // Larger window size
            settingsFrame.setLayout(new BorderLayout());

            JPanel settingsPanel = new JPanel(new GridLayout(4, 2, 10, 10));

            JLabel timerLabel = new JLabel("Timer Duration:");
            timerSlider = new JSlider(15, 60, INITIAL_TIME);
            timerSlider.setMajorTickSpacing(15);
            timerSlider.setMinorTickSpacing(5);
            timerSlider.setPaintTicks(true);
            timerSlider.setPaintLabels(true);

            JLabel punctuationLabel = new JLabel("Include Punctuation:");
            punctuationCheckBox = new JCheckBox();

            settingsPanel.add(timerLabel);
            settingsPanel.add(timerSlider);
            settingsPanel.add(punctuationLabel);
            settingsPanel.add(punctuationCheckBox);

            JButton okButton = new JButton("OK");
            okButton.addActionListener(e -> {
                timerDuration = timerSlider.getValue();
                includePunctuation = punctuationCheckBox.isSelected();
                settingsFrame.dispose();
                createAndShowGameGUI();
            });

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(okButton);

            settingsFrame.add(settingsPanel, BorderLayout.CENTER);
            settingsFrame.add(buttonPanel, BorderLayout.SOUTH);

            // Increase the font size of labels
            Font labelFont = timerLabel.getFont();
            timerLabel.setFont(labelFont.deriveFont(labelFont.getSize() * 1.5f));
            punctuationLabel.setFont(labelFont.deriveFont(labelFont.getSize() * 1.5f));

            // Adjust total words based on timer duration
            timerSlider.addChangeListener(e -> updateTotalWordsLabel());

            settingsFrame.setLocationRelativeTo(null);
            settingsFrame.setVisible(true);

            // Initial update of the total words label
            updateTotalWordsLabel();
        }
    } private void updateTotalWordsLabel() {
        JLabel totalWordsLabel = new JLabel("Total Words: " + calculateTotalWords(), SwingConstants.CENTER);
        Font labelFont = totalWordsLabel.getFont();
        totalWordsLabel.setFont(labelFont.deriveFont(labelFont.getSize() * 1.5f));

        JPanel settingsPanel = (JPanel) settingsFrame.getContentPane().getComponent(0);
        settingsPanel.add(totalWordsLabel, 6);  // Index 6 corresponds to the position where the label should be added
        settingsFrame.validate();
        settingsFrame.repaint();
    }

    private int calculateTotalWords() {
        int timerValue = timerSlider.getValue();
        if (timerValue <= 15) {
            return 100;
        } else if (timerValue <= 30) {
            return 200;
        } else if (timerValue <= 45) {
            return 300;
        } else {
            return 400;
        }
    }
}

// world - wor = 3 correct
// example - examplee = correct, but have 1 incorrect
    /*
private void accuracyAndWPM(String[] userType) {
    StyledDocument doc = wordPane.getStyledDocument();
    int correctCharacters = 0;
    int totalCharacters = 0;
    int cursorPosition = 0;
    incorrectWordCount = 0;

    for (int i = 0; i < Math.min(userType.length, printedWords.length); i++) {
        String userWord = userType[i];
        String printedWord = printedWords[i];

        int minLength = Math.min(userWord.length(), printedWord.length());
        int lastCorrectPosition = 0; // Track the last correct character position

        for (int j = 0; j < userWord.length(); j++) {
            totalCharacters++;
            if (j < printedWord.length() && userWord.charAt(j) == printedWord.charAt(j)) {
                correctCharacters++;
                lastCorrectPosition = j + 1; // Update last correct position
            } else {
                // Count mistakes only after the last correct position
                if (j >= lastCorrectPosition) {
                    incorrectWordCount++;
                }
            }
        }

        // Add space between words (except for the last word)
        totalCharacters += (userWord.length() < printedWord.length()) ? 1 : 0;
    }

    double accuracy = ((double) correctCharacters / totalCharacters) * 100;
    double wpm = (totalCharacters / 5.0) / (INITIAL_TIME / 60.0);

    JOptionPane.showMessageDialog(null,
            String.format("Game Over!\nAccuracy: %.2f%%\nWPM: %.2f\nIncorrect Words: %d",
                    accuracy, wpm, incorrectWordCount),
            "Game Over", JOptionPane.INFORMATION_MESSAGE);
}
*/

//?? if hello - helloo c-? inc-?
//?? if hello - hell c-? inc-?
//?? if hello - helo c-? inc-?
//?? backspace - inc, accuracy & wpm?
//?? last word - if not finish type the correct character still count correct?

