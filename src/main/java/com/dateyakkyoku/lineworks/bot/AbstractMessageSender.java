package com.dateyakkyoku.lineworks.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.time.Instant;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * 抽象化されたメッセージ送信クラス
 *
 * @author Takahiro MURAKAMI
 */
public abstract class AbstractMessageSender extends Properties {

    /**
     * LineBotの環境設定ファイル.
     */
    private String envrinmentFileName = "LineBotSetting.xml";

    public static final String MESSAGE_JSON_SWITCH = "f";
    
    public static final String SETTING_XML_SWITCH = "s";

    // ボット番号
    public static final String BOT_NO = "BOT_NO";

    // 認証キー v1.0 2.0 共用
    public static final String PRIVATE_KEY = "PRIVATE_KEY";

    // タイムリミット
    public static final String TIME_LIMIT = "TIME_LIMIT";

    // クライアントID v2.0
    public static final String CLIENT_ID = "CLIENT_ID";

    // スコープ v2.0
    public static final String SCOPE = "SCOPE";

    // クライアント署名用 v2.0
    public static final String CLIENT_SECRET = "CLIENT_SECRET";

    // サービスアカウントID v2.0
    public static final String SERVICE_ACCOUNT_ID = "SERVICE_ACCOUNT_ID";

    // アクセストークン取得先URL
    public static final String TOKEN_URL = "TOKEN_URL";
    String tokenUrl = "https://auth.worksmobile.com/oauth2/v2.0/token";

    // メッセージ送信先URL
    public static final String PUSH_URL = "PUSH_URL";
    String pushUrl = "https://www.worksapis.com/v1.0/bots/{botId}/users/{userId}/messages";

    // 外部引数を格納する
    protected Properties args = new Properties();

    public AbstractMessageSender() {

    }

