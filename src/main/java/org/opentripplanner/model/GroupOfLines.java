package org.opentripplanner.model;

/**
 * OTP model for group of lines. Only used for NeTEx at the moment
 */
public class GroupOfLines extends TransitEntity {

    private String privateCode;
    private String shortName;
    private String name;
    private String description;

    public GroupOfLines(FeedScopedId id) {
        super(id);
    }

    public String getPrivateCode() {
        return privateCode;
    }

    public void setPrivateCode(String privateCode) {
        this.privateCode = privateCode;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
