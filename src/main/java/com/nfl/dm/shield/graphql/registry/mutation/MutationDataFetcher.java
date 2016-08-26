package com.nfl.dm.shield.graphql.registry.mutation;

import com.nfl.dm.shield.graphql.exception.GlitrValidationException;
import com.nfl.dm.shield.util.JsonUtils;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Map;
import java.util.Set;

/**
 * DataFetcher used when GraphQL operation is a mutation
 */
public class MutationDataFetcher implements DataFetcher {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(MutationDataFetcher.class);

    private final Class mutationInputClass;
    private final Validator validator;
    private final RelayMutation mutationFunc;


    public MutationDataFetcher(Class mutationInputClass, Validator validator, RelayMutation mutationFunc) {
        this.mutationInputClass = mutationInputClass;
        this.validator = validator;
        this.mutationFunc = mutationFunc;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get(DataFetchingEnvironment env) {
        Map<String, Object> inputMap = env.getArgument("input");

        // map fields from input map to mutationInputClass
        Object inputPayloadMtn = JsonUtils.convertValue(inputMap, mutationInputClass);
        // apply some validation on inputPayloadMtn (should validation be in the mutationFunc instead?)
        validate(inputPayloadMtn);
        // mutate and return output
        RelayMutationType mutationOutput = mutationFunc.call((RelayMutationType) inputPayloadMtn, env);
        // set the client mutation id
        mutationOutput.setClientMutationId((String) inputMap.get("clientMutationId"));

        return mutationOutput;
    }

    private void validate(Object inputPayloadMtn) {
        if (validator == null) {
            return;
        }

        Set<ConstraintViolation<Object>> errors = validator.validate(inputPayloadMtn);
        if (!errors.isEmpty()) {
            throw new GlitrValidationException("Error validating input mutation.", errors);
        }
    }
}