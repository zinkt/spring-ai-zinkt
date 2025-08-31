package top.zinkt.springaizinkt.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import top.zinkt.springaizinkt.data.BookingStatus;

import java.time.LocalDate;
import java.util.function.Function;

@Configuration
public class BookingTools {

	@Autowired
	FlightBookingService  flightBookingService;

	@JsonInclude(Include.NON_NULL)
	public record BookingDetails(String bookingNumber, String name, LocalDate date, BookingStatus bookingStatus,
			String from, String to, String bookingClass) {
	}

	public record CancelBookingRequest(@ToolParam(description = "预定号，可以是纯数字") String bookingNumber, @ToolParam(description = "客户姓名") String name) {}
	@Bean
	@Tool(description = "处理机票退订")
	public Function<CancelBookingRequest, String> cancelBookingRequestTool(){
		return cancelBookingRequest -> {
			// 调用退订业务方法
			flightBookingService.cancelBooking(cancelBookingRequest.bookingNumber, cancelBookingRequest.name);
			return "退订成功";
		};
	}

	public record BookingDetailsRequest(String bookingNumber, String name){}
	@Bean
	@Tool(description = "获取机票预定信息")
	public Function<BookingDetailsRequest, BookingDetails> getBookingDetails() {
		return bookingDetailsRequest -> {
			try {
				return flightBookingService.getBookingDetails(bookingDetailsRequest.bookingNumber,  bookingDetailsRequest.name);
			}catch (Exception e){
				return new BookingDetails(bookingDetailsRequest.bookingNumber, bookingDetailsRequest.name, null, null, null, null, null);
			}
		};
	}



}
