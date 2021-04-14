# LINEWORKS シンプルメッセージ送信ボット

## 環境設定
LineBotSetting.sample.xml内にLINEボットの設定値をそれぞれ格納し、LineBotSetting.xmlにリネームする。

## 送信データの用意
sample.jsonを元に、送信電文ファイルを作成する。(JSONファイルはUTF-8でコードしてください)

同文を一斉配信する場合は、送信先のIDを複数記載する。

メッセージに改行を含めたいときは、全角で￥ｎを入れると自動的に変換する。

## 送信
java -jar linebot2-x.x.x-jar-with-dependencies.jar -f [送信電文ファイル]
