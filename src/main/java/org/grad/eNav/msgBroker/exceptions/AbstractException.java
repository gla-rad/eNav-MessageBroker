package org.grad.eNav.msgBroker.exceptions;

import org.grad.eNav.msgBroker.utils.Pair;

import java.util.List;

/**
 * The abstract exception.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public interface AbstractException {

    /**
     * Gets the exception message.
     *
     * @return the default message.
     */
    public String getMessage();

    /**
     * Gets field errors.
     *
     * @return the field errors
     */
    public List<Pair<String, String>> getFieldErrors();

    /**
     * Gets global errors.
     *
     * @return the global errors
     */
    public List<String> getGlobalErrors();
}