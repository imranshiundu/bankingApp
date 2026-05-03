import service.BankingCore;
import web.WebBankingServer;

public class WebBankingApp {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        BankingCore bankingCore = new BankingCore();
        WebBankingServer server = new WebBankingServer(bankingCore, port);
        server.start();
    }
}
