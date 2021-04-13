package com.dateyakkyoku.linebot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * プロパティを取得するためのクラス.
 *
 * @author Takahiro MURAKAMI
 */
public class PropertyLoader {

    private File propertyFile;

    /**
     * プロパティファイルを指定してインスタンス化.
     *
     * @param FilePath
     */
    public PropertyLoader(String FilePath) {
        this.setPropertyFilePath(FilePath);
    }

    /**
     * プロパティファイルを登録する.
     *
     * @param filePath
     */
    public void setPropertyFilePath(String filePath) {
        File f = new File(filePath);
        this.propertyFile = f;
    }

    public String getProperty(String key) {
        String rvalue = "";
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(this.propertyFile));
            rvalue = prop.getProperty(key);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PropertyLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PropertyLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rvalue;
    }

}
