// model/InvestmentAccount.java
package model;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

public class InvestmentAccount extends Account {
    private final Map<Fund, BigDecimal> investments;

    public InvestmentAccount() {
        super();
        this.investments = new EnumMap<>(Fund.class);
        for (Fund fund : Fund.values()) {
            investments.put(fund, BigDecimal.ZERO);
        }
    }

    public Map<Fund, BigDecimal> getInvestments() {
        return investments;
    }

    public void invest(Fund fund, BigDecimal amount) {
        investments.put(fund, investments.get(fund).add(amount));
        withdraw(amount);
    }

    public void withdrawInvestments() {
        BigDecimal total = BigDecimal.ZERO;
        for (Fund fund : Fund.values()) {
            total = total.add(investments.get(fund));
            investments.put(fund, BigDecimal.ZERO);
        }
        deposit(total);
    }

    public void applyFundGrowth() {
        for (Fund fund : Fund.values()) {
            BigDecimal current = investments.get(fund);
            if (current.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal growth = current.multiply(BigDecimal.valueOf(fund.getAppreciationRate()));
                investments.put(fund, current.add(growth));
            }
        }
    }
}