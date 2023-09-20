package org.opentripplanner.ext.transmodelapi.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;

public class PassThroughPointInputType {

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType
    .newInputObject()
    .name("PassThroughPoint")
    .description("Defines one pass-through point which the journey must pass by.")
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("name")
        .description(
          "Optional name of the pass-through point for debugging and logging and is not used in routing."
        )
        .type(Scalars.GraphQLString)
        .build()
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("placeIds")
        .description(
          "The ids of elements in the OTP model. Defines one or multiple stops of the pass-through point, of which only one is required to be passed by. Currently supports" +
          " Quay, StopPlace, multimodal StopPlace, and GroupOfStopPlaces."
        )
        .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
        .build()
    )
    .build();
}
