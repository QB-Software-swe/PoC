package it.qbsoftware.application.controllers.changes;

import it.qbsoftware.adapters.in.jmaplib.method.call.changes.ChangesIdentityMethodCallAdapter;
import it.qbsoftware.adapters.in.jmaplib.method.response.changes.ChangesIdentityMethodResponseAdapter;
import it.qbsoftware.application.controllers.ControllerHandlerBase;
import it.qbsoftware.application.controllers.HandlerRequest;
import it.qbsoftware.business.domain.exception.AccountNotFoundException;
import it.qbsoftware.business.domain.exception.InvalidArgumentsException;
import it.qbsoftware.business.domain.exception.changes.CannotCalculateChangesException;
import it.qbsoftware.business.ports.in.usecase.changes.ChangesIdentityMethodCallUsecase;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.identity.ChangesIdentityMethodCall;
import rs.ltt.jmap.common.method.error.CannotCalculateChangesMethodErrorResponse;
import rs.ltt.jmap.common.method.error.InvalidArgumentsMethodErrorResponse;
import rs.ltt.jmap.common.method.response.identity.ChangesIdentityMethodResponse;

import java.util.ArrayList;

import com.google.inject.Inject;

public class ChangesIdentityMethodCallController extends ControllerHandlerBase {
    private final ChangesIdentityMethodCallUsecase changesIdentityMethodCallUsecase;

    @Inject
    public ChangesIdentityMethodCallController(
            final ChangesIdentityMethodCallUsecase changesIdentityMethodCallUsecase) {
        this.changesIdentityMethodCallUsecase = changesIdentityMethodCallUsecase;
    }

    @Override
    public MethodResponse handle(final HandlerRequest handlerRequest) {
        if (handlerRequest.methodCall() instanceof ChangesIdentityMethodCall changesIdentityMethodCall) {
            final ChangesIdentityMethodCallAdapter changesIdentityMethodCallAdapter = new ChangesIdentityMethodCallAdapter(
                    changesIdentityMethodCall);

            try {
                final ChangesIdentityMethodResponseAdapter changesIdentityMethodResponseAdapter = (ChangesIdentityMethodResponseAdapter) changesIdentityMethodCallUsecase
                        .call(changesIdentityMethodCallAdapter, handlerRequest.previousResponses());
                return changesIdentityMethodResponseAdapter.adaptee();

            } catch (final AccountNotFoundException accountNotFoundException) {
                return new InvalidArgumentsMethodErrorResponse();
            } catch (final CannotCalculateChangesException CannotCalculateChangesException) {
                return new CannotCalculateChangesMethodErrorResponse();
            } catch (final InvalidArgumentsException invalidArgumentsException) {
                return new InvalidArgumentsMethodErrorResponse();
            }
        }
        return super.handle(handlerRequest);
    }
}