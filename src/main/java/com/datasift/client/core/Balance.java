package com.datasift.client.core;

import com.datasift.client.BaseDataSiftResult;
import com.datasift.client.DataSiftClient;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class Balance extends BaseDataSiftResult {
    @JsonProperty
    protected BalanceData balance;

    public Balance() {
    }

    /**
     * @return The account's price plan
     */
    public String pricePlan() {
        return balance.plan;
    }

    /**
     * @return The amount of credit remaining on the account
     */
    public double credit() {
        return balance.credit;
    }

    /**
     * @return The DPUs remaining on this plan for the current month.
     */
    public double remainingDpus() {
        return balance.remainingDpus;
    }

    /**
     * @return The license cost for your data plus, if your remaining_dpus falls to zero and you continue to run
     *         streams, the additional DPU cost you have incurred so far this month.
     */
    public double cost() {
        return balance.cost;
    }

    public BalanceData getBalance() {
        return balance;
    }

    @Override
    public String toString() {
        try {
            return DataSiftClient.MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Unable to generate string representation of this response/result";
        }
    }

    public static class BalanceData {
        @JsonProperty
        double cost;
        @JsonProperty
        String plan;
        @JsonProperty("remaining_dpus")
        double remainingDpus;
        @JsonProperty
        double credit;

        public void setPlan(String plan) {
            this.plan = plan;
        }

        public void setRemainingDpus(double remainingDpus) {
            this.remainingDpus = remainingDpus;
        }

        public void setCredit(double credit) {
            this.credit = credit;
        }
    }
}
