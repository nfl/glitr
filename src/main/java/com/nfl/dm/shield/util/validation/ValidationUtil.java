package com.nfl.dm.shield.util.validation;

import com.nfl.dm.shield.web.exception.NFLException;
import com.nfl.dm.shield.web.exception.NFLValidationException;
import com.nfl.dm.shield.web.response.NFLError;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Copied from identity, good reason to make it part of a lib!
 */
public abstract class ValidationUtil {

    public static void validOrThrowException(Object o, Validator[] validators,
            Object... validationHints) throws NFLValidationException {

        BindingResult errors = validate(o, validators, validationHints);

        if (errors.hasErrors()) {
            List<NFLError> nflErrorList = getNflErrors(errors);
            throw new NFLValidationException("Validation failure.", nflErrorList);
        }
    }

    public static NFLException validOrReturnException(Object o, Validator[] validators,
                                                      Object... validationHints) {

        BindingResult errors = validate(o, validators, validationHints);

        if (errors.hasErrors()) {
            List<NFLError> nflErrorList = getNflErrors(errors);
            return new NFLValidationException("Validation failure.", nflErrorList);
        }

        return null;
    }

    public static void validOrThrowException(Object o, Validator validator,
            Object... validationHints) throws NFLValidationException {
        Validator[] validators = new Validator[1];
        validators[0] = validator;
        validOrThrowException(o, validators, validationHints);
    }

    public static NFLException validOrReturnException(Object o, Validator validator,
                                             Object... validationHints) {
        Validator[] validators = new Validator[1];
        validators[0] = validator;
        return validOrReturnException(o, validators, validationHints);
    }

    public static BindingResult validate(Object o, Validator[] validators, Object... validationHints) {
        DataBinder binder = new DataBinder(o);
        binder.addValidators(validators);
        binder.validate(validationHints);
        return binder.getBindingResult();
    }

    public static BindingResult validate(Object o, Validator validator, Object... validationHints) {
        Validator[] validators = new Validator[1];
        validators[0] = validator;
        return validate(o, validators, validationHints);
    }

    private static List<NFLError> getNflErrors(BindingResult errors) {
        List<NFLError> nflErrorList = new ArrayList<>();
        for (FieldError error : errors.getFieldErrors()) {
            NFLError nflError = new NFLError(error.getField(), error.getDefaultMessage(), error.getRejectedValue());
            nflErrorList.add(nflError);
        }
        return nflErrorList;
    }
}