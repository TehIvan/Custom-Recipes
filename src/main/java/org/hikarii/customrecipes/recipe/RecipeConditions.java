package org.hikarii.customrecipes.recipe;

public class RecipeConditions {
    private final String permission;
    private final int requiredXpLevel;
    private final int xpReward;
    private final int cooldownSeconds;
    private final int craftLimitDaily;
    private final int craftLimitWeekly;
    private final int craftLimitTotal;
    private final double moneyCost;

    public RecipeConditions(String permission, int requiredXpLevel, int xpReward,
                           int cooldownSeconds, int craftLimitDaily, int craftLimitWeekly,
                           int craftLimitTotal, double moneyCost) {
        this.permission = permission;
        this.requiredXpLevel = requiredXpLevel;
        this.xpReward = xpReward;
        this.cooldownSeconds = cooldownSeconds;
        this.craftLimitDaily = craftLimitDaily;
        this.craftLimitWeekly = craftLimitWeekly;
        this.craftLimitTotal = craftLimitTotal;
        this.moneyCost = moneyCost;
    }

    public RecipeConditions(String permission, int requiredXpLevel, int xpReward) {
        this(permission, requiredXpLevel, xpReward, 0, 0, 0, 0, 0);
    }

    public RecipeConditions() {
        this(null, 0, 0, 0, 0, 0, 0, 0);
    }

    public String getPermission() {
        return permission;
    }

    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }

    public int getRequiredXpLevel() {
        return requiredXpLevel;
    }

    public boolean hasXpLevelRequirement() {
        return requiredXpLevel > 0;
    }

    public int getXpReward() {
        return xpReward;
    }

    public boolean hasXpReward() {
        return xpReward != 0;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean hasCooldown() {
        return cooldownSeconds > 0;
    }

    public int getCraftLimitDaily() {
        return craftLimitDaily;
    }

    public boolean hasDailyLimit() {
        return craftLimitDaily > 0;
    }

    public int getCraftLimitWeekly() {
        return craftLimitWeekly;
    }

    public boolean hasWeeklyLimit() {
        return craftLimitWeekly > 0;
    }

    public int getCraftLimitTotal() {
        return craftLimitTotal;
    }

    public boolean hasTotalLimit() {
        return craftLimitTotal > 0;
    }

    public double getMoneyCost() {
        return moneyCost;
    }

    public boolean hasMoneyCost() {
        return moneyCost > 0;
    }

    public boolean hasAnyCondition() {
        return hasPermission() || hasXpLevelRequirement() || hasXpReward() ||
               hasCooldown() || hasDailyLimit() || hasWeeklyLimit() ||
               hasTotalLimit() || hasMoneyCost();
    }

    public boolean hasAnyLimit() {
        return hasDailyLimit() || hasWeeklyLimit() || hasTotalLimit();
    }

    @Override
    public String toString() {
        return "RecipeConditions{" +
                "permission='" + permission + '\'' +
                ", requiredXpLevel=" + requiredXpLevel +
                ", xpReward=" + xpReward +
                ", cooldownSeconds=" + cooldownSeconds +
                ", craftLimitDaily=" + craftLimitDaily +
                ", craftLimitWeekly=" + craftLimitWeekly +
                ", craftLimitTotal=" + craftLimitTotal +
                ", moneyCost=" + moneyCost +
                '}';
    }
}
