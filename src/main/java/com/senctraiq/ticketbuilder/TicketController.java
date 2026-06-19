package com.senctraiq.ticketbuilder;

import com.senctraiq.ApiResponse.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@CrossOrigin(originPatterns = "*", maxAge = 3600)
public class TicketController {

	@Autowired
	private TicketService ticketService;

	/**
	 * Check if there is an existing (non-deleted) ticket for the provided messageId.
	 * Returns a handover status response so callers can branch directly.
	 */
	@GetMapping("/check-handover/{senderId}")
	public ResponseEntity<ApiResponse<Ticket>> checkTicketHandOverToHuman(@PathVariable String senderId) {
		{

			ApiResponse<Ticket> response = new ApiResponse<>();
			Ticket ticket = ticketService.checkTicketHandOverToHuman(senderId);
			try {
				response.setStatusCode(HttpStatus.OK.value());
				if (ticket != null) {
					response.setMessage("Handover status retrieved successfully");
					response.setEntity(ticket);
				} else {
					response.setMessage("No Ticket found for the provided senderId");
					response.setEntity(null);
				}
			} catch (Exception e) {
				response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				response.setMessage("An error occurred while checking handover status: " + e.getMessage());
				response.setEntity(null);
			}
			return ResponseEntity.status(HttpStatus.OK).body(response);
		}
	}
	private TicketHandoverStatusResponseDTO buildHandoverStatusResponse(String messageId, Ticket ticket) {
		TicketHandoverStatusResponseDTO dto = new TicketHandoverStatusResponseDTO();
		dto.setMessageId(messageId);
		dto.setTicketFound(ticket != null);

		if (ticket == null) {
			dto.setHandedOver(false);
			dto.setHandoverStatus("NO_ACTIVE_TICKET");
			return dto;
		}

		dto.setHandedOver(ticket.isTicketHandedOver());
		dto.setHandoverStatus(ticket.isTicketHandedOver() ? "HANDED_OVER" : "NOT_HANDED_OVER");
		dto.setTicketNumber(ticket.getTicketNumber());
		dto.setTicketStatus(ticket.getTicketStatus());
		dto.setTicketAssignedTo(ticket.getTicketAssignedTo());
		dto.setTicketHandoverTime(ticket.getTicketHandoverTime());
		dto.setTicketHandoverBy(ticket.getTicketHandoverBy());
		dto.setHandoverNote(ticket.getHandoverNote());
		dto.setTicketCreatedAt(ticket.getTicketCreatedAt());
		dto.setTicketUpdatedAt(ticket.getTicketUpdatedAt());
		return dto;
	}

}
