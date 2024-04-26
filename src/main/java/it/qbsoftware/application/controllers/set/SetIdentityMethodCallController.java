package it.qbsoftware.application.controllers.set;

import com.google.inject.Inject;

import it.qbsoftware.adapters.in.jmaplib.method.call.set.SetIdentityMethodCallAdapter;
import it.qbsoftware.adapters.in.jmaplib.method.response.set.SetIdentityMethodResponseAdapter;
import it.qbsoftware.application.controllers.ControllerHandlerBase;
import it.qbsoftware.application.controllers.HandlerRequest;
import it.qbsoftware.business.domain.exception.AccountNotFoundException;
import it.qbsoftware.business.domain.exception.StateMismatchException;
import it.qbsoftware.business.ports.in.usecase.set.SetIdentityMethodCallUsecase;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.identity.SetIdentityMethodCall;
import rs.ltt.jmap.common.method.error.InvalidArgumentsMethodErrorResponse;
import rs.ltt.jmap.common.method.error.StateMismatchMethodErrorResponse;

public class SetIdentityMethodCallController extends ControllerHandlerBase {
    private final SetIdentityMethodCallUsecase setIdentityMethodCallUsecase;

    @Inject
    public SetIdentityMethodCallController(final SetIdentityMethodCallUsecase setIdentityMethodCallUsecase) {
        this.setIdentityMethodCallUsecase = setIdentityMethodCallUsecase;
    }

    @Override
    public MethodResponse handle(final HandlerRequest handlerRequest) {
        if (handlerRequest.methodCall() instanceof SetIdentityMethodCall setIdentityMethodCall) {
            final SetIdentityMethodCallAdapter setIdentityMethodCallAdapter = new SetIdentityMethodCallAdapter(
                    setIdentityMethodCall);
            try {
                final SetIdentityMethodResponseAdapter setIdentityMethodResponseAdapter = (SetIdentityMethodResponseAdapter) setIdentityMethodCallUsecase
                        .call(setIdentityMethodCallAdapter, handlerRequest.previousResponses());
                return setIdentityMethodResponseAdapter.adaptee();
            } catch (final AccountNotFoundException accountNotFoundException) {
                return new InvalidArgumentsMethodErrorResponse();
            } catch (final StateMismatchException stateMismatchException) {
                return new StateMismatchMethodErrorResponse();
            }
        }
        return super.handle(handlerRequest);
    }
}