    public AbstractMessageSender(String[] arguments) {

        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].contains("-")) {
                this.args.setProperty(
                        arguments[i].substring(1),
                        arguments[++i]);
            }
        }
        
        // 引数チェック
        if(this.args.containsKey(SETTING_XML_SWITCH)){
            this.envrinmentFileName = this.args.getProperty(SETTING_XML_SWITCH);
        }      
        this.loadEnvironment();
        this.exec();
    }

    public String getWorkDir() {
        String rvalue = "";
        try {

            ProtectionDomain pd = this.getClass().getProtectionDomain();
            CodeSource cs = pd.getCodeSource();
            URL location = cs.getLocation();
            java.net.URI uri = location.toURI();
            Path path = Paths.get(uri).getParent();
            rvalue = path.toString();

        } catch (URISyntaxException ex) {
            Logger.getLogger(AbstractMessageSender.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rvalue;
    }

    /**
     * ファイルから設定を取得する。
     */
    public void loadEnvironment() {
        try {
            
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "setting xml {0} loaded.", this.envrinmentFileName);

            String settingPath = this.getWorkDir() + File.separator + this.envrinmentFileName;

            File envFile = new File(settingPath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(envFile);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();

            this.setProperty(BOT_NO,
                    ((String) xPath.compile("/linebotsetting/botNo")
                            .evaluate(doc, XPathConstants.STRING)).trim());

            this.setProperty(PRIVATE_KEY,
                    ((String) xPath.compile("/linebotsetting/privateKey")
                            .evaluate(doc, XPathConstants.STRING)).trim());

            // プライベートキーが改行されている場合の対処
            String[] keys = this.getProperty(PRIVATE_KEY).split("\n");
            String pkey = "";
            for (String key : keys) {
                pkey += key.trim().replace("\\n", "\n");
            }
            this.setProperty(PRIVATE_KEY, pkey.replaceAll("\\n", "\n"));

            // clientId v2.0 設定
            this.setProperty(CLIENT_ID,
                    ((String) xPath.compile("/linebotsetting/clientId")
                            .evaluate(doc, XPathConstants.STRING)).trim());

            // client Secret v2.0 設定
            this.setProperty(CLIENT_SECRET,
                    ((String) xPath.compile("/linebotsetting/clientSercret")
                            .evaluate(doc, XPathConstants.STRING)).trim());

            // scope v2.0 設定
            this.setProperty(SCOPE,
                    ((String) xPath.compile("/linebotsetting/scope")
                            .evaluate(doc, XPathConstants.STRING)).trim());

            // scope v2.0 設定
            this.setProperty(SERVICE_ACCOUNT_ID,
                    ((String) xPath.compile("/linebotsetting/serviceAccountId")
                            .evaluate(doc, XPathConstants.STRING)).trim());

        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException ex) {
            Logger.getLogger(AbstractMessageSender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setTokenTimeLimit(Long limit) {
        this.setProperty(TIME_LIMIT, String.valueOf(limit));
    }

    public Long getTokenTimeLimit() {
        if (!this.containsKey(this)) {
            this.setProperty(TIME_LIMIT, "3000");
        }
        return Long.valueOf(this.getProperty(TIME_LIMIT));
    }

    public String getAccessTalken() throws Exception {

        String rvalue = "";

        Long startTime = Instant.now().getEpochSecond();
        Long endTime = startTime + this.getTokenTimeLimit();

        // 共通処理開始
        try {

            // 認証関連の設定より、オブジェクトをインスタンス化する
            // 認証キー
            //System.out.println(this.privateKey);
            JWK jwk = JWK.parseFromPEMEncodedObjects(
                    this.getProperty(PRIVATE_KEY));

            // ヘッダー
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            // 電文本体を格納
            JWTClaimsSet payload = new JWTClaimsSet.Builder()
                    .claim("iss", this.getProperty(CLIENT_ID))
                    .claim("sub", this.getProperty(SERVICE_ACCOUNT_ID))
                    .claim("iat", startTime.toString())
                    .claim("exp", endTime.toString())
                    .build();

            // RSA署名を格納する。
            JWSSigner signer = new RSASSASigner(jwk.toRSAKey());

            SignedJWT signedJWT = new SignedJWT(header, payload); // ヘッダと電文本体を結合する。
            signedJWT.sign(signer); //署名を行う。

            // トークンを取得する
            // HttpClientインスタンス化
            HttpClient client = new HttpClient();
            client.getHostConfiguration().setHost(tokenUrl);

            PostMethod method = new PostMethod(tokenUrl);
            method.addParameter(new Header("Content-Type", "application/x-www-form-urlencoded"));
            method.addParameter("assertion", signedJWT.serialize());
            method.addParameter("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer"); // アクセス許可種別は固定値
            method.addParameter("client_id", this.getProperty(CLIENT_ID));
            method.addParameter("client_secret", this.getProperty(CLIENT_SECRET));
            method.addParameter("scope", this.getProperty(SCOPE));

            int result = client.executeMethod(method);
            if (result != 200) {
                String errmsg = String.format("アクセストークンの取得に失敗しました。[%n]", result);
                throw new Exception(errmsg);
            }

            String szResponse;
            try (InputStreamReader ISR = new InputStreamReader(method.getResponseBodyAsStream()); BufferedReader br = new BufferedReader(ISR)) {
                szResponse = "";
                String line;
                while ((line = br.readLine()) != null) {
                    szResponse += line;
                }
            }

            if (szResponse.contains("access_token")) {

                // Responseからアクセストークンを抜き出す。
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(szResponse);
                String token = node.get("access_token").textValue();

                rvalue = token;

                // 取得後処理をキックする。トークンの状態管理などに使う。
                this.onGettingToken(token);
            }

        } catch (JOSEException | IOException ex) {
            Logger.getLogger(SimpleMessageSender.class.getName()).log(Level.SEVERE, null, ex);
        }

        return rvalue;

    }

    /**
     * ファイルパスよりJSONデータを格納して返す。
     *
     * @param filePath
     * @return JSON
     */
    public JsonStructure parse(String filePath) {
        JsonStructure rvalue = null;
        try {
            JsonReader reader = Json.createReader(
                    new InputStreamReader(
                            new FileInputStream(filePath), "UTF-8"));
            rvalue = reader.read();
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(AbstractMessageSender.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rvalue;
    }

    public void exec(String from, String[] sendTargets, String messageText) {

        for (String sendTo : sendTargets) {

            String lineWorksId = sendTo;
            Logger.getLogger(SimpleMessageSender.class.getName()).log(Level.INFO, "TARGET :{0}", lineWorksId);

            // LineWorksのメッセージ送信先URL
            String msgUrl = this.pushUrl;
            msgUrl = msgUrl.replace("{botId}", this.getProperty(BOT_NO));

            try {
                String eSendTo = URLEncoder.encode(lineWorksId, "UTF-8");
                msgUrl = msgUrl.replace("{userId}", eSendTo);
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "URL : {0}", msgUrl);

                // アクセストークンを取得する。
                String token = this.getAccessTalken();

                // メッセージを送る
                // ここからは発行するメッセージの処理に応じて個別実装する領域
                if (token.length() > 0) {
                    // HttpClientインスタンス化
                    HttpClient client = new HttpClient();
                    client.getHostConfiguration().setHost(msgUrl);

                    // 送信するメッセージのヘッダを作成
                    PostMethod pmethod = new PostMethod(msgUrl);
                    pmethod.setRequestHeader("authorization", "Bearer " + token);
                    pmethod.setRequestHeader("Content-Type", "application/json");

                    // メッセージを格納する。
                    // setBodyParameterでkey-value-pairにするLINEWORKS側でエラーを返す。
                    // なので、ResultEntityでJSONを書き込む。
                    String message = this.buildMessage(lineWorksId, messageText);

                    pmethod.setRequestEntity(new StringRequestEntity(message, "application/json", "UTF-8"));

                    int code = client.executeMethod(pmethod);
                    switch (code) {
                        case 200:
                            this.onAfterSendMessage(lineWorksId, token, "SUCCESS");
                            break;
                        case 201:
                            this.onAfterSendMessage(lineWorksId, token, "CREATED");
                            break;
                        default:
                            this.onAfterSendMessage(lineWorksId, token, "FALSE : CODE " + code);
                            break;
                    }

                    String szResponse;
                    try (InputStreamReader ISR = new InputStreamReader(pmethod.getResponseBodyAsStream()); BufferedReader br = new BufferedReader(ISR)) {
                        szResponse = "";
                        String line;
                        while ((line = br.readLine()) != null) {
                            szResponse += line;
                        }

                    }
                    Logger.getLogger(this.getClass().getName()).log(Level.INFO, "BODY : {0}", szResponse);
                }

            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(SimpleMessageSender.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(SimpleMessageSender.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(SimpleMessageSender.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void exec() {

        String jsonFilePath = this.args.getProperty(MESSAGE_JSON_SWITCH);
        JsonStructure jStr = this.parse(jsonFilePath);

        String messageText = jStr.asJsonObject().getString("message");
        JsonArray sendToList = (JsonArray) jStr.getValue("/send_to");

        String[] sendTargets = new String[sendToList.size()];
        for (int i = 0; i < sendTargets.length; i++) {
            sendTargets[i] = sendToList.getJsonObject(i).getString("id");
        }

        this.exec(SCOPE, sendTargets, messageText);

    }

    /**
     * アクセストークンを取得したときの処理. 会話の開始を記録するときなどに使用する。
     *
     * @param targetId
     * @param token
     */
    public void onGettingToken(String token) {
        Logger.getLogger(SimpleMessageSender.class.getName()).log(Level.INFO, "TOKEN : {0}", token);
    }

    /**
     * メッセージ送信後の処理. 本来は会話処理に終了フラグを立てたりする。
     *
     * @param targetId
     * @param token
     * @param message
     */
    public void onAfterSendMessage(String targetId, String Token, String result) {
        Logger.getLogger(SimpleMessageSender.class.getName()).log(Level.INFO, "RESULT : {0}", result);
    }

    /**
     * メッセージをビルドする.
     *
     * @return
     */
    public abstract String buildMessage(String targetId, String text);

}
