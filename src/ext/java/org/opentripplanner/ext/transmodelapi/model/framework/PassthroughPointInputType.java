package org.opentripplanner.ext.transmodelapi.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;

public class PassthroughPointInputType {

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType
    .newInputObject()
    .name("PassthroughPoint")
    .description("TODO")
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name("places")
        .description(
          "The ids of elements in the OTP model. Currently supports" +
          " Quay, StopPlace, multimodal StopPlace, and GroupOfStopPlaces."
        )
        .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLString)))
        .build()
    )
    .build();
}
