package app_quan_ly_tai_chinh;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class EditTransactionController {
    @FXML private TextField descriptionField;
    @FXML private TextField amountField;
    @FXML private DatePicker datePicker;
    @FXML private RadioButton incomeRadio;
    @FXML private RadioButton expenseRadio;
    @FXML private Button confirmButton;
    @FXML private Button cancelButton;

    private ToggleGroup group = new ToggleGroup();
    private Transaction currentTransaction;
    private Controller mainController;
    private DatabaseConnection dbConnection = new DatabaseConnection();

    @FXML
    private void initialize() {
        incomeRadio.setToggleGroup(group);
        expenseRadio.setToggleGroup(group);
    }

    public void setTransactionData(Transaction transaction, Controller controller) {
        this.currentTransaction = transaction;
        this.mainController = controller;

        // Set initial values
        descriptionField.setText(transaction.getDescription());
        amountField.setText(String.valueOf(Math.abs(transaction.getAmount())));
        datePicker.setValue(transaction.getDate());

        if (transaction.getAmount() >= 0) {
            incomeRadio.setSelected(true);
        } else {
            expenseRadio.setSelected(true);
        }

        // Event Handlers
        confirmButton.setOnAction(e -> updateTransaction());
        cancelButton.setOnAction(e -> ((Stage) cancelButton.getScene().getWindow()).close());
    }

    private void updateTransaction() {
        try {
            double amount = Double.parseDouble(amountField.getText());
            if (expenseRadio.isSelected()) {
                amount = -Math.abs(amount);
            }

            // Update database
            try (Connection conn = dbConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE Transactions SET Description=?, Amount=?, Date=? WHERE ID=?")) {
                stmt.setString(1, descriptionField.getText());
                stmt.setDouble(2, amount);
                stmt.setDate(3, java.sql.Date.valueOf(datePicker.getValue()));
                stmt.setInt(4, currentTransaction.getId());
                stmt.executeUpdate();
            }

            // Update transaction in the table
            currentTransaction.setDescription(descriptionField.getText());
            currentTransaction.setAmount(amount);
            currentTransaction.setDate(datePicker.getValue());
            mainController.refreshTable(); // Call a method in Controller to update UI

            ((Stage) confirmButton.getScene().getWindow()).close();
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter a valid amount.");
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to update transaction.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
