package com.suiseika.solenoid;

public enum ProcessedForm {
    CRUSHED("crushed_", "", "crushed_ore_template"),
    CONCENTRATE("", "_concentrate", "ore_concentrate_template"),
    SLAG("", "_slag", "ore_concentrate_template");

    private final String prefix;
    private final String suffix;
    private final String templateModel;

    ProcessedForm(String prefix, String suffix, String templateModel) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.templateModel = templateModel;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getTemplateModel() {
        return templateModel;
    }
}
