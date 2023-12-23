import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameModeSelection {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameModeSelection modeSelection = new GameModeSelection();
            modeSelection.showModeSelectionPage();
        });
    }

    public void showModeSelectionPage() {
        JFrame frame = createFrame("Select Game Mode");

        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));

        JButton timedButton = createModeButton("Timed Mode", e -> startTimedGame());
        JButton wordsButton = createModeButton("Words Mode", e -> startWordsGame());
        JButton quotesButton = createDisabledModeButton("Quotes Mode (Coming Soon)");

        panel.add(timedButton);
        panel.add(wordsButton);
        panel.add(quotesButton);

        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private JFrame createFrame(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);
        return frame;
    }

    private JButton createModeButton(String buttonText, ActionListener actionListener) {
        JButton button = new JButton(buttonText);
        button.addActionListener(actionListener);
        return button;
    }

    private JButton createDisabledModeButton(String buttonText) {
        JButton button = new JButton(buttonText);
        button.setEnabled(false);
        return button;
    }

    private void startTimedGame() {
        TypingGame typingGame = new TypingGame();
        typingGame.showSettingsPage();
    }

    private void startWordsGame() {
        WordMode wordMode = new WordMode(this); // Pass reference to GameModeSelection
        wordMode.setEndGameListener(() -> {
            // Handle end game logic if needed
            showModeSelectionPage();
        });
        wordMode.showWordModeOptions(); // Call showWordModeOptions on WordMode instance
    }
}

// need exit? or just the x button