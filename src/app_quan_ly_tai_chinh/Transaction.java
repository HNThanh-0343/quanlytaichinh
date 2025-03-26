package app_quan_ly_tai_chinh;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private int id;
    private LocalDate date;
    private String description;
    private double amount;
    private String type; // "Income" or "Expense"

    public Transaction(int id, LocalDate date, String description, double amount, String type) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.type = type;
    }

    // Getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return id + "," + date.format(DateTimeFormatter.ISO_LOCAL_DATE) + "," + description + "," + amount + "," + type;
    }

    public static Transaction fromString(String line) {
        String[] parts = line.split(",", -1); // Use -1 to include trailing empty strings
        int id = Integer.parseInt(parts[0]);
        LocalDate date = LocalDate.parse(parts[1], DateTimeFormatter.ISO_LOCAL_DATE);
        String description = parts[2]; // This can be empty
        double amount = Double.parseDouble(parts[3]);
        String type = parts[4];
        return new Transaction(id, date, description, amount, type);
    }
}