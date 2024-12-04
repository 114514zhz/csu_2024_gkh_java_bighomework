import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Client {
    private static int score = 10;  // 初始分数
    private static DataInputStream input;
    private static DataOutputStream output;
    private static JFrame frame;
    private static JLabel messageLabel, scoreLabel, countdownLabel;
    private static JTextField answerField;
    private static JPanel gamePanel;
    private static String correctAnswer;
    private static String[] options;
    private static int currentMode;
    private static Timer countdownTimer;
    private static int countdown = 10;
    private static JLabel hintLabel;  // 添加一个Label用于显示首尾字母提示
    private static JPanel optionsPanel; // 用于显示选项的面板
    private static File masteredFile = new File("已掌握单词.txt");
    private static File unmasteredFile = new File("未掌握单词.txt");
    private static String chinese;
    private static String english;
    private static String answertype;
    public static void main(String[] args) {
        connectToServer();
        SwingUtilities.invokeLater(() -> createMainMenu());
    }

    private static void connectToServer() {
        try {
            Socket socket = new Socket("100.70.84.176", 12345);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createMainMenu() {
        frame = new JFrame("考研单词记忆游戏");
        frame.setSize(800, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // 设置整体布局居中
        gbc.insets = new Insets(10, 10, 10, 10); // 添加间距
        gbc.anchor = GridBagConstraints.CENTER; // 居中对齐

        // 创建标签并添加到窗口
        JLabel label = new JLabel("请选择模式: ");
        label.setFont(new Font("SansSerif", Font.BOLD, 20)); // 设置标签字体
        gbc.gridx = 0;
        gbc.gridy = 0;
        frame.add(label, gbc);

        // 创建两个按钮并添加到窗口
        JButton mode1Button = new JButton("中文补齐英文");
        mode1Button.setPreferredSize(new Dimension(200, 50)); // 设置按钮尺寸
        mode1Button.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 1;
        frame.add(mode1Button, gbc);

        JButton mode2Button = new JButton("英文选择中文");
        mode2Button.setPreferredSize(new Dimension(200, 50));
        mode2Button.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 2;
        frame.add(mode2Button, gbc);

        // 为按钮添加事件监听器
        mode1Button.addActionListener(e -> selectMode(1));
        mode2Button.addActionListener(e -> selectMode(2));

        frame.setLocationRelativeTo(null); // 窗口居中显示
        frame.setVisible(true);
    }

    private static void selectMode(int mode) {
        currentMode = mode;
        frame.getContentPane().removeAll(); // 清除主菜单
        frame.repaint();
        gamePanel = new JPanel();
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
        frame.add(gamePanel);

        scoreLabel = new JLabel("当前分数: " + score);
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 20)); // 设置标签字体
        gamePanel.add(scoreLabel);

        countdownLabel = new JLabel("倒计时: " + countdown);
        countdownLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        gamePanel.add(countdownLabel);

        messageLabel = new JLabel("加载中");
        messageLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        gamePanel.add(messageLabel);

        hintLabel = new JLabel("");  // 初始化首尾字母提示Label
        hintLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        gamePanel.add(hintLabel);

        answerField = new JTextField(20);
        answerField.addActionListener(e -> checkAnswer());
        gamePanel.add(answerField);

        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        gamePanel.add(optionsPanel); // 将选项面板加入到游戏面板中

        frame.revalidate();
        frame.repaint();
        // 确保光标自动出现在 answerField 中
        answerField.requestFocusInWindow();

        if (mode == 1) {
            playCToE();
        } else {
            playEToC();
        }
    }

    private static void playCToE() {
        try {
            output.writeInt(1);  // 发送模式选择到服务器
            chinese = input.readUTF();
            String hint = input.readUTF();
            correctAnswer = input.readUTF();
            english=correctAnswer;

            // 更新界面显示
            messageLabel.setText("请根据中文补齐英文: " + chinese);
            hintLabel.setText(hint);  // 显示首尾字母提示
            countdown = 10; // 重置倒计时
            countdownLabel.setText("倒计时: " + countdown);

            startCountdown();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void playEToC() {
        try {
            // 发送当前模式给服务器
            output.writeInt(2);
            english = input.readUTF();
            options = new String[4];
            for (int i = 0; i < 4; i++) {
                options[i] = input.readUTF();
            }
            chinese = input.readUTF();  // 正确答案
            for (int i = 0; i < 4; i++){
                if (chinese.equals(options[i])){
                    correctAnswer = String.valueOf((char) ('A' + i));  //正确答案的字母（A、B、C、D）
                }
            }
            // 更新界面显示
            messageLabel.setText("请选择正确的中文: " + english);
            countdown = 10;
            countdownLabel.setText("倒计时: " + countdown);
            startCountdown();
            // 清空旧的选项并生成新的选项
            optionsPanel.removeAll();
            // 显示选项
            for (int i = 0; i < options.length; i++) {
                JLabel optionLabel = new JLabel((char) ('A' + i) + ": " + options[i]);
                optionLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
                optionsPanel.add(optionLabel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startCountdown() {
        countdownTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                countdown--;
                countdownLabel.setText("倒计时: " + countdown);
                if (countdown == 0) {
                    countdownTimer.stop();
                    // 没有回答时扣1分，并显示正确答案
                    score -= 1;
                    showResult("您没有回答，正确答案是: " + correctAnswer);
                    answertype="超时未回答";
                    storeWord(unmasteredFile,answertype);
                }
            }
        });
        countdownTimer.start();
    }

    private static void checkAnswer() {
        String answer = answerField.getText().trim().toUpperCase();  // 用户输入的字母（A、B、C、D）
        // 答对，存储到已掌握单词
        if (answer.equals(correctAnswer)) {
            score++;
            answertype="回答正确";
            storeWord(masteredFile,answertype);
            showResult("恭喜回答正确！");
        } else {
            // 答错，存储到未掌握单词
            score -= 2;
            answertype="回答错误";
            storeWord(unmasteredFile,answertype);
            if (score < 0) {
                score = 0;
            }
            showResult("回答错误，正确答案是: " + correctAnswer);
        }

    }

    private static void storeWord(File file,String answertype) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            // 如果文件不存在，创建文件
            if (!file.exists()) {
                file.createNewFile();
            }
            writer.write(english+"  "+chinese+""+answertype);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void showResult(String resultMessage) {
        // 停止倒计时
        countdownTimer.stop();
        // 显示结果对话框
        JOptionPane.showMessageDialog(null, resultMessage, "游戏结果", JOptionPane.INFORMATION_MESSAGE);

        // 更新分数和清空输入框
        scoreLabel.setText("当前分数: " + score);
        answerField.setText("");

        // 延迟一段时间后继续游戏
        Timer delayTimer = new Timer(1000, e -> {
            if (score > 0) {
                // 继续游戏
                if (currentMode == 1) {
                    playCToE();
                } else {
                    playEToC();
                }
            } else {
                JOptionPane.showMessageDialog(null, "游戏结束，您的分数为0！", "游戏结束", JOptionPane.INFORMATION_MESSAGE);
                //关闭当前页面，回到主界面
                frame.getContentPane().removeAll();
                frame.repaint();
                frame.setVisible(false);
                score=10;
                createMainMenu();
            }
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

}

