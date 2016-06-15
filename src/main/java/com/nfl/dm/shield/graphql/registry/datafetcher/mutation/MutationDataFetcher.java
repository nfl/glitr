package com.nfl.dm.shield.graphql.registry.datafetcher.mutation;

import com.nfl.dm.shield.graphql.error.NFLGraphQLValidationError;
import com.nfl.dm.shield.util.JsonUtils;
import com.nfl.dm.shield.util.validation.ValidationUtil;
import com.nfl.dm.shield.web.exception.NFLValidationException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Validator;
import rx.functions.Func4;

import java.util.LinkedHashMap;
import java.util.Map;

public class MutationDataFetcher implements DataFetcher {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(MutationDataFetcher.class);

    private final Class mutationPayloadClass;
    private final Class mutationInputClass;
    private final Validator validator;
    private final Func4<Object, Class, Class, DataFetchingEnvironment, Object> mutationFunc;


    public MutationDataFetcher(Class mutationPayloadClass, Class mutationInputClass, Validator validator,
                               Func4<Object, Class, Class, DataFetchingEnvironment, Object> mutationFunc) {
        this.mutationPayloadClass = mutationPayloadClass;
        this.mutationInputClass = mutationInputClass;
        this.validator = validator;
        this.mutationFunc = mutationFunc;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get(DataFetchingEnvironment env) {
        Map<String, Object> inputMap = env.getArgument("input");
        Map<String, Object> input = (Map<String, Object>) inputMap.get(
                StringUtils.uncapitalize(mutationPayloadClass.getSimpleName()));

        // map fields from input map to mutationInputClass
        Object inputPayloadMtn = JsonUtils.convertValue(input, mutationInputClass);

        Object mutationOutput;
        // apply some validation on inputPayloadMtn (should validation be in the mutationFunc instead?)
        if (validator != null) {
            try {
                ValidationUtil.validOrThrowException(inputPayloadMtn, validator);
            } catch (NFLValidationException e) {
                // construct error
                throw new NFLGraphQLValidationError()
                        .setErrors(e.getErrors())
                        .setMessage(e.getMessage());
            }
        }
        // mutate and return output
        mutationOutput = mutationFunc.call(inputPayloadMtn, mutationInputClass, mutationPayloadClass, env);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clientMutationId", inputMap.get("clientMutationId"));
        result.put(StringUtils.uncapitalize(mutationPayloadClass.getSimpleName()), mutationOutput);
        return result;
    }
}
