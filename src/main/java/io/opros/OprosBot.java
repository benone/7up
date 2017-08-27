package io.opros;


import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionTimeoutException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.opros.OprosBot.State.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class OprosBot extends TelegramLongPollingBot {
    enum State { NOT_REGISTERED, WAITING_QUIZ, IN_PROGRESS_QUIZ}
    enum Type { USER, COMPANY}

    private static Map<String, UserData> users = new HashMap<>();
    private static Set<Poll> polls = new HashSet<Poll>();

    static Web3j web3 = Web3j.build(new HttpService());  // defaults to http://localhost:8545/
    static Credentials credentials;



    static {
        users.put("northcapen", new UserData(NOT_REGISTERED));
        users.put("melsrose", new UserData(NOT_REGISTERED));
        users.put("gex194", new UserData(NOT_REGISTERED));
        users.put("Anikanaum", new UserData(NOT_REGISTERED));
        users.put("Библиоглобус", new UserData(Type.COMPANY, "123", "/var/folders/35/qblq10fs0xd0dglld1rz8my40000gn/T/ethereum_dev_mode/keystore/UTC--2017-08-27T00-29-54.608326416Z--c3878f6010777abe4296de6208c2ca46ed9ccd8e"));


        Question question = new Question(1L, "Хотели ли вы провести свой отпуск с любимым котиком? (Да/Нет/Я отдыхаю только с котиком)", asList("Да", "Нет", "Я отдыхаю только с котиком"));
        Question question2 = new Question(2L, "Помогает ли вам алкоголь лучше понимать иностранный язык? (Немного/Средне/Существенно)", asList("Немного", "Средне", "Существенно"));
        Question question3 = new Question(3L, "Было ли желание у вас проветрить салон самолета? (Было/Не было/Как-то я почти открыл иллюминатор)", asList("Было", "Не было", "Как-то я почти открыл иллюминатор"));
        Question question4 = new Question(4L, "Какой вид транспорта вы предпочтете в путешествиях? (Верблюд/Автостоп/Кривая коза)", asList("Верблюд", "Автостоп", "Кривая коза"));
        Poll poll = new Poll(1L, "Важный опрос", asList(question, question2, question3, question4));

        poll.author = "Библиоглобус";
        poll.price = new BigDecimal("0.05");
        users.get("northcapen").accountNumber = "0x5e2E5b93BCa911C2e3A275B7b43eBbBf4ca280ed";

        polls.add(poll);

        try {
            credentials = WalletUtils.loadCredentials(users.get("Библиоглобус").password, users.get("Библиоглобус").privateKeyFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CipherException e) {
            e.printStackTrace();
        }


    }

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();
            String result = null;
            String userName = update.getMessage().getFrom().getUserName();
            final UserData userData = users.get(userName);
            State state = userData.state;

            switch (state) {
                case NOT_REGISTERED:
                    if (message_text.equals("Готово")) {
                        users.put(userName, new UserData(WAITING_QUIZ));
                        result = "Спасибо, вы зарегистрированы. Вам создан кошелек: " + userData.accountNumber + ". Начните опрос (/start_quiz)";
                    } else {
                        result = "Наш чат-бот Opros.io приветствует вас. Теперь вы можете, не покидая телеграм, делиться своим мнением и заодно подзаработать. Чтобы начать, зарегиструйтесь по ссылке (https://github.com/gex194/7up) и напишите 'Готово', когда закончите";

                    }
                    break;

                case WAITING_QUIZ:
                    if (message_text.equals("/start_quiz")) {
                        ArrayList<Poll> polls = new ArrayList<>(OprosBot.polls);
                        Poll poll = polls.get(0);
                        userData.pollId = poll.id;
                        userData.questionId = 1L;
                        userData.state = IN_PROGRESS_QUIZ;

                        result = "Это опрос от компании " + poll.author + " за " + poll.price + " ETH. " + poll.getQuestion(userData.questionId).text;
                    } else {
                        result = "Нужно начать опрос (/start_quiz)";
                    }
                    break;

                case IN_PROGRESS_QUIZ:
                    Poll poll = polls.stream().filter(p -> Objects.equals(p.id, userData.pollId)).collect(toList()).get(0);
                    Question question = poll.getQuestion(userData.questionId);
                    if(userData.questionId  == poll.questions.size()) {
                        userData.state = State.WAITING_QUIZ;
                        new Thread(() -> {
                            try {
                                TransactionReceipt transactionReceipt = Transfer.sendFunds(
                                        web3, credentials, userData.accountNumber, poll.price, Convert.Unit.ETHER);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (TransactionTimeoutException e) {
                                e.printStackTrace();
                            }
                        }).start();

                        result = "Спасибо за опрос. Ваши деньги (" + poll.price + " ETH) очень скоро придут на Ваш счет!";
                    }
                    else if (question.answer.contains(message_text)) {
                        userData.questionId = userData.questionId + 1;
                        result = poll.getQuestion(userData.questionId).text;
                    } else {
                        result = "Вот сейчас не совсем понял. Попробуем еще раз: " + question.text;
                    }

                    break;
            }


            long chat_id = update.getMessage().getChatId();

            SendMessage message = new SendMessage() // Create a message object object
                    .setChatId(chat_id)
                    .setText(result);
            try {
                sendMessage(message); // Sending our message object to user
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }



    public String getBotUsername() {
        return "Opros.io";
    }

    public String getBotToken() {
        return "367476042:AAFwjhhdDEUez704wnHHSNk_rPcY61fKXhk";
    }
}

class UserData {
    OprosBot.Type type;

    OprosBot.State state;
    Long pollId;
    Long questionId;

    String password;
    String privateKeyFile;
    String accountNumber;

    public UserData(OprosBot.State state) {
        this.state = state;
    }

    public UserData(OprosBot.Type type, String password, String privateKeyFile) {
        this.type = type;
        this.password = password;
        this.privateKeyFile = privateKeyFile;
    }
}

class Poll {
    Long id;
    String name;
    BigDecimal price;
    String author;
    List<Question> questions = new ArrayList<>();

    public Poll(Long id, String name, List<Question> questions) {
        this.id = id;
        this.name = name;
        this.questions = questions;
    }

    Question getQuestion(Long index) {
        return questions.stream().filter(q -> q.id.equals(index)).collect(toList()).get(0);
    }
}

class Question {
    Long id;
    String text;
    List<String> answer = new ArrayList();

    public Question(Long id, String text, List<String> answer) {
        this.id = id;
        this.text = text;
        this.answer = answer;
    }
}
