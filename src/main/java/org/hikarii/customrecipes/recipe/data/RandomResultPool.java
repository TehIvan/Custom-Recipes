package org.hikarii.customrecipes.recipe.data;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomResultPool {
    
    public static final RandomResultPool CANCELLED = new RandomResultPool(null, false, 0);

    public static final RandomResultPool EMPTY_SAVED = new RandomResultPool(null, false, 0);

    private final List<RandomResult> results;
    private final int totalWeight;
    private final boolean showChances;
    private final int failureChance; 
    private final Random random = new Random();

    public RandomResultPool(List<RandomResult> results, boolean showChances) {
        this(results, showChances, 0);
    }

    public RandomResultPool(List<RandomResult> results, boolean showChances, int failureChance) {
        this.results = results != null ? new ArrayList<>(results) : new ArrayList<>();
        this.showChances = showChances;
        this.failureChance = Math.max(0, Math.min(100, failureChance));
        this.totalWeight = this.results.stream().mapToInt(RandomResult::getWeight).sum();
    }

    public boolean hasRandomResults() {
        return !results.isEmpty();
    }

    public List<RandomResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    public int getTotalWeight() {
        return totalWeight;
    }

    public boolean isShowChances() {
        return showChances;
    }

    public int getFailureChance() {
        return failureChance;
    }

    public boolean hasFailureChance() {
        return failureChance > 0;
    }

    public boolean rollFailure() {
        if (failureChance <= 0) return false;
        if (failureChance >= 100) return true;
        return random.nextInt(100) < failureChance;
    }

    public double getChance(RandomResult result) {
        if (totalWeight <= 0) return 0;
        return (result.getWeight() * 100.0) / totalWeight;
    }

    public ItemStack selectRandomResult() {
        if (results.isEmpty()) {
            return null;
        }

        if (results.size() == 1) {
            return results.get(0).getItem();
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;

        for (RandomResult result : results) {
            cumulative += result.getWeight();
            if (roll < cumulative) {
                return result.getItem();
            }
        }

        return results.get(results.size() - 1).getItem();
    }

    public int size() {
        return results.size();
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean isEmptySaved() {
        return this == EMPTY_SAVED;
    }
}
