package it.qbsoftware.business.services.get;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import it.qbsoftware.adapters.in.jmaplib.entity.EmailAdapter;
import it.qbsoftware.business.domain.entity.AccountState;
import it.qbsoftware.business.domain.exception.AccountNotFoundException;
import it.qbsoftware.business.domain.exception.InvalidArgumentsException;
import it.qbsoftware.business.domain.exception.InvalidResultReferenceExecption;
import it.qbsoftware.business.domain.methodcall.filter.EmailPropertiesFilter;
import it.qbsoftware.business.domain.methodcall.process.get.GetReferenceIdsResolver;
import it.qbsoftware.business.domain.methodcall.response.AccountNotFoundMethodErrorResponse;
import it.qbsoftware.business.domain.util.get.RetrivedEntity;
import it.qbsoftware.business.ports.in.guava.ListMultimapPort;
import it.qbsoftware.business.ports.in.jmap.entity.EmailPort;
import it.qbsoftware.business.ports.in.jmap.entity.ResponseInvocationPort;
import it.qbsoftware.business.ports.in.jmap.error.InvalidArgumentsMethodErrorResponsePort;
import it.qbsoftware.business.ports.in.jmap.error.InvalidResultReferenceMethodErrorResponsePort;
import it.qbsoftware.business.ports.in.jmap.method.call.get.GetEmailMethodCallPort;
import it.qbsoftware.business.ports.in.jmap.method.response.MethodResponsePort;
import it.qbsoftware.business.ports.in.jmap.method.response.get.GetEmailMethodResponseBuilderPort;
import it.qbsoftware.business.ports.in.jmap.method.response.get.GetEmailMethodResponsePort;
import it.qbsoftware.business.ports.out.domain.AccountStateRepository;
import it.qbsoftware.business.ports.out.jmap.EmailRepository;


