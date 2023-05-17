# LINEWORKS シンプルメッセージ送信ボット

送信相手、送信者、メッセージをファイルに記入してプログラムに渡すと、送信相手にボットからメッセージを送る。
送信相手は一度に複数指定が可能。
LINEWORKSにリマインドサービスを構築する際のバックエンドとして作成した。

* lineworksのメッセージAPIが2.0にアップグレードしたのに合わせて、内部動作を改定した。

## 環境設定
LineBotSetting.sample.xml内にLINEWORKSボットの設定値をそれぞれ格納し、LineBotSetting.xmlにリネームする。

## 送信データの用意
sample.jsonを元に、送信電文ファイルを作成する。(JSONファイルはUTF-8でコードしてください)

同文を一斉配信する場合は、送信先のIDを複数記載する。

メッセージに改行を含めたいときは、全角で￥ｎを入れると自動的に変換する。

## 送信
java -jar linebot2-x.x.x-jar-with-dependencies.jar -f [送信電文ファイル] < -s [設定XMLファイル] >

-f 電文の格納されたjsonファイルを指定

-s 設定の格納されたxmlファイルを指定する（省略するとLineBotSetting.xmlを使用する)

## 組み込みで使用する場合(LineBotSetting.xmlを使用しない場合)

SimpleMessageSenderを宣言し、LINEWORKSより提供された以下の項目をsetPropertyで指定する。

* ボット番号　BOT_NO
* 認証キー  PRIVATE_KEY
* コンシューマーキー CONSUMER_KEY;
* タイムリミット TIME_LIMIT
* クライアントID CLIENT_ID
* スコープ SCOPE
* クライアント署名用 CLIENT_SECRET
* サービスアカウントID SERVICE_ACCOUNT_ID

その後、exec(String from, String[] sendTargets, String message)を呼び出すと、メッセージをsendTargetsに指定したユーザに向けて送信する。

 
