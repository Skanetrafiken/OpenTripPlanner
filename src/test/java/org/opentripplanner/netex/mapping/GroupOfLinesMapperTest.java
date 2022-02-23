package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.Test;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PrivateCodeStructure;

public class GroupOfLinesMapperTest {

    private final String ID = "RUT:GroupOfLines:1";
    private final String PRIVATE_CODE = "test_private_code";
    private final String NAME = "test_name";
    private final String SHORT_NAME = "test_short_name";
    private final String DESCRIPTION = "description";

    @Test
    public void mapGroupOfLines() {
        GroupOfLinesMapper mapper = new GroupOfLinesMapper(MappingSupport.ID_FACTORY);

        org.opentripplanner.model.GroupOfLines groupOfLines = mapper.mapGroupOfLines(createGroupOfLines());

        assertNotNull(groupOfLines);
        assertEquals(ID, groupOfLines.getId().getId());
        assertEquals(NAME, groupOfLines.getName());
        assertEquals(SHORT_NAME, groupOfLines.getShortName());
        assertEquals(DESCRIPTION, groupOfLines.getDescription());
    }

    private GroupOfLines createGroupOfLines() {
        return new GroupOfLines()
                .withId(ID)
                .withPrivateCode(new PrivateCodeStructure().withValue(PRIVATE_CODE))
                .withName(new MultilingualString().withValue(NAME))
                .withShortName(new MultilingualString().withValue(SHORT_NAME))
                .withDescription(new MultilingualString().withValue(DESCRIPTION));
    }
}
