// BankingApp.java
import java.util.Scanner;
import service.BankingService;

public class BankingApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BankingService bankingService = new BankingService(scanner);
        bankingService.start();
    }
}