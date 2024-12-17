package com.example.dbviewer;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TableController {
    private static String DB_URL = "jdbc:postgresql://localhost:5432/project1"; // URL вашей базы данных
    private static String USER; // Имя пользователя
    private static String PASSWORD; // Пароль

    private Map<String, TableView<List<Object>>> tableViews = new HashMap<>();
    private TabPane tabPane;

    public void run(Stage primaryStage) {
        tabPane = new TabPane();

        // Изначально загружаем все таблицы из базы данных
        loadTables();

        // Поле для ввода SQL-запроса и кнопка для его выполнения
        TextField queryField = new TextField();
        queryField.setPrefWidth(300);
        queryField.setPromptText("Введите SQL запрос");
        Button executeButton = new Button("Выполнить");
        executeButton.setOnAction(e -> executeQuery(queryField.getText()));

        // Кнопка обновления всех таблиц и списка таблиц
        Button refreshButton = new Button("Обновить таблицы");
        refreshButton.setOnAction(e -> refreshTables());

        Button addButton = new Button("Добавить");
        addButton.setOnAction(e -> addRow());

        Button updateButton = new Button("Обновить запись");
        updateButton.setOnAction(e -> updateRow());

        Button deleteButton = new Button("Удалить");
        deleteButton.setOnAction(e -> deleteRow());


        HBox queryBox = new HBox(10, queryField, executeButton, refreshButton, addButton, updateButton, deleteButton);

        queryBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(queryBox);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setTitle("Database Viewer with SQL Executor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    static void tryConnect() {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    // Метод для загрузки всех таблиц в начале
    private void loadTables() {
        List<String> tables = getTablesFromDatabase();
        for (String tableName : tables) {
            addTableTab(tableName);
        }
    }

    // Метод для получения списка таблиц из базы данных
    private List<String> getTablesFromDatabase() {
        List<String> tables = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             ResultSet resultSet = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (resultSet.next()) {
                tables.add(resultSet.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tables;
    }

    // Метод для создания и добавления вкладки для таблицы
    private void addTableTab(String tableName) {
        if (tableViews.containsKey(tableName)) return;

        TableView<List<Object>> tableView = createTableViewForTable(tableName);
        tableViews.put(tableName, tableView);

        Tab tab = new Tab(tableName);
        tab.setContent(tableView);
        tabPane.getTabs().add(tab);

        // Обработка закрытия вкладки
        tab.setOnClosed(event -> tableViews.remove(tableName));
    }

    // Метод для создания TableView с данными из конкретной таблицы
    private TableView<List<Object>> createTableViewForTable(String tableName) {
        TableView<List<Object>> tableView = new TableView<>();
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName)) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Создаем столбцы
            tableView.getColumns().clear();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                TableColumn<List<Object>, Object> column = new TableColumn<>(columnName);
                final int columnIndex = i - 1;
                column.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue().get(columnIndex)));
                tableView.getColumns().add(column);
            }

            // Заполняем данные
            tableView.getItems().clear();
            while (resultSet.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(resultSet.getObject(i));
                }
                tableView.getItems().add(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tableView;
    }

    // Метод для выполнения SQL запроса и отображения результата
    private void executeQuery(String query) {
        if (query.trim().isEmpty()) return;

        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {

            if (query.trim().toLowerCase().startsWith("select")) {
                // Выполнение SELECT запроса
                ResultSet resultSet = statement.executeQuery(query);
                TableView<List<Object>> tableView = new TableView<>();
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                tableView.getColumns().clear();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    TableColumn<List<Object>, Object> column = new TableColumn<>(columnName);
                    final int columnIndex = i - 1;
                    column.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue().get(columnIndex)));
                    tableView.getColumns().add(column);
                }

                while (resultSet.next()) {
                    List<Object> row = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(resultSet.getObject(i));
                    }
                    tableView.getItems().add(row);
                }

                Tab resultTab = new Tab("Результат запроса");
                resultTab.setContent(tableView);
                tabPane.getTabs().add(resultTab);
                tabPane.getSelectionModel().select(resultTab);

            } else {
                int rowsAffected = statement.executeUpdate(query);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Запрос выполнен");
                alert.setHeaderText(null);
                alert.setContentText("Запрос успешно выполнен. Затронуто строк: " + rowsAffected);
                alert.showAndWait();

                // Обновляем данные в таблице, если это добавление, обновление или удаление
                Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                if (selectedTab != null) {
                    refreshTable(selectedTab.getText());
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка при выполнении запроса: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    // Метод для обновления данных всех таблиц и вкладок
    private void refreshTables() {
        List<String> currentTables = getTablesFromDatabase();

        // Добавляем новые таблицы, если они появились в БД
        for (String tableName : currentTables) {
            if (!tableViews.containsKey(tableName)) {
                addTableTab(tableName);
            }
        }

        // Удаляем вкладки для таблиц, которые были удалены из БД
        tableViews.keySet().removeIf(tableName -> {
            if (!currentTables.contains(tableName)) {
                tabPane.getTabs().removeIf(tab -> tab.getText().equals(tableName));
                return true;
            }
            return false;
        });

        // Обновляем данные в существующих таблицах
        for (String tableName : currentTables) {
            refreshTable(tableName);
        }
    }

    // Метод для обновления данных в конкретной таблице
    private void refreshTable(String tableName) {
        TableView<List<Object>> tableView = tableViews.get(tableName);
        if (tableView != null) {
            tableView.getItems().clear();
            try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName)) {

                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (resultSet.next()) {
                    List<Object> row = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(resultSet.getObject(i));
                    }
                    tableView.getItems().add(row);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void addRow() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            showAlert("Ошибка", "Не выбрана таблица для добавления записи!");
            return;
        }

        TableView<List<Object>> tableView = tableViews.get(selectedTab.getText());
        if (tableView == null) {
            showAlert("Ошибка", "Не удалось найти таблицу для текущей вкладки!");
            return;
        }

        String tableName = selectedTab.getText();
        Map<String, String> columnTypes = getColumnTypes(tableName); // Получаем типы столбцов

        // Получаем список столбцов и исключаем автоинкрементные поля
        List<String> columnNames = getEditableColumnNames(tableName);
        if (columnNames.isEmpty()) {
            showAlert("Ошибка", "Нет доступных для ввода столбцов в таблице!");
            return;
        }

        Dialog<List<Object>> dialog = new Dialog<>();
        dialog.setTitle("Добавить запись");
        dialog.setHeaderText("Введите значения для новой записи:");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        List<TextField> fields = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            grid.add(new Label(columnName + ":"), 0, i);
            TextField textField = new TextField();
            textField.setPromptText(columnName);
            grid.add(textField, 1, i);
            fields.add(textField);
        }

        dialog.getDialogPane().setContent(grid);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                List<Object> values = new ArrayList<>();
                for (TextField field : fields) {
                    values.add(field.getText());
                }
                return values;
            }
            return null;
        });

        Optional<List<Object>> result = dialog.showAndWait();

        result.ifPresent(values -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
                String insertQuery = generateInsertQuery(tableName, columnNames);
                try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                    for (int i = 0; i < values.size(); i++) {
                        String columnName = columnNames.get(i);
                        String columnType = columnTypes.get(columnName); // Получаем тип столбца

                        // В зависимости от типа столбца обрабатываем значение
                        if (columnType.equalsIgnoreCase("INTEGER") || columnType.equalsIgnoreCase("INT") || columnType.equalsIgnoreCase("INT4")) {
                            try {
                                statement.setInt(i + 1, Integer.parseInt(values.get(i).toString()));
                            } catch (NumberFormatException e) {
                                showAlert("Ошибка", "Значение для столбца '" + columnName + "' должно быть целым числом.");
                                return;
                            }
                        } else if (columnType.equalsIgnoreCase("VARCHAR") || columnType.equalsIgnoreCase("CHAR")) {
                            statement.setString(i + 1, values.get(i).toString());
                        } else if (columnType.equalsIgnoreCase("TEXT")) {
                            statement.setString(i + 1, values.get(i).toString());
                        } else {
                            // Обработка других типов, если необходимо
                            statement.setObject(i + 1, values.get(i));
                        }
                    }
                    int affectedRows = statement.executeUpdate();

                    if (affectedRows > 0) {
                        refreshTables();
                        showAlert("Успех", "Запись успешно добавлена.");
                    } else {
                        showAlert("Ошибка", "Не удалось добавить запись.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Ошибка", "Произошла ошибка при добавлении записи: " + e.getMessage());
            }
        });
    }

    private List<String> getEditableColumnNames(String tableName) {
        List<String> columnNames = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String isAutoIncrement = columns.getString("IS_AUTOINCREMENT");
                    if (!"YES".equalsIgnoreCase(isAutoIncrement)) {
                        columnNames.add(columnName);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось получить список столбцов: " + e.getMessage());
        }
        return columnNames;
    }

    private String generateInsertQuery(String tableName, List<String> columnNames) {
        String columns = String.join(", ", columnNames);
        String placeholders = String.join(", ", Collections.nCopies(columnNames.size(), "?"));
        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
    }


    private void updateRow() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            showAlert("Ошибка", "Не выбрана таблица для обновления записи!");
            return;
        }

        TableView<List<Object>> tableView = tableViews.get(selectedTab.getText());
        if (tableView == null) {
            showAlert("Ошибка", "Не удалось найти таблицу для текущей вкладки!");
            return;
        }

        List<Object> selectedRow = tableView.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showAlert("Ошибка", "Не выбрана строка для обновления!");
            return;
        }

        String tableName = selectedTab.getText();
        List<String> columnNames = getEditableColumnNames(tableName);
        Map<String, String> columnTypes = getColumnTypes(tableName); // Получаем типы столбцов

        Dialog<List<Object>> dialog = new Dialog<>();
        dialog.setTitle("Обновить запись");
        dialog.setHeaderText("Редактируйте значения для выбранной записи:");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        List<TextField> fields = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            grid.add(new Label(columnName + ":"), 0, i);

            if (selectedRow.get(i + 1) != null) {
                TextField textField = new TextField(selectedRow.get(i + 1).toString()); // +1 пропускает `id`
                textField.setPromptText(columnName);
                grid.add(textField, 1, i);
                fields.add(textField);
            } else {
                TextField textField = new TextField(""); // +1 пропускает `id`
                textField.setPromptText(columnName);
                grid.add(textField, 1, i);
                fields.add(textField);
            }
        }

        dialog.getDialogPane().setContent(grid);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                List<Object> newValues = new ArrayList<>();
                for (TextField field : fields) {
                    newValues.add(field.getText());
                }
                return newValues;
            }
            return null;
        });

        Optional<List<Object>> result = dialog.showAndWait();

        result.ifPresent(newValues -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
                String updateQuery = generateUpdateQuery(tableName, columnNames);
                try (PreparedStatement statement = connection.prepareStatement(updateQuery)) {
                    for (int i = 0; i < newValues.size(); i++) {
                        String columnName = columnNames.get(i);
                        String columnType = columnTypes.get(columnName); // Получаем тип столбца

                        // В зависимости от типа столбца обрабатываем значение
                        if (columnType.equalsIgnoreCase("INTEGER") || columnType.equalsIgnoreCase("INT") || columnType.equalsIgnoreCase("INT4")) {
                            try {
                                statement.setInt(i + 1, Integer.parseInt(newValues.get(i).toString()));
                            } catch (NumberFormatException e) {
                                showAlert("Ошибка", "Значение для столбца '" + columnName + "' должно быть целым числом.");
                                return;
                            }
                        } else if (columnType.equalsIgnoreCase("VARCHAR") || columnType.equalsIgnoreCase("CHAR")) {
                            statement.setString(i + 1, newValues.get(i).toString());
                        } else if (columnType.equalsIgnoreCase("TEXT")) {
                            statement.setString(i + 1, newValues.get(i).toString());
                        } else {
                            // Обработка других типов, если необходимо
                            statement.setObject(i + 1, newValues.get(i));
                        }
                    }

                    // Устанавливаем значение для WHERE (id)
                    statement.setObject(newValues.size() + 1, selectedRow.get(0));
                    statement.executeUpdate();

                    refreshTables();
                    showAlert("Успех", "Запись успешно обновлена.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Ошибка", "Произошла ошибка при обновлении записи: " + e.getMessage());
            }
        });
    }


    private Map<String, String> getColumnTypes(String tableName) {
        Map<String, String> columnTypes = new HashMap<>();

        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, tableName, null);

            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                columnTypes.put(columnName, columnType);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return columnTypes;
    }


    private String generateUpdateQuery(String tableName, List<String> columnNames) {
        StringBuilder setClause = new StringBuilder();
        for (String column : columnNames) {
            if (setClause.length() > 0) setClause.append(", ");
            setClause.append(column).append(" = ?");
        }
        return "UPDATE " + tableName + " SET " + setClause + " WHERE id = ?";
    }




    private void deleteRow() {
        // Проверяем, есть ли активная вкладка и соответствующая TableView
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            showAlert("Ошибка", "Не выбрана таблица для удаления записи!");
            return;
        }

        TableView<List<Object>> tableView = tableViews.get(selectedTab.getText());
        if (tableView == null) {
            showAlert("Ошибка", "Не удалось найти таблицу для текущей вкладки!");
            return;
        }

        // Получаем выделенную строку
        List<Object> selectedRow = tableView.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showAlert("Ошибка", "Не выбрана строка для удаления!");
            return;
        }

        // Открываем диалоговое окно с подтверждением удаления
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Подтверждение удаления");
        confirmationAlert.setHeaderText("Вы уверены, что хотите удалить эту запись?");
        confirmationAlert.setContentText("Удаление записи необратимо.");

        // Добавляем кнопки "Да" и "Нет"
        ButtonType buttonTypeYes = new ButtonType("Да");
        ButtonType buttonTypeNo = new ButtonType("Нет");
        confirmationAlert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

        // Ждем выбора пользователя
        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == buttonTypeYes) {
            // Генерируем SQL-запрос для удаления строки
            String tableName = selectedTab.getText();
            try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
                // Получаем первичный ключ (предполагается, что он находится в первом столбце)
                Object primaryKeyValue = selectedRow.get(0);
                String primaryKeyColumn = getPrimaryKeyColumn(connection, tableName);

                if (primaryKeyColumn == null) {
                    showAlert("Ошибка", "Не удалось определить первичный ключ для таблицы!");
                    return;
                }

                // Удаляем запись
                String deleteQuery = "DELETE FROM " + tableName + " WHERE " + primaryKeyColumn + " = ?";
                try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
                    statement.setObject(1, primaryKeyValue);
                    int affectedRows = statement.executeUpdate();

                    if (affectedRows > 0) {
                        // Обновляем интерфейс
                        tableView.getItems().remove(selectedRow);
                        showAlert("Успех", "Запись успешно удалена.");
                    } else {
                        showAlert("Ошибка", "Не удалось удалить запись.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Ошибка", "Произошла ошибка при удалении записи: " + e.getMessage());
            }
        } else {
            showAlert("Отмена", "Удаление записи отменено.");
        }
    }


    private String getPrimaryKeyColumn(Connection connection, String tableName) throws SQLException {
        // Получаем информацию о первичном ключе из метаданных базы данных
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet pkResult = metaData.getPrimaryKeys(null, null, tableName)) {
            if (pkResult.next()) {
                return pkResult.getString("COLUMN_NAME");
            }
        }
        return null;
    }

    static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }


    public static String getUSER() {
        return USER;
    }

    public static void setUSER(String USER) {
        TableController.USER = USER;
    }

    public static String getPASSWORD() {
        return PASSWORD;
    }

    public static void setPASSWORD(String PASSWORD) {
        TableController.PASSWORD = PASSWORD;
    }
}
