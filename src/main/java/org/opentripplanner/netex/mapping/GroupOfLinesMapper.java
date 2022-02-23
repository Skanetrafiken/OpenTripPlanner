package org.opentripplanner.netex.mapping;


import org.opentripplanner.model.GroupOfLines;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PrivateCodeStructure;

/** Responsible for mapping NeTEx Branding into the OTP model. */
public class GroupOfLinesMapper {

    private final FeedScopedIdFactory feedScopedIdFactory;

    public GroupOfLinesMapper(FeedScopedIdFactory feedScopedIdFactory) {
        this.feedScopedIdFactory = feedScopedIdFactory;
    }

    /**
     * Convert NeTEx GroupOfLines Entity into OTP model.
     * @param groupOfLines NeTEx GroupOfLines entity.
     * @return OTP GroupOfLines model
     */
    public GroupOfLines mapGroupOfLines(org.rutebanken.netex.model.GroupOfLines groupOfLines) {
        GroupOfLines model = new GroupOfLines(feedScopedIdFactory.createId(groupOfLines.getId()));

        final PrivateCodeStructure privateCode = groupOfLines.getPrivateCode();
        final MultilingualString shortName = groupOfLines.getShortName();
        final MultilingualString name = groupOfLines.getName();
        final MultilingualString description = groupOfLines.getDescription();

        if (privateCode != null) {
            model.setPrivateCode(privateCode.getValue());
        }

        if (shortName != null) {
            model.setShortName(shortName.getValue());
        }

        if (name != null) {
            model.setName(name.getValue());
        }

        if (description != null) {
            model.setDescription(description.getValue());
        }

        return model;
    }
}
