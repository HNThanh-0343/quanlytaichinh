package app_quan_ly_tai_chinh;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Controller {
    @FXML private TextField descriptionField;
    @FXML private TextField amountField;
    @FXML private DatePicker datePicker;
    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, Integer> idColumn;
    @FXML private TableColumn<Transaction, Double> amountColumn;
    @FXML private TableColumn<Transaction, String> descriptionColumn;
    @FXML private TableColumn<Transaction, LocalDate> dateColumn;
    @FXML private BarChart<String, Number> transactionChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private ChoiceBox<String> aggregationChoiceBox;
    @FXML private ChoiceBox<String> viewChoiceBox;
    @FXML private ChoiceBox<String> yearChoiceBox;
    @FXML private RadioButton incomeRadio;
    @FXML private RadioButton expenseRadio;

    private ToggleGroup group = new ToggleGroup();
    private ObservableList<Transaction> transactionList = FXCollections.observableArrayList();
    private ObservableList<Transaction> filteredTransactionList = FXCollections.observableArrayList();
    private List<Text> dataTextNodes = new ArrayList<>();


    private DatabaseConnection dbConnection;

    @FXML
    private void initialize() {
    	
    	amountColumn.setCellFactory(column -> new TableCell<Transaction, Double>() {
    	    @Override
    	    protected void updateItem(Double amount, boolean empty) {
    	        super.updateItem(amount, empty);
    	        if (empty || amount == null) {
    	            setText(null);
    	            setStyle(""); // Reset style
    	        } else {
    	            setText(String.format("%,.0f VND", amount)); // Format currency

    	            // Apply color based on income/expense
    	            if (amount >= 0) {
    	                setStyle("-fx-text-fill: green;"); // Green for income
    	            } else {
    	                setStyle("-fx-text-fill: red;"); // Red for expenses
    	            }
    	        }
    	    }
    	});

    	
        dbConnection = new DatabaseConnection();

        incomeRadio.setToggleGroup(group);
        expenseRadio.setToggleGroup(group);

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        transactionTable.setItems(filteredTransactionList);
        transactionList.addListener((ListChangeListener<Transaction>) change -> {
            filterTransactions(viewChoiceBox.getValue());
            populateYearChoiceBox(); // Update years when transactions change
            updateChart();
        });

        aggregationChoiceBox.getItems().addAll("Month and Year", "Year");
        aggregationChoiceBox.setValue("Month and Year");
        aggregationChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateChart());
        aggregationChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateChart();
            yearChoiceBox.setVisible("Month and Year".equals(newValue)); // Show only when "Month and Year" is selected
        });

        viewChoiceBox.getItems().addAll("All", "Income", "Expenses");
        viewChoiceBox.setValue("All");
        viewChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> filterTransactions(newValue));

        yearChoiceBox.setOnAction(e -> updateChart());
        

        loadTransactionsFromDatabase();
    }

    private void loadTransactionsFromDatabase() {
        transactionList.clear();
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Transactions ORDER BY Date")) {

            while (rs.next()) {
                int id = rs.getInt("ID");
                LocalDate date = rs.getDate("Date").toLocalDate();
                String description = rs.getString("Description");
                double amount = rs.getDouble("Amount");
                String type = amount >= 0 ? "Income" : "Expense";

                Transaction transaction = new Transaction(id, date, description, amount, type);
                transactionList.add(transaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load transactions.");
        }

        populateYearChoiceBox(); // Ensure year choice box is updated
    }

    @FXML
    private void handleAddTransaction() {
        String description = descriptionField.getText();
        double amount;

        try {
            amount = Double.parseDouble(amountField.getText());
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Please enter a valid amount.");
            return;
        }

        if (incomeRadio.isSelected()) {
            amount = Math.abs(amount);
        } else if (expenseRadio.isSelected()) {
            amount = -Math.abs(amount);
        } else {
            showAlert("Selection Error", "Please select whether the amount is income or expense.");
            return;
        }

        LocalDate date = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO Transactions (Date, Description, Amount) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setString(2, description);
            stmt.setDouble(3, amount);
            stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int id = generatedKeys.getInt(1);
                transactionList.add(new Transaction(id, date, description, amount, amount >= 0 ? "Income" : "Expense"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to add transaction.");
        }

        descriptionField.clear();
        amountField.clear();
        datePicker.setValue(null);
    }

    @FXML
    private void handleDeleteTransaction() {
        Transaction selectedTransaction = transactionTable.getSelectionModel().getSelectedItem();
        if (selectedTransaction == null) {
            showAlert("Selection Error", "Please select a transaction to delete.");
            return;
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM Transactions WHERE ID = ?")) {
            stmt.setInt(1, selectedTransaction.getId());
            stmt.executeUpdate();
            transactionList.remove(selectedTransaction);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to delete transaction.");
        }
    }
    
    @FXML
    private void handleEditTransaction() {
        Transaction selectedTransaction = transactionTable.getSelectionModel().getSelectedItem();
        if (selectedTransaction == null) {
            showAlert("Selection Error", "Please select a transaction to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("EditTransaction.fxml"));
            Parent root = loader.load();

            // Get the EditTransactionController and pass the selected transaction
            EditTransactionController editController = loader.getController();
            editController.setTransactionData(selectedTransaction, this); // Pass current controller for updates

            Stage stage = new Stage();
            stage.setTitle("Edit Transaction");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open edit window.");
        }
    }



    private void populateYearChoiceBox() {
        if (transactionList.isEmpty()) return;

        Set<Integer> years = transactionList.stream()
            .map(t -> t.getDate().getYear())
            .collect(Collectors.toSet());

        List<Integer> sortedYears = new ArrayList<>(years);
        Collections.sort(sortedYears, Collections.reverseOrder());

        yearChoiceBox.getItems().setAll(sortedYears.stream().map(String::valueOf).collect(Collectors.toList()));

        if (!sortedYears.isEmpty()) {
            yearChoiceBox.setValue(String.valueOf(sortedYears.get(0))); // Select most recent year
        }
    }

    private void filterTransactions(String filterType) {
        filteredTransactionList.clear();
        for (Transaction transaction : transactionList) {
            if ("All".equals(filterType) || 
                ("Income".equals(filterType) && transaction.getAmount() >= 0) || 
                ("Expenses".equals(filterType) && transaction.getAmount() < 0)) {
                filteredTransactionList.add(transaction);
            }
        }
    }
    
    public void refreshTable() {
        transactionTable.refresh();
    }

    private void updateChart() {
        // Clear existing data from the chart
        transactionChart.getData().clear();

        // Remove old labels
        for (Text dataText : dataTextNodes) {
            ((Group) dataText.getParent()).getChildren().remove(dataText);
        }
        dataTextNodes.clear(); // Clear the list of text nodes

        // Map to store aggregated amounts
        Map<String, Double> dateAmountMap = new TreeMap<>();

        // Get the selected aggregation type
        String aggregationType = aggregationChoiceBox.getValue();

        // Show year choice box only for "Month and Year" view
        yearChoiceBox.setVisible("Month and Year".equals(aggregationType));
        String selectedYear = yearChoiceBox.getValue();

        // Select appropriate date format
        DateTimeFormatter formatter = "Year".equals(aggregationType) ? 
                                      DateTimeFormatter.ofPattern("yyyy") : 
                                      DateTimeFormatter.ofPattern("yyyy-MM");

        // Aggregate transaction amounts by date key
        for (Transaction transaction : filteredTransactionList) {
            if ("Month and Year".equals(aggregationType) && selectedYear != null &&
                !(transaction.getDate().getYear() == Integer.parseInt(selectedYear))) {
                continue;
            }

            String dateKey = transaction.getDate().format(formatter);
            dateAmountMap.put(dateKey, dateAmountMap.getOrDefault(dateKey, 0.0) + transaction.getAmount());
        }

        // Sort dates in ascending order
        List<String> sortedDates = new ArrayList<>(dateAmountMap.keySet());
        Collections.sort(sortedDates);

        // Create series for the chart
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (String dateKey : sortedDates) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(dateKey, dateAmountMap.get(dateKey));

            // Attach a listener to dynamically add the labels when nodes are created
            data.nodeProperty().addListener((observable, oldNode, newNode) -> {
                if (newNode != null) {
                    String formattedAmount = String.format("%,.0f VND", data.getYValue().doubleValue());

                    Text dataText = new Text(formattedAmount);
                    dataText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-fill: black;");
                    dataTextNodes.add(dataText); // Store the text node

                    // Ensure text is added to the correct parent
                    newNode.parentProperty().addListener((obs, oldParent, newParent) -> {
                        if (newParent != null) {
                            ((Group) newParent).getChildren().add(dataText);
                        }
                    });

                    // Adjust text position when the node bounds change
                    newNode.boundsInParentProperty().addListener((obs, oldBounds, newBounds) -> {
                        Platform.runLater(() -> {
                            dataText.setLayoutX(newBounds.getMinX() + newBounds.getWidth() / 2 - dataText.prefWidth(-1) / 2);
                            dataText.setLayoutY(newBounds.getMinY() - 10); // Position above the bar
                        });
                    });
                }
            });

            series.getData().add(data);
        }

        // Add the series to the chart
        transactionChart.getData().add(series);
        
        // Get the current Y-axis
        NumberAxis yAxis = (NumberAxis) transactionChart.getYAxis();
        yAxis.setAutoRanging(false); // Disable automatic scaling

        // Find the maximum value in the dataset
        double maxAmount = dateAmountMap.values().stream().max(Double::compare).orElse(0.0);

        // Calculate a dynamic upper bound (add 10% padding or round to the nearest 50,000)
        double upperBound = Math.ceil(maxAmount * 1.1 / 50000) * 50000; // 10% padding and rounding up

        // Ensure a reasonable minimum upper bound
        if (upperBound < 100000) {
            upperBound = 100000; // Prevent too small upper bound
        }

        yAxis.setUpperBound(upperBound);
        yAxis.setTickUnit(upperBound / 10); // Adjust tick marks for better readability

    }



    public void adjustLayout(double width, double height) {
        System.out.println("Adjusting layout: Width = " + width + ", Height = " + height);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
