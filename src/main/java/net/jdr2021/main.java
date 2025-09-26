package net.jdr2021;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.io.InputStream;

/**
 * @version 1.0
 * @Author jdr
 * @Date 2024-5-23 14:25
 * @注释
 */

public class main extends Application {

    public static Stage PrimaryStage;

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/main.fxml"));
            Parent content = loader.load();
            Scene scene = new Scene(content);

            // 添加图标
            InputStream iconStream = getClass().getResourceAsStream("/images/logo.png");
            Image icon = new Image(iconStream);
            primaryStage.getIcons().add(icon);
            primaryStage.setTitle("OSS遍历漏洞工具20251001-新版发布关注公众号：诸神之光，基于20240725-jdr2021版本");
            primaryStage.setScene(scene);
//            primaryStage.setResizable(false);
            primaryStage.sizeToScene();  //根据窗口大小调整
            primaryStage.show();


            PrimaryStage = primaryStage;
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        launch(args);
    }
}
