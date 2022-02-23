package org.opentripplanner.ext.transmodelapi.model.network;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper;
import org.opentripplanner.model.GroupOfLines;

public class GroupOfLinesType {

    private static final String NAME = "GroupOfLines";

    public static GraphQLObjectType create() {
        return GraphQLObjectType
                .newObject()
                .name(NAME)
                .description("Additional (optional) grouping of lines for particular purposes such as e.g. fare harmonisation or public presentation.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(env -> TransitIdMapper.mapEntityIDToApi(env.getSource()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("privateCode")
                        .type(Scalars.GraphQLString)
                        .description("Group private code.")
                        .dataFetcher(env -> ((GroupOfLines) env.getSource()).getPrivateCode())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("shortName")
                        .description("Group short name.")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(env -> ((GroupOfLines) env.getSource()).getShortName())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .description("Group name.")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(env -> ((GroupOfLines) env.getSource()).getName())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("description")
                        .description("Group description.")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(env -> ((GroupOfLines) env.getSource()).getDescription())
                        .build())
                .build();
    }
}