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


// 2.1
// mistakes made?

public class TypingGame {
    private static final int TOTAL_WORDS = 10;
    private static final int INITIAL_TIME = 30;
    private int incorrectWordCount;
    private int timerDuration; // Added variable to store timer duration
    private enum GameType { SAME_TEXT, DIFFERENT_TEXT };
    private JLabel timerLabel;
    private JTextPane wordPane;
    private JTextField inputField;

    private Timer timer;
    private int remainingTime;
    private String[] printedWords;
    private boolean gameStarted;
    private boolean scoreDialogShown;


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TypingGame game = new TypingGame();
            game.createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Type-A-Thon");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        try {
            // Load the image for the icon
            Image iconImage = new ImageIcon(new File("src/typing.png").toURI().toURL()).getImage();

            // Set the icon image for the frame
            frame.setIconImage(iconImage);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Object[] options = {"15 seconds", "30 seconds", "45 seconds", "60 seconds"};
        int selectedOption = JOptionPane.showOptionDialog(
                null,
                "Choose the timer duration:",
                "Timer Duration",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        // Set timer duration based on the player's choice
        switch (selectedOption) {
            case 0:
                timerDuration = 15;
                break;
            case 1:
                timerDuration = 30;
                break;
            case 2:
                timerDuration = 45;
                break;
            case 3:
                timerDuration = 60;
                break;
            default:
                timerDuration = INITIAL_TIME; // Default to INITIAL_TIME if an unexpected option is selected
        }


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

        // Load words, but don't start the timer immediately
        loadWords();

    }

    private void startGame() {
        remainingTime = timerDuration; // Initialize remainingTime with timerDuration
        updateTimerLabel();

        // Stop the previous timer if it exists
        if (timer != null) {
            timer.stop();
        }

        // Create a new Timer object
        timer = new Timer(1000, new TimerActionListener());
        timer.start();

        inputField.requestFocus();
    }

    private void loadWords() {
        Random random = new Random();
        List<String> words = readWordsFromFile();

        printedWords = new String[TOTAL_WORDS];
        StringBuilder wordDisplay = new StringBuilder();

        // Ask the user whether to include punctuation
        int includePunctuationOption = JOptionPane.showConfirmDialog(
                null,
                "Include punctuation in words?",
                "Include Punctuation",
                JOptionPane.YES_NO_OPTION
        );

        boolean includePunctuation = (includePunctuationOption == JOptionPane.YES_OPTION);

        for (int i = 0; i < TOTAL_WORDS; i++) {
            String randomWord = getRandomWord(words, random, includePunctuation);
            printedWords[i] = randomWord;
            wordDisplay.append(randomWord).append(" ");
        }

        remainingTime = timerDuration; // Set the timer duration

        showWordsWithColor(wordDisplay.toString(), Color.DARK_GRAY);
    }

    // Helper method to get a random word with optional punctuation
    private String getRandomWord(List<String> words, Random random, boolean includePunctuation) {
        String word = words.get(random.nextInt(words.size()));

        // Include punctuation based on the user's choice
        if (includePunctuation && random.nextDouble() < 0.2) {
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
                    // If there are remaining characters in printedWord, set their color to DARK_GRAY
                    StyleConstants.setForeground(set, Color.DARK_GRAY);
                }

                doc.setCharacterAttributes(cursorPosition, 1, set, false);
                cursorPosition++;
            }

            // Add space between words (except for the last word)
            if (i < printedWords.length - 1) {
                // Check for space after the typed text
                if (charIndex < typedText.length() && typedText.charAt(charIndex) == ' ') {
                    StyleConstants.setForeground(originalSet, Color.GREEN); // Set space color to GREEN
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
                    // Stop the timer first
                    timer.stop();

                    // Set the timer object to null
                    timer = null;

                    inputField.setEnabled(false);

                    // Check if the score dialog has already been shown
                    if (!scoreDialogShown) {
                        String[] userType = inputField.getText().split(" ");
                        accuracyAndWPM(userType);

                        // Set the flag to true to indicate that the score dialog has been shown
                        scoreDialogShown = true;

                        // Clear input field and word area for the next game
                        inputField.setText("");
                        wordPane.setText("");

                        // Start a new game
                        gameStarted = false;
                        inputField.setEnabled(true);

                        // Ask the user whether to play again or not
                        if (askUserIfWantsToPlayAgain()) {
                            // Reset the scoreDialogShown flag for the new game
                            scoreDialogShown = false;

                            // Ask the user whether to play again with the same text or different text
                            GameType userChoice = askUserForGameType();
                            if (userChoice == GameType.SAME_TEXT) {
                                // Start a new game with the same text
                                startGameWithSameText();
                            } else {
                                // Start a new game with different text
                                loadWords();  // Load new words for the user to input
                                startGame();  // Create a new Timer object for a consistent timer
                            }
                        } else {
                            // User chose not to play again, exit the program
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
        // Display the same text the user just typed
        showWordsWithColor(String.join(" ", printedWords), Color.DARK_GRAY);

        // Reset the timer and initialize it to the original time
        remainingTime = INITIAL_TIME;
        updateTimerLabel();

        // Create a new Timer object
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
                startGame(); // Start the game when the user types the first character
            }

            // Get the current typed text
            String typedText = inputField.getText();
            updateColorsForTypedText(typedText);
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

