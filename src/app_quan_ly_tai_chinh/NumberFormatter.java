package app_quan_ly_tai_chinh;

import java.text.NumberFormat;
import java.util.Locale;

public class NumberFormatter {
  @SuppressWarnings("deprecation")
private static final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
  
  public static String formatNumber(double number) {
    return currencyFormatter.format(number);
  }
}
