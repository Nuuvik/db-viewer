package com.example.dbviewer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private Stage primaryStage;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    private void login() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        try {
            TableController.setUSER(username);
            TableController.setPASSWORD(password);
            TableController.tryConnect();
            openMainStage();
        } catch (Exception e) {
            TableController.showAlert("Ошибка", "Ошибка подключения к базе данных!");
        }
    }

    private void openMainStage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
            Stage mainStage = new Stage();
            // Закрываем окно авторизации
            primaryStage.close();

            // Открываем основное окно
            TableController tableController = new TableController();
            tableController.run(mainStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}