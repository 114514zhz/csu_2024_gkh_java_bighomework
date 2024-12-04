import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static List<String[]> wordList = new ArrayList<>(); // 存储所有的词汇及其中文意思
    public static void main(String[] args) {
        loadWords();
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("服务器已启动，等待客户端连接...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接成功！");
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadWords() {
        try (BufferedReader reader = new BufferedReader(new FileReader("words.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 2); // 根据空格分割
                if (parts.length == 2) {
                    wordList.add(parts); // 将英文和中文存储在列表中
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                DataInputStream input = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            while (true) {
                int mode = input.readInt(); // 读取客户端请求的游戏模式
                if (mode == 1) {
                    CToE(output);
                } else if (mode == 2) {
                    EToC(output);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void CToE(DataOutputStream output) throws IOException {
        Random random = new Random();
        String[] word = wordList.get(random.nextInt(wordList.size())); // 随机选择一个单词
        String chinese = word[1]; // 中文描述
        String english = word[0]; // 英文单词
        String hint = english.charAt(0) + " _ ".repeat(english.length()-2) + english.charAt(english.length() - 1); // 提示信息
        output.writeUTF(chinese);  // 发送中文
        output.writeUTF(hint);     // 发送提示
        output.writeUTF(english);  // 发送正确答案
    }

    private static void EToC(DataOutputStream output) throws IOException {
        Random random = new Random();
        String[] word = wordList.get(random.nextInt(wordList.size())); // 随机选择一个单词
        String english = word[0]; // 英文单词
        String correctChinese = word[1]; // 正确的中文翻译
        // 随机选择四个中文选项，其中一个正确，三个错误
        List<String> options = new ArrayList<>();
        options.add(correctChinese);
        while (options.size() < 4) {
            String[] wrongWord = wordList.get(random.nextInt(wordList.size()));
            String wrongChinese = wrongWord[1];
            if (!options.contains(wrongChinese)) {
                options.add(wrongChinese);
            }
        }
        Collections.shuffle(options); // 打乱选项顺序
        // 发送问题及选项
        output.writeUTF(english); // 发送英文单词
        for (String option : options) {
            output.writeUTF(option); // 发送每个选项
        }
        output.writeUTF(correctChinese); // 发送正确答案
    }
}
