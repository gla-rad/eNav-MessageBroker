package org.grad.eNav.msgBroker.exceptions;

import org.apache.logging.log4j.util.Strings;
import org.grad.eNav.msgBroker.utils.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerErrorException extends RuntimeException implements AbstractException {


    /**
     * The Field errors.
     */
    private List<Pair<String,String>> fieldErrors;

    /**
     * The Global errors.
     */
    private List<String> globalErrors;

    /**
     * Instantiates a new Validation exception.
     *
     * Call to super to {@link Throwable#fillInStackTrace()}
     *
     * @param fieldErrors	The errors of individual fields
     * @param globalErrors	The global validation errors
     */
    public InternalServerErrorException(final List<Pair<String,String>> fieldErrors, final List<String> globalErrors) {
        super();
        setFieldErrors(fieldErrors);
        setGlobalErrors(globalErrors);
    }

    /**
     * Instantiates a new Validation exception.
     *
     * Call to super to {@link Throwable#fillInStackTrace()}
     */
    public InternalServerErrorException() {
        super();
        setFieldErrors(null);
        setGlobalErrors(null);
    }

    /**
     * Instantiates a new Validation exception.
     *
     * @param globalError the global error
     */
    public InternalServerErrorException(final String globalError) {
        this();
        addGlobalError(globalError);
    }

    /**
     * Instantiates a new Validation exception.
     *
     * @param field   the field
     * @param message the message
     */
    public InternalServerErrorException(final String field, final String message) {
        this();
        addFieldError(field, message);
    }

    /**
     * Gets short message.
     *
     * @return the short message
     */
    @Override
    public String getMessage() {
        return Stream.concat(
                getGlobalErrors().stream(),
                getFieldErrors().stream().map(p->String.format("Field \"%s\" %s.", p.getKey(),p.getValue()))
        ).collect(Collectors.collectingAndThen(Collectors.joining(" \n", "", ""),
                message -> Strings.isNotBlank(message) ? message : "There was an error during validation."
        ));
    }

    /**
     * Add global error.
     *
     * @param error the error
     */
    public void addGlobalError(final String error) {
        this.globalErrors.add(error);
    }

    /**
     * Add field error.
     *
     * @param field the field
     * @param error the error
     */
    public void addFieldError(final String field, final String error) {
        this.fieldErrors.add(new Pair<>(field,error));
    }

    /**
     * Gets field errors.
     *
     * @return the field errors
     */
    public List<Pair<String, String>> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * Sets field errors.
     *
     * @param fieldErrors the field errors
     */
    public void setFieldErrors(final List<Pair<String, String>> fieldErrors) {
        this.fieldErrors = Optional.ofNullable(fieldErrors).orElseGet(ArrayList::new);
    }

    /**
     * Gets global errors.
     *
     * @return the global errors
     */
    public List<String> getGlobalErrors() {
        return globalErrors;
    }

    /**
     * Sets global errors.
     *
     * @param globalErrors the global errors
     */
    public void setGlobalErrors(final List<String> globalErrors) {
        this.globalErrors = Optional.ofNullable(globalErrors).orElseGet(ArrayList::new);
    }
}
