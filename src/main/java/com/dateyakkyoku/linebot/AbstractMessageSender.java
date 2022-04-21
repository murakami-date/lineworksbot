package com.dateyakkyoku.linebot;

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
import javax.json.JsonValue;
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
public abstract class AbstractMessageSender {

    /**
     * LineBotの環境設定ファイル.
     */
    private String envrinmentFileName = "LineBotSetting.xml";

    // API ID
    String apiId = "";

    // サーバID
    String serverId = "";

    // ボット番号
    String botNo = "";

    // 認証キー v1.0 2.0 共用
    String privateKey = "";

    // コンシューマーキー
    String consumerKey = "";

    // タイムリミット
    Long timeLimit = 3000L;

    // クライアントID v2.0
    String clientId = "";

    // スコープ v2.0
    String scope = "";

    // クライアント署名用 v2.0
    String clientSecret = "";

    // サービスアカウントID v2.0
    String serviceAccountId = "";

    // アクセストークン取得先URL
    //String tokenUrl = "https://auth.worksmobile.com/b/{API ID}/server/token";
    String tokenUrl = "https://auth.worksmobile.com/oauth2/v2.0/token";

    // メッセージ送信先URL
    //String pushUrl = "https://apis.worksmobile.com/r/{API ID}/message/v1/bot/{botNo}/message/push";
    String pushUrl = "https://www.worksapis.com/v1.0/bots/{botId}/users/{userId}/messages";

    protected Properties properties = new Properties();

