package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class InvestmentAccount extends Account {
    private final Map<Fund, BigDecimal> investments;

    public InvestmentAccount() {
        super();
        this.investments = new EnumMap<>(Fund.class);
        for (Fund fund : Fund.values()) {
            investments.put(fund, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
    }

    public Map<Fund, BigDecimal> getInvestments() {
        return Collections.unmodifiableMap(investments);
    }

    public BigDecimal getTotalInvested() {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal amount : investments.values()) {
            total = total.add(amount);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public void invest(Fund fund, BigDecimal amount) {
        BigDecimal normalizedAmount = normalize(amount);
        withdraw(normalizedAmount);
        investments.put(fund, investments.get(fund).add(normalizedAmount));
    }

    public BigDecimal withdrawInvestments() {
        BigDecimal total = getTotalInvested();
        for (Fund fund : Fund.values()) {
            investments.put(fund, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        deposit(total);
        return total;
    }

    public void applyFundGrowth() {
        for (Fund fund : Fund.values()) {
            BigDecimal current = investments.get(fund);
            if (current.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal growth = current.multiply(fund.getAppreciationRate());
                investments.put(fund, current.add(growth).setScale(2, RoundingMode.HALF_UP));
            }
        }
    }
}
