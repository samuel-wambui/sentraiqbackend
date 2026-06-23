package com.senctraiq.ticketbuilder;

import com.senctraiq.ApiResponse.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/tickets")
@CrossOrigin(originPatterns = "*", maxAge = 3600)
public class TicketController {

	@Autowired
	private TicketService ticketService;

	@GetMapping
	public ResponseEntity<ApiResponse<List<TicketResponseDTO>>> getAllTickets() {
		List<TicketResponseDTO> tickets = ticketService.getAllTickets();
		ApiResponse<List<TicketResponseDTO>> response = new ApiResponse<>();
		response.setMessage("Tickets retrieved successfully");
		response.setStatusCode(HttpStatus.OK.value());
		response.setEntity(tickets);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/open")
	public ResponseEntity<ApiResponse<List<TicketResponseDTO>>> getOpenTickets() {
		List<TicketResponseDTO> tickets = ticketService.getOpenTickets();
		ApiResponse<List<TicketResponseDTO>> response = new ApiResponse<>();
		response.setMessage("Open tickets retrieved successfully");
		response.setStatusCode(HttpStatus.OK.value());
		response.setEntity(tickets);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/closed")
	public ResponseEntity<ApiResponse<List<TicketResponseDTO>>> getClosedTickets() {
		List<TicketResponseDTO> tickets = ticketService.getClosedTickets();
		ApiResponse<List<TicketResponseDTO>> response = new ApiResponse<>();
		response.setMessage("Closed tickets retrieved successfully");
		response.setStatusCode(HttpStatus.OK.value());
		response.setEntity(tickets);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/new")
	public ResponseEntity<ApiResponse<Map<String, List<TicketResponseDTO>>>> getNewTicketsByChannel() {
		Map<String, List<TicketResponseDTO>> ticketsByChannel = ticketService.getNewTicketsByChannel();
		ApiResponse<Map<String, List<TicketResponseDTO>>> response = new ApiResponse<>();
		response.setMessage("New tickets retrieved successfully");
		response.setStatusCode(HttpStatus.OK.value());
		response.setEntity(ticketsByChannel);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/handed-over")
	public ResponseEntity<ApiResponse<TicketHandoverQueuesResponseDTO>> getHandedOverTickets() {
		TicketHandoverQueuesResponseDTO tickets = ticketService.getHandedOverTicketQueues();
		ApiResponse<TicketHandoverQueuesResponseDTO> response = new ApiResponse<>();
		response.setMessage("Handed over tickets retrieved successfully");
		response.setStatusCode(HttpStatus.OK.value());
		response.setEntity(tickets);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/handed-over/ai")
	public ResponseEntity<ApiResponse<List<TicketResponseDTO>>> getTicketsHandedOverByAi() {
		List<TicketResponseDTO> tickets = ticketService.getTicketsHandedOverByAi();
		ApiResponse<List<TicketResponseDTO>> response = new ApiResponse<>();
		response.setMessage("Tickets handed over by AI retrieved successfully");
		response.setStatusCode(HttpStatus.OK.value());
		response.setEntity(tickets);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/handed-over/bank-support")
	public ResponseEntity<ApiResponse<List<TicketResponseDTO>>> getTicketsHandedOverByBankSupport() {
		List<TicketResponseDTO> tickets = ticketService.getTicketsHandedOverByBankSupport();
		ApiResponse<List<TicketResponseDTO>> response = new ApiResponse<>();
		response.setMessage("Tickets handed over by bank support retrieved successfully");
		response.setStatusCode(HttpStatus.OK.value());
		response.setEntity(tickets);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/active-bank-support")
	public ResponseEntity<ApiResponse<List<TicketResponseDTO>>> getActiveBankSupportTickets() {
		List<TicketResponseDTO> tickets = ticketService.getActiveBankSupportTickets();
		ApiResponse<List<TicketResponseDTO>> response = new ApiResponse<>();
		response.setMessage("Active bank support tickets retrieved successfully");
		response.setStatusCode(HttpStatus.OK.value());
		response.setEntity(tickets);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/new/{ticketNumber}/assign")
	public ResponseEntity<ApiResponse<TicketResponseDTO>> assignNewTicketToBankSupport(
			@PathVariable String ticketNumber,
			@RequestBody TicketAssignmentRequestDTO requestDTO
	) {
		Optional<TicketResponseDTO> assignedTicket = ticketService.assignTicketToBankSupport(ticketNumber, requestDTO);
		ApiResponse<TicketResponseDTO> response = new ApiResponse<>();
		response.setStatusCode(assignedTicket.isPresent() ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value());
		response.setMessage(assignedTicket.isPresent() ? "New ticket assigned to bank support successfully" : "Ticket not found");
		response.setEntity(assignedTicket.orElse(null));

		return ResponseEntity.status(assignedTicket.isPresent() ? HttpStatus.OK : HttpStatus.NOT_FOUND).body(response);
	}

	@PostMapping("/handed-over/{ticketNumber}/assign")
	public ResponseEntity<ApiResponse<TicketResponseDTO>> assignHandedOverTicketToBankSupport(
			@PathVariable String ticketNumber,
			@RequestBody TicketAssignmentRequestDTO requestDTO
	) {
		Optional<TicketResponseDTO> assignedTicket = ticketService.assignTicketToBankSupport(ticketNumber, requestDTO);
		ApiResponse<TicketResponseDTO> response = new ApiResponse<>();
		response.setStatusCode(assignedTicket.isPresent() ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value());
		response.setMessage(assignedTicket.isPresent() ? "Handed over ticket assigned to bank support successfully" : "Ticket not found");
		response.setEntity(assignedTicket.orElse(null));

		return ResponseEntity.status(assignedTicket.isPresent() ? HttpStatus.OK : HttpStatus.NOT_FOUND).body(response);
	}

	@PostMapping("/active-bank-support/{ticketNumber}/handover")
	public ResponseEntity<ApiResponse<TicketResponseDTO>> handOverActiveBankSupportTicket(
			@PathVariable String ticketNumber,
			@RequestBody TicketHandoverRequestDTO requestDTO,
			Authentication authentication
	) {
		String authenticatedUsername = authentication != null && authentication.isAuthenticated()
				? authentication.getName()
				: null;
		if ("anonymousUser".equalsIgnoreCase(String.valueOf(authenticatedUsername))) {
			authenticatedUsername = null;
		}
		Optional<TicketResponseDTO> handedOverTicket = ticketService.handOverTicketByBankSupport(ticketNumber, requestDTO, authenticatedUsername);
		ApiResponse<TicketResponseDTO> response = new ApiResponse<>();
		response.setStatusCode(handedOverTicket.isPresent() ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value());
		response.setMessage(handedOverTicket.isPresent() ? "Ticket handed over by bank support successfully" : "Ticket not found");
		response.setEntity(handedOverTicket.orElse(null));

		return ResponseEntity.status(handedOverTicket.isPresent() ? HttpStatus.OK : HttpStatus.NOT_FOUND).body(response);
	}

	@PostMapping("/{ticketNumber}/reply")
	public ResponseEntity<ApiResponse<TicketResponseDTO>> replyToTicket(
			@PathVariable String ticketNumber,
			@RequestBody TicketReplyRequestDTO requestDTO,
			Authentication authentication
	) {
		String authenticatedUsername = authentication != null && authentication.isAuthenticated()
				? authentication.getName()
				: null;
		if ("anonymousUser".equalsIgnoreCase(String.valueOf(authenticatedUsername))) {
			authenticatedUsername = null;
		}
		Optional<TicketResponseDTO> repliedTicket = ticketService.replyToTicket(ticketNumber, requestDTO, authenticatedUsername);
		ApiResponse<TicketResponseDTO> response = new ApiResponse<>();
		response.setStatusCode(repliedTicket.isPresent() ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value());
		response.setMessage(repliedTicket.isPresent() ? "Ticket reply sent successfully" : "Ticket not found");
		response.setEntity(repliedTicket.orElse(null));

		return ResponseEntity.status(repliedTicket.isPresent() ? HttpStatus.OK : HttpStatus.NOT_FOUND).body(response);
	}

	@PostMapping("/{ticketNumber}/close")
	public ResponseEntity<ApiResponse<TicketResponseDTO>> closeTicket(
			@PathVariable String ticketNumber,
			@RequestBody TicketCloseRequestDTO requestDTO,
			Authentication authentication
	) {
		ApiResponse<TicketResponseDTO> response = new ApiResponse<>();
		if (requestDTO == null || requestDTO.getClosingRemarks() == null || requestDTO.getClosingRemarks().isBlank()) {
			response.setStatusCode(HttpStatus.BAD_REQUEST.value());
			response.setMessage("Closing remarks are required");
			response.setEntity(null);
			return ResponseEntity.badRequest().body(response);
		}

		String authenticatedUsername = authentication != null && authentication.isAuthenticated()
				? authentication.getName()
				: null;
		if ("anonymousUser".equalsIgnoreCase(String.valueOf(authenticatedUsername))) {
			authenticatedUsername = null;
		}

		Optional<TicketResponseDTO> closedTicket = ticketService.closeTicket(ticketNumber, requestDTO, authenticatedUsername);
		response.setStatusCode(closedTicket.isPresent() ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value());
		response.setMessage(closedTicket.isPresent() ? "Ticket closed successfully" : "Ticket not found");
		response.setEntity(closedTicket.orElse(null));

		return ResponseEntity.status(closedTicket.isPresent() ? HttpStatus.OK : HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiResponse<Void>> handleTicketStateError(IllegalStateException error) {
		ApiResponse<Void> response = new ApiResponse<>();
		HttpStatus status = error.getMessage() != null && error.getMessage().startsWith("Reply webhook failed")
				? HttpStatus.BAD_GATEWAY
				: HttpStatus.CONFLICT;
		response.setMessage(error.getMessage());
		response.setStatusCode(status.value());
		response.setEntity(null);
		return ResponseEntity.status(status).body(response);
	}

	@GetMapping("/{ticketNumber}")
	public ResponseEntity<ApiResponse<TicketResponseDTO>> getTicket(@PathVariable String ticketNumber) {
		Optional<TicketResponseDTO> optionalTicket = ticketService.getTicketResponseByTicketNumber(ticketNumber);
		ApiResponse<TicketResponseDTO> response = new ApiResponse<>();
		response.setStatusCode(optionalTicket.isPresent() ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value());
		response.setMessage(optionalTicket.isPresent() ? "Ticket retrieved successfully" : "Ticket not found");
		response.setEntity(optionalTicket.orElse(null));

		return ResponseEntity.status(optionalTicket.isPresent() ? HttpStatus.OK : HttpStatus.NOT_FOUND).body(response);
	}

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
