package org.opentripplanner.index.transmodel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.DoubleStream;
// TODO move to org.opentripplanner.api.resource, this is a Jersey resource class

@Path("/routers/{routerId}/transmodel/index")    // It would be nice to get rid of the final /index.
@Produces(MediaType.APPLICATION_JSON) // One @Produces annotation for all endpoints.
public class TransmodelIndexAPI {
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(TransmodelIndexAPI.class);
    private static final String MSG_400 = "FOUR HUNDRED";

    /**
     * Choose short or long form of results.
     */
    @QueryParam("detail")
    private boolean detail = false;

    /**
     * Include GTFS entities referenced by ID in the result.
     */
    @QueryParam("refs")
    private boolean refs = false;

    private final TransmodelGraphIndex index;
    private final ObjectMapper deserializer = new ObjectMapper();

    public TransmodelIndexAPI(@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        router = otpServer.getRouter(routerId);
        index = new TransmodelGraphIndex( router.graph.index);
    }

    /* Needed to check whether query parameter map is empty, rather than chaining " && x == null"s */
    @Context
    UriInfo uriInfo;

    @Context
    HttpHeaders headers;

    private Router router;

    @POST
    @Path("/graphql")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getGraphQL(HashMap<String, Object> queryParameters, @HeaderParam("OTPTimeout") @DefaultValue("10000") int timeout, @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") long maxResolves) {
        int finalTimeout = checkTimeout(timeout);
        String query = (String) queryParameters.get("query");
        Object queryVariables = queryParameters.getOrDefault("variables", null);
        String operationName = (String) queryParameters.getOrDefault("operationName", null);
        Map<String, Object> variables;
        if (queryVariables instanceof Map) {
            variables = (Map) queryVariables;
        } else if (queryVariables instanceof String && !((String) queryVariables).isEmpty()) {
            try {
                variables = deserializer.readValue((String) queryVariables, Map.class);
            } catch (IOException e) {
                LOG.error("Variables must be a valid json object");
                return Response.status(Response.Status.BAD_REQUEST).entity(MSG_400).build();
            }
        } else {
            variables = new HashMap<>();
        }
        return index.getGraphQLResponse(query, router, variables, operationName, finalTimeout, maxResolves);
    }

    @POST
    @Path("/graphql")
    @Consumes("application/graphql")
    public Response getGraphQL(String query, @HeaderParam("OTPTimeout") @DefaultValue("10000") int timeout, @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") long maxResolves) {
        int finalTimeout = checkTimeout(timeout);
        return index.getGraphQLResponse(query, router, null, null, finalTimeout, maxResolves);
    }

    @POST
    @Path("/graphql/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getGraphQLBatch(List<HashMap<String, Object>> queries, @HeaderParam("OTPTimeout") @DefaultValue("10000") int timeout, @HeaderParam("OTPMaxResolves") @DefaultValue("1000000") long maxResolves) {
        int finalTimeout = checkTimeout(timeout);
        List<Map<String, Object>> responses = new ArrayList<>();
        List<Callable<Map>> futures = new ArrayList();

        for (HashMap<String, Object> query : queries) {
            Map<String, Object> variables;
            if (query.get("variables") instanceof Map) {
                variables = (Map) query.get("variables");
            } else if (query.get("variables") instanceof String && ((String) query.get("variables")).length() > 0) {
                try {
                    variables = deserializer.readValue((String) query.get("variables"), Map.class);
                } catch (IOException e) {
                    LOG.error("Variables must be a valid json object");
                    return Response.status(Response.Status.BAD_REQUEST).entity(MSG_400).build();
                }
            } else {
                variables = null;
            }
            String operationName = (String) query.getOrDefault("operationName", null);

            futures.add(() -> index.getGraphQLExecutionResult((String) query.get("query"), router,
                    variables, operationName, finalTimeout, maxResolves));
        }

        try {
            List<Future<Map>> results = index.threadPool.invokeAll(futures);

            for (int i = 0; i < queries.size(); i++) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("id", queries.get(i).get("id"));
                response.put("payload", results.get(i).get());
                responses.add(response);
            }
        } catch (CancellationException | ExecutionException | InterruptedException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Response.Status.OK).entity(responses).build();
    }

    private int checkTimeout(int timeout) {
        if (router.timeouts.length > 0) {
            int newTimeout = (int) Math.floor(DoubleStream.of(router.timeouts).sum() + 5) * 1000;
            if (newTimeout > timeout) {
                LOG.debug("Timeout set to sum of router config timeouts. Sum: {}. Old timeout: {}", newTimeout, timeout);
                timeout = newTimeout;
            }
        }
        return timeout;
    }

}