@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class GetEmailMethodCallServiceTest {

    @Mock
    private AccountStateRepository accountStateRepository;

    @Mock
    private ListMultimapPort<String, ResponseInvocationPort> previousResponses;

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private GetEmailMethodCallPort getEmailMethodCallPort;

    @Mock
    private GetReferenceIdsResolver getReferenceIdsResolver;

    @Mock
    private EmailPropertiesFilter emailPropertiesFilter;

    @Mock
    private InvalidArgumentsMethodErrorResponsePort invalidArgumentsMethodErrorResponsePort;

    @Mock
    private InvalidResultReferenceMethodErrorResponsePort invalidResultReferenceMethodErrorResponsePort;

    @Mock
    private GetEmailMethodResponsePort getEmailMethodResponsePort;

    @Mock
    private GetEmailMethodResponseBuilderPort getEmailMethodResponseBuilderPort;

    @InjectMocks
    private GetEmailMethodCallService getEmailMethodCallService;


    @Test
    public void testCallWithRetrive() throws AccountNotFoundException, InvalidResultReferenceExecption, InvalidArgumentsException {

        String accountId = "testAccountId";
        String[] emailIds = new String[] { "emailId1", "emailId2" };
        AccountState accountState = new AccountState(accountId);
        EmailPort[] emails = new EmailPort[] { new EmailAdapter(null), new EmailAdapter(null) };

        when(getReferenceIdsResolver.resolve(any(), any())).thenReturn(emailIds);
        when(emailRepository.retrive(any())).thenReturn(new RetrivedEntity<>(emails, new String[] {}));
        when(getEmailMethodCallPort.accountId()).thenReturn(accountId);
        when(accountStateRepository.retrive(accountId)).thenReturn(accountState);
        when(emailPropertiesFilter.filter(any(), any(), any())).thenReturn(emails);
        when(getEmailMethodResponseBuilderPort.reset()).thenReturn(getEmailMethodResponseBuilderPort);
        
        when(getEmailMethodResponseBuilderPort.list(any())).thenReturn(getEmailMethodResponseBuilderPort);
        
        when(getEmailMethodResponseBuilderPort.notFound(any())).thenReturn(getEmailMethodResponseBuilderPort);
        
        when(getEmailMethodResponseBuilderPort.state(any())).thenReturn(getEmailMethodResponseBuilderPort);
        when(getEmailMethodResponseBuilderPort.build()).thenReturn(getEmailMethodResponsePort);

        MethodResponsePort[] methodResponsePorts = getEmailMethodCallService.call(getEmailMethodCallPort, previousResponses);

        verify(getEmailMethodResponseBuilderPort).reset();
        verify(getEmailMethodResponseBuilderPort).list(emails);
        verify(getEmailMethodResponseBuilderPort).notFound(any());
        verify(getEmailMethodResponseBuilderPort).state(accountState.emailState());
        verify(getEmailMethodResponseBuilderPort).build();
        verify(emailRepository).retrive(emailIds);
        assertEquals(methodResponsePorts[0], getEmailMethodResponsePort);
        assertEquals(methodResponsePorts.length, 1);
    }


    @Test
    public void testCallWithAccountNotFoundException() throws AccountNotFoundException, InvalidResultReferenceExecption, InvalidArgumentsException {
                
        String accountId = "testAccountId";

        when(accountStateRepository.retrive(accountId)).thenThrow(new AccountNotFoundException());
        when(getEmailMethodCallPort.accountId()).thenReturn(accountId);
        
        MethodResponsePort[] methodResponsePorts = getEmailMethodCallService.call(getEmailMethodCallPort, previousResponses);
        
        assertTrue(methodResponsePorts[0] instanceof AccountNotFoundMethodErrorResponse);
    }

    @Test
    public void testCallWithInvalidResultReferenceException() throws AccountNotFoundException, InvalidResultReferenceExecption, InvalidArgumentsException {
                
        when(getReferenceIdsResolver.resolve(any(), any())).thenThrow(new InvalidResultReferenceExecption());
        
        MethodResponsePort[] methodResponsePorts = getEmailMethodCallService.call(getEmailMethodCallPort, previousResponses);
        
        assertTrue(methodResponsePorts[0] instanceof InvalidResultReferenceMethodErrorResponsePort);
    }

    @Test
    public void testCallWithInvalidArgumentsException() throws AccountNotFoundException, InvalidResultReferenceExecption, InvalidArgumentsException {
       
        String[] emailIds = new String[] { "emailId1", "emailId2" };
        EmailPort[] emails = new EmailPort[] { new EmailAdapter(null), new EmailAdapter(null) };
        
        when(getReferenceIdsResolver.resolve(any(), any())).thenReturn(emailIds);
        when(emailPropertiesFilter.filter(any(), any(), any())).thenThrow(new InvalidArgumentsException());
        when(emailRepository.retrive(any())).thenReturn(new RetrivedEntity<>(emails, new String[] {}));
        
        MethodResponsePort[] methodResponsePorts = getEmailMethodCallService.call(getEmailMethodCallPort, previousResponses);
        
        assertTrue(methodResponsePorts[0] instanceof InvalidArgumentsMethodErrorResponsePort);
    }



    @Test
    public void testCallWithRetriveAll() throws AccountNotFoundException, InvalidResultReferenceExecption, InvalidArgumentsException{
        String accountId = "testAccountId";
        String[] emailIds = null;
        AccountState accountState = new AccountState(accountId);
        EmailPort[] emails = new EmailPort[] { new EmailAdapter(null), new EmailAdapter(null) };

        when(getReferenceIdsResolver.resolve(any(), any())).thenReturn(emailIds);
        when(emailRepository.retriveAll(any())).thenReturn(new RetrivedEntity<>(emails, new String[] {}));
        when(getEmailMethodCallPort.accountId()).thenReturn(accountId);
        when(accountStateRepository.retrive(accountId)).thenReturn(accountState);
        when(emailPropertiesFilter.filter(any(), any(), any())).thenReturn(emails);
        when(getEmailMethodResponseBuilderPort.reset()).thenReturn(getEmailMethodResponseBuilderPort);
        
        when(getEmailMethodResponseBuilderPort.list(any())).thenReturn(getEmailMethodResponseBuilderPort);
        
        when(getEmailMethodResponseBuilderPort.notFound(any())).thenReturn(getEmailMethodResponseBuilderPort);
        
        when(getEmailMethodResponseBuilderPort.state(any())).thenReturn(getEmailMethodResponseBuilderPort);
        when(getEmailMethodResponseBuilderPort.build()).thenReturn(getEmailMethodResponsePort);

        MethodResponsePort[] methodResponsePorts = getEmailMethodCallService.call(getEmailMethodCallPort, previousResponses);

        verify(getEmailMethodResponseBuilderPort).reset();
        verify(getEmailMethodResponseBuilderPort).list(emails);
        verify(getEmailMethodResponseBuilderPort).notFound(any());
        verify(getEmailMethodResponseBuilderPort).state(accountState.emailState());
        verify(getEmailMethodResponseBuilderPort).build();
        verify(emailRepository).retriveAll(accountId);
        assertEquals(methodResponsePorts[0], getEmailMethodResponsePort);
        assertEquals(methodResponsePorts.length, 1);

    }


}