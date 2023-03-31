package com.dateyakkyoku.lineworks.bot;

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
     * やっつけ仕事。
     *
     * @param lineWorksId
     * @param messageText
     */
    @Override
    public String buildMessage(String lineWorksId, String messageText) {

        messageText = messageText.replace("￥ｎ", "\\n");

        String message = "{\"accountId\":\""
                + this.getProperty(SERVICE_ACCOUNT_ID)
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
}
