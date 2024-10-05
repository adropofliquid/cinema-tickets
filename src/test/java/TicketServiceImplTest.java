import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImplTest {

    private TicketServiceImpl ticketService;

    @Mock
    private TicketPaymentService paymentService;

    @Mock
    private SeatReservationService reservationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    @Test
    public void testValidPurchaseWithOnlyAdultTickets() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);

        ticketService.purchaseTickets(1L, adultTickets);

        // Verify payment amount and seat reservation
        verify(paymentService).makePayment(1L, 3 * 25);  // 3 Adult tickets * £25
        verify(reservationService).reserveSeat(1L, 3);   // 3 Adult seats reserved
    }

    @Test
    public void testValidPurchaseWithAdultAndChildTickets() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);

        ticketService.purchaseTickets(1L, adultTickets, childTickets);

        // Verify payment amount and seat reservation
        verify(paymentService).makePayment(1L, 2 * 25 + 2 * 15);  // 2 Adult tickets * £25 + 2 Child tickets * £15
        verify(reservationService).reserveSeat(1L, 4);            // 2 Adult + 2 Child seats reserved
    }

    @Test
    public void testValidPurchaseWithAdultChildAndInfantTickets() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        ticketService.purchaseTickets(1L, adultTickets, childTickets, infantTickets);

        // Verify payment amount and seat reservation
        verify(paymentService).makePayment(1L, 2 * 25 + 2 * 15);  // 2 Adult tickets * £25 + 2 Child tickets * £15
        verify(reservationService).reserveSeat(1L, 4);            // 2 Adult + 2 Child seats reserved, no seats for infants
    }

    @Test
    public void testInvalidPurchaseWithoutAdultTickets() {
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, childTickets));
    }

    @Test
    public void testInvalidPurchaseWithMoreThan25Tickets() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 6);

        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, adultTickets, childTickets));
    }

    @Test
    public void testInvalidAccountId() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(0L, adultTickets));
    }

    @Test
    public void testValidPurchaseWithMaximum25Tickets() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 10);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 10);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 5);

        ticketService.purchaseTickets(1L, adultTickets, childTickets, infantTickets);

        verify(paymentService).makePayment(1L, 10 * 25 + 10 * 15);
        verify(reservationService).reserveSeat(1L, 20);  // Only Adult + Child seats reserved
    }


    @Test
    public void testPaymentServiceInteraction() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);

        ticketService.purchaseTickets(1L, adultTickets);

        ArgumentCaptor<Integer> paymentCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(paymentService).makePayment(eq(1L), paymentCaptor.capture());

        assertEquals(3 * 25, paymentCaptor.getValue());  // Ensure correct payment amount
    }

    @Test
    public void testSeatReservationServiceInteraction() {
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        ticketService.purchaseTickets(1L, adultTickets, childTickets);

        ArgumentCaptor<Integer> seatCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(reservationService).reserveSeat(eq(1L), seatCaptor.capture());

        assertEquals(3, seatCaptor.getValue());  // Ensure correct number of seats reserved
    }
}