    public AbstractMessageSender(String[] args) {

        this.loadEnvironment();

        for (int i = 0; i < args.length; i++) {
            if (args[i].contains("-")) {
                properties.setProperty(
                        args[i].substring(1),
                        args[++i]);
            }
        }
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

    private void loadEnvironment() {
        try {
            String settingPath = this.getWorkDir() + File.separator + this.envrinmentFileName;

            File envFile = new File(settingPath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(envFile);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();

            this.apiId = ((String) xPath.compile("/linebotsetting/apiId")
                    .evaluate(doc, XPathConstants.STRING)).trim();

            this.serverId = ((String) xPath.compile("/linebotsetting/serverId")
                    .evaluate(doc, XPathConstants.STRING)).trim();

            this.botNo = ((String) xPath.compile("/linebotsetting/botNo")
                    .evaluate(doc, XPathConstants.STRING)).trim();

            this.privateKey = ((String) xPath.compile("/linebotsetting/privateKey")
                    .evaluate(doc, XPathConstants.STRING)).trim();
            String[] keys = this.privateKey.split("\n");
            this.privateKey = "";
            for (String key : keys) {
                this.privateKey += key.trim().replace("\\n", "\n");
            }
            this.privateKey = this.privateKey.replaceAll("\\n", "\n");

            this.consumerKey = ((String) xPath.compile("/linebotsetting/consumerKey")
                    .evaluate(doc, XPathConstants.STRING)).trim();

            //this.timeLimit = Long.parseLong(((String) xPath.compile("/linebotsetting/timeLimit")
            //        .evaluate(doc, XPathConstants.STRING)).trim());

            // clientId v2.0 設定
            this.clientId = ((String) xPath.compile("/linebotsetting/clientId")
                    .evaluate(doc, XPathConstants.STRING)).trim();

            // client Secret v2.0 設定
            this.clientSecret = ((String) xPath.compile("/linebotsetting/clientSercret")
                    .evaluate(doc, XPathConstants.STRING)).trim();

            // scope v2.0 設定
            this.scope = ((String) xPath.compile("/linebotsetting/scope")
                    .evaluate(doc, XPathConstants.STRING)).trim();

            // scope v2.0 設定
            this.serviceAccountId = ((String) xPath.compile("/linebotsetting/serviceAccountId")
                    .evaluate(doc, XPathConstants.STRING)).trim();

        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException ex) {
            Logger.getLogger(AbstractMessageSender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setProperty(String key, String value) {
        this.properties.setProperty(key, value);
    }

    public String getProperty(String key) {
        return this.properties.getProperty(key);
    }

    public AbstractMessageSender() {

    }

    public void setTokenTimeLimit(Long limit) {
        this.timeLimit = limit;
    }

    public Long getTokenTimeLimit() {
        return this.timeLimit;
    }

    public String getAccessTalken() throws Exception {

        String rvalue = "";

        Long startTime = Instant.now().getEpochSecond();
        Long endTime = startTime + timeLimit;

        //tokenUrl = tokenUrl.replace("{API ID}", apiId);
        // 共通処理開始
        try {

            // 認証関連の設定より、オブジェクトをインスタンス化する
            // 認証キー
            //System.out.println(this.privateKey);
            JWK jwk = JWK.parseFromPEMEncodedObjects(this.privateKey);

            // ヘッダー
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            // 電文本体を格納
            JWTClaimsSet payload = new JWTClaimsSet.Builder()
                    .claim("iss", this.clientId)
                    .claim("sub", this.serviceAccountId)
                    .claim("iat", startTime.toString())
                    .claim("exp", endTime.toString())
                    .build();

            // RSA署名を格納する。
            JWSSigner signer = new RSASSASigner(jwk.toRSAKey());

            SignedJWT signedJWT = new SignedJWT(header, payload); // ヘッダと電文本体を結合する。
            signedJWT.sign(signer); //署名を行う。
            //System.out.println(signedJWT.serialize());

            // トークンを取得する
            // HttpClientインスタンス化
            HttpClient client = new HttpClient();
            client.getHostConfiguration().setHost(tokenUrl);

            PostMethod method = new PostMethod(tokenUrl);
            method.addParameter(new Header("Content-Type", "application/x-www-form-urlencoded"));
            method.addParameter("assertion", signedJWT.serialize());
            method.addParameter("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer"); // アクセス許可種別は固定値
            method.addParameter("client_id", this.clientId);
            method.addParameter("client_secret", this.clientSecret);
            method.addParameter("scope", this.scope);

            int result = client.executeMethod(method);
            if (result != 200) {
                throw new Exception("アクセストークンの取得に失敗しました。");
            }

            String szResponse;
            try ( InputStreamReader ISR = new InputStreamReader(method.getResponseBodyAsStream());  BufferedReader br = new BufferedReader(ISR)) {
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

    public void exec() {

        String jsonFilePath = this.getProperty("f");
        JsonStructure jStr = this.parse(jsonFilePath);

        String messageText = jStr.asJsonObject().getString("message");
        JsonArray sendToList = (JsonArray) jStr.getValue("/send_to");

        for (JsonValue sendTo : sendToList) {

            String lineWorksId = sendTo.asJsonObject().getString("id");
            Logger.getLogger(SimpleMessageSender.class.getName()).log(Level.INFO, "TARGET :{0}", lineWorksId);

            // LineWorksのメッセージ送信先URL
            String msgUrl = this.pushUrl;
//            pushUrl = pushUrl.replace("{API ID}", apiId);
            msgUrl = msgUrl.replace("{botId}", botNo);

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
                    pmethod.setRequestHeader("Content-Type","application/json");

                    // メッセージを格納する。
                    // setBodyParameterでkey-value-pairにするLINEWORKS側でエラーを返す。
                    // なので、ResultEntityでJSONを書き込む。
                    String message = this.buildMessage(lineWorksId, messageText);

                    //pmethod.setRequestEntity(new StringRequestEntity(message, "application/json", "UTF-8"));
                    pmethod.setRequestEntity(new StringRequestEntity(message, "application/json", "UTF-8"));

                    int code = client.executeMethod(pmethod);
                    if (code == 200) {
                        this.onAfterSendMessage(lineWorksId, token, "SUCCESS");
                    } else {
                        this.onAfterSendMessage(lineWorksId, token, "FALSE : CODE " + code);
                    }

                    String szResponse;
                    try ( InputStreamReader ISR = new InputStreamReader(pmethod.getResponseBodyAsStream());  BufferedReader br = new BufferedReader(ISR)) {
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

    /**
     * アクセストークンを取得したときの処理. 会話の開始を記録するときなどに使用する。
     *
     * @param targetId
     * @param token
     */
    public abstract void onGettingToken(String token);

    /**
     * メッセージ送信後の処理. 本来は会話処理に終了フラグを立てたりする。
     *
     * @param targetId
     * @param token
     * @param message
     */
    public abstract void onAfterSendMessage(String targetId, String token, String message);

    /**
     * メッセージをビルドする.
     *
     * @return
     */
    public abstract String buildMessage(String targetId, String text);

}
