package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    // Ticket prices
    private static final int MAX_SEATS_ON_SINGLE_PURCHASE = 25;
    private static final int ADULT_TICKET_PRICE = 25;
    private static final int CHILD_TICKET_PRICE = 15;
    private static final int INFANT_TICKET_PRICE = 0;

    public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        validatePurchaseRequest(accountId, ticketTypeRequests);

        int totalAmountToPay = calculateTotalPayment(ticketTypeRequests);
        int totalSeatsToReserve = calculateTotalSeats(ticketTypeRequests);

        processPayment(accountId, totalAmountToPay);
        reserveSeats(accountId, totalSeatsToReserve);
    }

    /**
     * Validate the purchase request for various conditions.
     */
    private void validatePurchaseRequest(Long accountId, TicketTypeRequest... ticketTypeRequests) {
        validateAccountId(accountId);
        validateTicketComposition(ticketTypeRequests);
    }

    /**
     * Validates if the account ID is valid.
     */
    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Invalid account ID.");
        }
    }

    /**
     * Validates the composition of tickets ensuring business rules are met.
     */
    private void validateTicketComposition(TicketTypeRequest... ticketTypeRequests) {
        int totalTickets = 0;
        int adultTickets = 0;
        int childTickets = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            int ticketCount = request.getNoOfTickets();

            switch (request.getTicketType()) {
                case ADULT:
                    adultTickets += ticketCount;
                    break;
                case CHILD:
                    childTickets += ticketCount;
                    break;
                case INFANT:
                    // Infants do not require seats, so we don't count them in seat calculations.
                    break;
            }
            totalTickets += ticketCount;
        }

        if (totalTickets > MAX_SEATS_ON_SINGLE_PURCHASE) {
            throw new InvalidPurchaseException("Cannot purchase more than 25 tickets at a time.");
        } //- Only a maximum of 25 tickets that can be purchased at a time.

        if (adultTickets == 0 && childTickets > 0) {
            throw new InvalidPurchaseException("Child tickets cannot be purchased without at least one Adult ticket.");
        } //- Child and Infant tickets cannot be purchased without purchasing an Adult ticket.
    }

    /**
     * Calculates the total payment amount based on the ticket types.
     */
    private int calculateTotalPayment(TicketTypeRequest... ticketTypeRequests) {
        int totalAmount = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT:
                    totalAmount += request.getNoOfTickets() * ADULT_TICKET_PRICE;
                    break;
                case CHILD:
                    totalAmount += request.getNoOfTickets() * CHILD_TICKET_PRICE;
                    break;
                case INFANT:
                    // Infants do not pay for a ticket.
                    break;
            }
        }
        return totalAmount;
    }

    /**
     * Calculates the total number of seats to reserve based on the ticket types.
     */
    private int calculateTotalSeats(TicketTypeRequest... ticketTypeRequests) {
        int totalSeats = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT:
                case CHILD:
                    totalSeats += request.getNoOfTickets();
                    break;
                case INFANT:
                    // Infants do not require a seat.
                    break;
            }
        }
        return totalSeats;
    }

    /**
     * Makes a payment request through the TicketPaymentService.
     */
    private void processPayment(Long accountId, int totalAmount) {
        paymentService.makePayment(accountId, totalAmount);
    }

    /**
     * Makes a seat reservation request through the SeatReservationService.
     */
    private void reserveSeats(Long accountId, int totalSeats) {
        reservationService.reserveSeat(accountId, totalSeats);
    }
}
