package com.dateyakkyoku.linebot;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 指定したＩＤに指定した文言を送信するシンプルなLINEWORKSボットの実装例
 *
 * @author Takahiro MURAKAMI
 */
public class SimpleMessageSender extends AbstractMessageSender {

    public SimpleMessageSender(String[] args) {
        super(args);
    }

    public SimpleMessageSender() {

    }

    /**
     * メッセージ本体からJSONを作成する。
     *
     * @param lineWorksId
     * @param messageText
     */
    @Override
    public String buildMessage(String lineWorksId, String messageText) {

        messageText = messageText.replace("￥ｎ", "\\n");

        String message = "{\"accountId\":\""
                + this.serviceAccountId
                + "\",\"content\":{\"type\":\"text\",\"text\":\""
                + messageText
                + "\"}}";

        return message;
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            new SimpleMessageSender(args);
        } else {
            System.out.println("引数 -f <jsonFile>");
        }
    }

    @Override
    public void onGettingToken(String token) {
        Logger.getLogger(SimpleMessageSender.class.getName()).log(Level.INFO, "TOKEN : {0}", token);
    }

    @Override
    public void onAfterSendMessage(String targetId, String Token, String result) {
        Logger.getLogger(SimpleMessageSender.class.getName()).log(Level.INFO, "RESULT : {0}", result);
    }

}
